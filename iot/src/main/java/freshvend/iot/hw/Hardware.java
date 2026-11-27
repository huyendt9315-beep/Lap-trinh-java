package freshvend.iot.hw;

import java.util.Optional;

/**
 * Lớp trừu tượng phần cứng. Simulator dùng SimHardware; firmware ESP32 có cùng cấu trúc:
 * DHT22 -> Thermometer, servo -> Servo, cảm biến khay ra -> ExitSensor, công tắc cửa -> DoorSensor.
 */
public final class Hardware {
  private Hardware() {}

  public record Reading(double tempC, double humidityPct) {}

  public enum ExitCheck { ITEM_DETECTED, NOT_DETECTED, SENSOR_UNAVAILABLE }

  public interface Thermometer { Optional<Reading> read(); }

  /** Chạy một chu trình xuất cho khay slotNo (chặn đến khi servo chạy xong). */
  public interface Servo { void dispense(int slotNo) throws InterruptedException; }

  public interface ExitSensor { ExitCheck check(); }

  public interface DoorSensor { boolean isOpen(); }
}
