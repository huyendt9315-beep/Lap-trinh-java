package freshvend.iot;

import freshvend.common.Log;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/**
 * Nhật ký lệnh bền vững (ghi đồng bộ xuống đĩa). Đây là cơ chế chống xuất hàng trùng:
 *   EXECUTING : đã ghi TRƯỚC khi servo chạy.
 *   DONE      : đã có kết quả (result + eventKey cố định), CHƯA được backend xác nhận.
 *   REPORTED  : backend đã nhận kết quả.
 * Nếu thiết bị mất điện/khởi động lại khi đang EXECUTING thì không biết hàng đã ra hay chưa:
 * KHÔNG quay servo lại, chuyển sang DONE với kết quả UNVERIFIED rồi báo cho backend.
 */
public final class CommandJournal {
  public enum State { EXECUTING, DONE, REPORTED }

  public static final class Entry {
    public final String commandKey;
    public State state;
    public int dispenseId, slotNo, attemptNo, mech;
    public String result = "", eventKey = "";
    public long ts;

    Entry(String key) { this.commandKey = key; }

    String line() {
      return String.join("|", commandKey, state.name(), String.valueOf(dispenseId), String.valueOf(slotNo),
          String.valueOf(attemptNo), String.valueOf(mech), result, eventKey, String.valueOf(ts));
    }
  }

  private static final int KEEP_REPORTED = 500;
  private final Path file;
  private final Map<String, Entry> entries = new LinkedHashMap<>();
  private final List<String> recovered = new ArrayList<>();

  public CommandJournal(Path file) {
    this.file = file;
    try {
      if (file.getParent() != null) Files.createDirectories(file.getParent());
      if (Files.exists(file)) load();
    } catch (IOException e) {
      throw new IllegalStateException("Không đọc được nhật ký lệnh " + file + ": " + e.getMessage(), e);
    }
    recover();
    compact();
  }

  private void load() throws IOException {
    for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
      String[] p = line.split("\\|", -1);
      if (p.length < 9) continue; // bỏ dòng hỏng (ví dụ ghi dở khi mất điện)
      try {
        Entry e = entries.computeIfAbsent(p[0], Entry::new);
        e.state = State.valueOf(p[1]);
        e.dispenseId = Integer.parseInt(p[2]);
        e.slotNo = Integer.parseInt(p[3]);
        e.attemptNo = Integer.parseInt(p[4]);
        e.mech = Integer.parseInt(p[5]);
        e.result = p[6];
        e.eventKey = p[7];
        e.ts = Long.parseLong(p[8]);
      } catch (RuntimeException ex) {
        Log.info("JOURNAL", "bỏ qua dòng hỏng: " + line);
      }
    }
  }

  private void recover() {
    for (Entry e : entries.values()) {
      if (e.state == State.EXECUTING) {
        e.state = State.DONE;
        e.result = "UNVERIFIED";
        e.eventKey = UUID.randomUUID().toString();
        e.ts = System.currentTimeMillis();
        append(e);
        recovered.add(e.commandKey);
        Log.info("RECOVERY", "lệnh " + e.commandKey + " đang chạy dở khi thiết bị dừng -> KHÔNG quay servo lại, báo UNVERIFIED");
      }
    }
  }

  private void compact() {
    try {
      List<Entry> keep = new ArrayList<>();
      List<Entry> reported = new ArrayList<>();
      for (Entry e : entries.values()) (e.state == State.REPORTED ? reported : keep).add(e);
      int from = Math.max(0, reported.size() - KEEP_REPORTED);
      List<Entry> all = new ArrayList<>(reported.subList(from, reported.size()));
      all.addAll(keep);
      Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
      List<String> lines = new ArrayList<>();
      for (Entry e : all) lines.add(e.line());
      Files.write(tmp, lines, StandardCharsets.UTF_8);
      try {
        Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
      } catch (AtomicMoveNotSupportedException ex) {
        Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
      }
      entries.clear();
      for (Entry e : all) entries.put(e.commandKey, e);
    } catch (IOException e) {
      Log.info("JOURNAL", "không nén được nhật ký: " + e.getMessage());
    }
  }

  private void append(Entry e) {
    try {
      Files.writeString(file, e.line() + System.lineSeparator(), StandardCharsets.UTF_8,
          StandardOpenOption.CREATE, StandardOpenOption.APPEND, StandardOpenOption.SYNC);
    } catch (IOException ex) {
      // Không ghi được nhật ký => không được phép vận hành cơ cấu (an toàn).
      throw new IllegalStateException("Không ghi được nhật ký lệnh, từ chối xuất hàng: " + ex.getMessage(), ex);
    }
  }

  public synchronized Entry get(String key) { return entries.get(key); }

  public synchronized List<String> recoveredKeys() { return new ArrayList<>(recovered); }

  /** Ghi TRƯỚC mỗi lần quay servo. */
  public synchronized void markExecuting(Command c, int mechanicalAttempt) {
    Entry e = entries.computeIfAbsent(c.commandKey(), Entry::new);
    e.state = State.EXECUTING;
    e.dispenseId = c.dispenseId();
    e.slotNo = c.slotNo();
    e.attemptNo = c.attemptNo();
    e.mech = mechanicalAttempt;
    e.result = "";
    e.eventKey = "";
    e.ts = System.currentTimeMillis();
    append(e);
  }

  public synchronized String markDone(Command c, String result) {
    Entry e = entries.computeIfAbsent(c.commandKey(), Entry::new);
    e.dispenseId = c.dispenseId();
    e.slotNo = c.slotNo();
    e.attemptNo = c.attemptNo();
    e.state = State.DONE;
    e.result = result;
    e.eventKey = UUID.randomUUID().toString();
    e.ts = System.currentTimeMillis();
    append(e);
    return e.eventKey;
  }

  public synchronized void markReported(String key) {
    Entry e = entries.get(key);
    if (e == null) return;
    e.state = State.REPORTED;
    e.ts = System.currentTimeMillis();
    append(e);
  }

  public synchronized List<Entry> pendingReports() {
    List<Entry> l = new ArrayList<>();
    for (Entry e : entries.values()) if (e.state == State.DONE) l.add(e);
    return l;
  }

  public synchronized int pendingCount() { return pendingReports().size(); }
}
