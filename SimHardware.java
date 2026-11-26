package freshvend.iot.hw;

import freshvend.common.Log;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/** Phần cứng giả lập, điều khiển từ dòng lệnh của IoTSimulator. */
public final class SimHardware
    implements Hardware.Thermometer, Hardware.Servo, Hardware.ExitSensor, Hardware.DoorSensor {
  public volatile double tempC = 5.0, humidity = 65.0;
  public volatile boolean dhtFailed = false;      // DHT22 hỏng/mất kết nối
  public volatile boolean jammed = false;         // servo chạy nhưng hàng không ra
  public volatile boolean exitSensorOk = true;    // cảm biến khay ra còn hoạt động
  public volatile boolean doorOpen = false;
  public final AtomicInteger servoRuns = new AtomicInteger();
  private volatile boolean itemOut = false;
  private final long moveMs;

  public SimHardware(long moveMs) { this.moveMs = moveMs; }

  @Override public Optional<Hardware.Reading> read() {
    if (dhtFailed) return Optional.empty();
    return Optional.of(new Hardware.Reading(tempC, humidity));
  }

  @Override public void dispense(int slotNo) throws InterruptedException {
    itemOut = false;
    Log.info("SIM-SERVO", "slot " + slotNo + " bắt đầu quay");
    if (moveMs > 0) Thread.sleep(moveMs);
    itemOut = !jammed;
    servoRuns.incrementAndGet();
    Log.info("SIM-SERVO", "slot " + slotNo + " xong, hàng ra=" + itemOut);
  }

  @Override public Hardware.ExitCheck check() {
    if (!exitSensorOk) return Hardware.ExitCheck.SENSOR_UNAVAILABLE;
    return itemOut ? Hardware.ExitCheck.ITEM_DETECTED : Hardware.ExitCheck.NOT_DETECTED;
  }

  @Override public boolean isOpen() { return doorOpen; }
}
