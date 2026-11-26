package freshvend.demo;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import freshvend.common.Log;
import freshvend.common.MiniJson;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Backend GIẢ để thử thiết bị trên localhost, mô phỏng hợp đồng API trong 05_API-Specification.md:
 * OAuth2 client_credentials, Bearer + kiểm tra machineId (401/403), chống trùng eventKey, lệnh được giao
 * lại đến khi nhận kết quả, tạo cảnh báo. KHÔNG phải backend của TV2 và không dùng cho hệ thống thật.
 * Có thêm /demo/fault để chèn lỗi (503, mất phản hồi) phục vụ kiểm thử độ bền.
 */
public final class DemoBackend {
  static final String ADMIN = "demo-admin-token", LEGACY = "demo-device-token";
  static final String CLIENT_ID = "freshvend-device-1", CLIENT_SECRET = "demo-secret";
  static final Set<String> RESULTS = Set.of("DELIVERED", "JAM_SUSPECTED", "UNVERIFIED", "REFUSED");
  static final Pattern MACHINE =
      Pattern.compile("^/api/v1/machines/([^/]+)/(telemetry|commands|dispense-events|alerts|cold-chain)$");

  private final Object lock = new Object();
  private final long ttlSec = Long.getLong("token.ttl.sec", 3600);
  private final Map<String, Long> tokens = new HashMap<>();
  private int tokensIssued = 0;
  private final Map<String, Map<String, Object>> telemetry = new LinkedHashMap<>();
  private final Map<String, Map<String, Object>> events = new LinkedHashMap<>();
  private final Map<String, Map<String, Object>> commands = new LinkedHashMap<>();
  private final List<Map<String, Object>> alerts = new ArrayList<>();
  private double minC = 2, maxC = 8;
  private String degradedMessage = "Máy tạm dừng bán";
  private int nextDispenseId = 12, alertSeq = 1;
  private int failNext = 0, failStatus = 503, lostAck = 0;
  private String failPath = "";

  // ---------------------------------------------------------------- tiện ích
  private static Map<String, Object> err(String m) { return new LinkedHashMap<>(Map.of("error", m)); }

  private static void reply(HttpExchange x, int code, Object json) throws Exception {
    byte[] data = MiniJson.write(json).getBytes(StandardCharsets.UTF_8);
    x.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
    x.sendResponseHeaders(code, data.length);
    try (var out = x.getResponseBody()) { out.write(data); }
  }

  private static String body(HttpExchange x) throws Exception {
    return new String(x.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
  }

  private static String bearer(HttpExchange x) {
    String h = x.getRequestHeaders().getFirst("Authorization");
    return (h != null && h.startsWith("Bearer ")) ? h.substring(7) : null;
  }

  private static boolean admin(HttpExchange x) { return ADMIN.equals(x.getRequestHeaders().getFirst("X-Admin-Token")); }

  private static Map<String, String> query(String raw) {
    Map<String, String> m = new HashMap<>();
    if (raw == null) return m;
    for (String kv : raw.split("&")) {
      int i = kv.indexOf('=');
      if (i > 0) m.put(URLDecoder.decode(kv.substring(0, i), StandardCharsets.UTF_8),
          URLDecoder.decode(kv.substring(i + 1), StandardCharsets.UTF_8));
    }
    return m;
  }

  private boolean validToken(String t) {
    synchronized (lock) {
      if (LEGACY.equals(t)) return true;
      Long exp = tokens.get(t);
      return exp != null && exp > System.currentTimeMillis();
    }
  }

  // ---------------------------------------------------------------- định tuyến
  void handle(HttpExchange x) {
    try {
      route(x);
    } catch (Exception e) {
      e.printStackTrace();
      try { reply(x, 500, err("internal")); } catch (Exception ignored) { }
    } finally {
      x.close();
    }
  }

  private void route(HttpExchange x) throws Exception {
    String p = x.getRequestURI().getPath(), m = x.getRequestMethod();
    if (p.equals("/oauth2/token") && m.equals("POST")) { token(x); return; }
    if (p.startsWith("/demo/")) { demo(x, p, m); return; }
    Matcher mm = MACHINE.matcher(p);
    if (!mm.matches()) { reply(x, 404, err("not found")); return; }
    String machine = mm.group(1), ep = mm.group(2);

    synchronized (lock) {
      if (failNext > 0 && (failPath.isEmpty() || ep.contains(failPath))) {
        failNext--;
        reply(x, failStatus, err("injected failure"));
        return;
      }
    }
    boolean deviceEp = ep.equals("commands") || ep.equals("dispense-events") || (ep.equals("telemetry") && m.equals("POST"));
    if (deviceEp) {
      String t = bearer(x);
      if (t == null || !validToken(t)) { reply(x, 401, err("unauthorized")); return; }
      if (!machine.equals("1")) { reply(x, 403, err("token không thuộc máy " + machine)); return; }
    } else if (!admin(x)) {
      reply(x, 401, err("cần quyền ADMIN/OPERATOR (demo: header X-Admin-Token)"));
      return;
    }
    switch (ep + ":" + m) {
      case "telemetry:POST": postTelemetry(x); break;
      case "telemetry:GET": synchronized (lock) { reply(x, 200, new ArrayList<>(telemetry.values())); } break;
      case "commands:GET": getCommands(x); break;
      case "dispense-events:POST": postEvent(x); break;
      case "alerts:GET": synchronized (lock) { reply(x, 200, new ArrayList<>(alerts)); } break;
      case "cold-chain:GET": reply(x, 200, coldChain()); break;
      case "cold-chain:PUT": putColdChain(x); break;
      default: reply(x, 405, err("method not allowed"));
    }
  }

  private void token(HttpExchange x) throws Exception {
    Map<String, String> f = query(body(x));
    if (!"client_credentials".equals(f.get("grant_type")) || !CLIENT_ID.equals(f.get("client_id"))
        || !CLIENT_SECRET.equals(f.get("client_secret"))) {
      reply(x, 401, err("invalid_client"));
      return;
    }
    String t = "dev-" + UUID.randomUUID();
    synchronized (lock) {
      tokens.put(t, System.currentTimeMillis() + ttlSec * 1000);
      tokensIssued++;
    }
    Map<String, Object> r = new LinkedHashMap<>();
    r.put("access_token", t);
    r.put("token_type", "Bearer");
    r.put("expires_in", ttlSec);
    reply(x, 200, r);
  }

  // ---------------------------------------------------------------- telemetry & cảnh báo
  private static String validateTelemetry(Map<String, Object> b) {
    for (String k : List.of("eventKey", "measuredAt", "tempC", "humidityPct", "doorOpen", "deviceState"))
      if (!b.containsKey(k)) return "thiếu trường " + k;
    if (MiniJson.str(b, "eventKey").isEmpty()) return "eventKey rỗng";
    if (MiniJson.str(b, "deviceState").isEmpty()) return "deviceState rỗng";
    try { Instant.parse(MiniJson.str(b, "measuredAt")); } catch (Exception e) { return "measuredAt không phải ISO-8601 UTC"; }
    if (b.get("tempC") != null && !(b.get("tempC") instanceof Number)) return "tempC phải là số hoặc null";
    if (b.get("humidityPct") != null && !(b.get("humidityPct") instanceof Number)) return "humidityPct phải là số hoặc null";
    if (b.get("doorOpen") != null && !(b.get("doorOpen") instanceof Boolean)) return "doorOpen phải là boolean hoặc null";
    return null;
  }

  private void postTelemetry(HttpExchange x) throws Exception {
    Map<String, Object> b;
    try { b = MiniJson.asMap(MiniJson.parse(body(x))); } catch (Exception e) { reply(x, 400, err("JSON không hợp lệ")); return; }
    String problem = validateTelemetry(b);
    if (problem != null) { reply(x, 400, err(problem)); return; }
    synchronized (lock) {
      String k = MiniJson.str(b, "eventKey");
      if (!telemetry.containsKey(k)) { telemetry.put(k, b); evaluateAlerts(b); }
    }
    reply(x, 200, new LinkedHashMap<>(Map.of("accepted", true)));
  }

  private void evaluateAlerts(Map<String, Object> b) {
    String at = MiniJson.str(b, "measuredAt");
    Object t = b.get("tempC");
    if (t instanceof Number) {
      double v = ((Number) t).doubleValue();
      setAlert("HIGH_TEMP", "HIGH", v > maxC, at);
      setAlert("LOW_TEMP", "MEDIUM", v < minC, at);
      setAlert("SENSOR_FAULT", "HIGH", false, at);
    } else {
      setAlert("SENSOR_FAULT", "HIGH", "DEGRADED".equals(MiniJson.str(b, "deviceState")), at);
    }
  }

  private void setAlert(String kind, String severity, boolean active, String at) {
    Map<String, Object> open = null;
    for (Map<String, Object> a : alerts) if (kind.equals(a.get("kind")) && a.get("closedAt") == null) open = a;
    if (active && open == null) {
      Map<String, Object> a = new LinkedHashMap<>();
      a.put("id", alertSeq++);
      a.put("machineId", 1);
      a.put("kind", kind);
      a.put("severity", severity);
      a.put("openedAt", at);
      a.put("closedAt", null);
      alerts.add(a);
      Log.info("DEMO-ALERT", "mở cảnh báo " + kind);
    } else if (!active && open != null) {
      open.put("closedAt", at);
      Log.info("DEMO-ALERT", "đóng cảnh báo " + kind);
    }
  }

  // ---------------------------------------------------------------- lệnh & kết quả xuất hàng
  private void getCommands(HttpExchange x) throws Exception {
    Map<String, String> q = query(x.getRequestURI().getRawQuery());
    int off = Integer.parseInt(q.getOrDefault("offset", "0")), lim = Integer.parseInt(q.getOrDefault("limit", "50"));
    List<Object> out = new ArrayList<>();
    synchronized (lock) {
      int idx = 0;
      for (Map<String, Object> c : commands.values()) {
        if (!"QUEUED".equals(c.get("status"))) continue; // lệnh chưa có kết quả thì LUÔN được giao lại
        if (idx++ < off || out.size() >= lim) continue;
        Map<String, Object> o = new LinkedHashMap<>();
        o.put("commandKey", c.get("commandKey"));
        o.put("dispenseId", c.get("dispenseId"));
        o.put("slotNo", c.get("slotNo"));
        o.put("attemptNo", c.get("attemptNo"));
        out.add(o);
      }
    }
    reply(x, 200, out);
  }

  private void postEvent(HttpExchange x) throws Exception {
    Map<String, Object> b;
    try { b = MiniJson.asMap(MiniJson.parse(body(x))); } catch (Exception e) { reply(x, 400, err("JSON không hợp lệ")); return; }
    String ck = MiniJson.str(b, "commandKey"), ek = MiniJson.str(b, "eventKey"), res = MiniJson.str(b, "result");
    if (ck.isEmpty() || ek.isEmpty() || !(b.get("attemptNo") instanceof Number)) { reply(x, 400, err("thiếu commandKey/eventKey/attemptNo")); return; }
    if (!RESULTS.contains(res)) { reply(x, 400, err("result không được hỗ trợ: " + res)); return; }
    int code = 200;
    Object resp = new LinkedHashMap<>(Map.of("accepted", true));
    synchronized (lock) {
      Map<String, Object> cmd = commands.get(ck);
      if (cmd == null) { code = 404; resp = err("không có lệnh " + ck); }
      else if (events.containsKey(ek)) { Log.info("DEMO", "eventKey trùng " + ek + " -> bỏ qua (idempotent)"); }
      else if (!"QUEUED".equals(cmd.get("status"))) { code = 409; resp = err("lệnh đã có kết quả khác"); }
      else {
        events.put(ek, b);
        cmd.put("status", "DONE");
        cmd.put("result", res);
        String now = Instant.now().toString();
        if (res.equals("JAM_SUSPECTED")) setAlert("DISPENSE_JAM", "HIGH", true, now);
        if (res.equals("UNVERIFIED")) setAlert("DISPENSE_UNVERIFIED", "MEDIUM", true, now);
        Log.info("DEMO", "nhận kết quả " + res + " cho lệnh " + ck);
      }
      if (code == 200 && lostAck > 0) { lostAck--; code = 500; resp = err("mô phỏng mất phản hồi sau khi đã xử lý"); }
    }
    reply(x, code, resp);
  }

  private Map<String, Object> coldChain() {
    Map<String, Object> m = new LinkedHashMap<>();
    synchronized (lock) { m.put("minC", minC); m.put("maxC", maxC); m.put("degradedMessage", degradedMessage); }
    return m;
  }

  private void putColdChain(HttpExchange x) throws Exception {
    try {
      Map<String, Object> b = MiniJson.asMap(MiniJson.parse(body(x)));
      synchronized (lock) {
        minC = ((Number) b.get("minC")).doubleValue();
        maxC = ((Number) b.get("maxC")).doubleValue();
        degradedMessage = MiniJson.str(b, "degradedMessage");
      }
      reply(x, 200, coldChain());
    } catch (Exception e) {
      reply(x, 400, err("cần minC, maxC, degradedMessage"));
    }
  }

  // ---------------------------------------------------------------- /demo/*
  private void demo(HttpExchange x, String p, String m) throws Exception {
    if (p.equals("/demo/status") && m.equals("GET")) {
      synchronized (lock) {
        long pending = commands.values().stream().filter(c -> "QUEUED".equals(c.get("status"))).count();
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("telemetryCount", telemetry.size());
        r.put("pendingCommands", pending);
        r.put("eventsCount", events.size());
        r.put("alertsCount", alerts.size());
        reply(x, 200, r);
      }
      return;
    }
    if (!admin(x)) { reply(x, 401, err("unauthorized admin")); return; }
    if (p.equals("/demo/commands") && m.equals("POST")) {
      String raw = body(x);
      Map<String, Object> b = raw.isBlank() ? new LinkedHashMap<>() : MiniJson.asMap(MiniJson.parse(raw));
      String key = UUID.randomUUID().toString();
      Map<String, Object> c = new LinkedHashMap<>();
      synchronized (lock) {
        c.put("commandKey", key);
        c.put("dispenseId", nextDispenseId++);
        c.put("slotNo", MiniJson.intOr(b, "slotNo", 2));
        c.put("attemptNo", 1);
        c.put("status", "QUEUED");
        commands.put(key, c);
      }
      Map<String, Object> r = new LinkedHashMap<>();
      r.put("commandKey", key);
      r.put("queued", true);
      reply(x, 200, r);
    } else if (p.equals("/demo/inspect") && m.equals("GET")) {
      synchronized (lock) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("telemetry", new ArrayList<>(telemetry.values()));
        r.put("events", new ArrayList<>(events.values()));
        r.put("commands", new ArrayList<>(commands.values()));
        r.put("alerts", new ArrayList<>(alerts));
        r.put("tokensIssued", tokensIssued);
        reply(x, 200, r);
      }
    } else if (p.equals("/demo/fault") && m.equals("POST")) {
      Map<String, Object> b = MiniJson.asMap(MiniJson.parse(body(x)));
      synchronized (lock) {
        failNext = MiniJson.intOr(b, "failNext", 0);
        failStatus = MiniJson.intOr(b, "status", 503);
        failPath = MiniJson.str(b, "path");
        lostAck = MiniJson.intOr(b, "lostAck", 0);
      }
      reply(x, 200, new LinkedHashMap<>(Map.of("ok", true)));
    } else {
      reply(x, 404, err("not found"));
    }
  }

  public static void main(String[] args) throws Exception {
    int port = Integer.getInteger("server.port", 8080);
    DemoBackend d = new DemoBackend();
    HttpServer s = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
    s.createContext("/", d::handle);
    s.setExecutor(Executors.newCachedThreadPool());
    s.start();
    Log.info("DEMO", "DemoBackend http://127.0.0.1:" + port + " (máy 1) | client OAuth2: " + CLIENT_ID + " / " + CLIENT_SECRET
        + " | token TTL " + d.ttlSec + "s | admin header X-Admin-Token: " + ADMIN);
  }
}
