package freshvend.iot;

import freshvend.common.Log;
import freshvend.iot.hw.Hardware.Reading;
import java.util.Optional;

/**
 * Giám sát chuỗi lạnh tại thiết bị: chống nhiễu bằng "xác nhận N mẫu liên tiếp" và độ trễ (hysteresis)
 * khi trở về bình thường, tránh báo động nhấp nháy quanh ngưỡng.
 * Cảm biến lỗi KHÔNG được coi là "nhiệt độ an toàn": trạng thái chuyển sang SENSOR_FAULT.
 */
public final class ColdChainMonitor {
  public enum Status { OK, TOO_WARM, TOO_COLD, SENSOR_FAULT }

  private final double min, max, hysteresis;
  private final int confirm;
  private Status status = Status.OK;
  private Status candidate = Status.OK;
  private int streak = 0;

  public ColdChainMonitor(double min, double max, double hysteresis, int confirm) {
    this.min = min;
    this.max = max;
    this.hysteresis = hysteresis;
    this.confirm = Math.max(1, confirm);
  }

  public synchronized Status update(Optional<Reading> reading) {
    Status observed;
    if (reading.isEmpty()) {
      observed = Status.SENSOR_FAULT;
    } else {
      double t = reading.get().tempC();
      observed = t > max ? Status.TOO_WARM : (t < min ? Status.TOO_COLD : Status.OK);
      // Trễ khi hồi phục: phải về sâu trong vùng an toàn mới coi là hết cảnh báo.
      if (observed == Status.OK) {
        if (status == Status.TOO_WARM && t > max - hysteresis) observed = Status.TOO_WARM;
        if (status == Status.TOO_COLD && t < min + hysteresis) observed = Status.TOO_COLD;
      }
    }
    if (observed == status) { candidate = status; streak = 0; return status; }
    if (observed == candidate) streak++; else { candidate = observed; streak = 1; }
    if (streak >= confirm) {
      Status old = status;
      status = observed;
      streak = 0;
      Log.info("ALERT", old + " -> " + status
          + reading.map(r -> " (nhiệt độ " + r.tempC() + "°C, ngưỡng " + min + ".." + max + ")").orElse(" (không đọc được DHT22)"));
    }
    return status;
  }

  public synchronized Status status() { return status; }

  public synchronized boolean alertActive() { return status != Status.OK; }
}
