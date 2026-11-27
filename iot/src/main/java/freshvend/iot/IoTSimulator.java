package freshvend.iot;

import freshvend.common.Log;
import freshvend.iot.hw.SimHardware;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Scanner;

/**
 * Trình giả lập thiết bị FreshVend (Thành viên 3). Cùng kiến trúc với firmware ESP32 trong thư mục firmware/.
 * Chạy:  java -cp build freshvend.iot.IoTSimulator
 */
public final class IoTSimulator {
  public static void main(String[] args) throws Exception {
    Config cfg = Config.load();
    SimHardware hw = new SimHardware(cfg.servoMoveMs);
    CommandJournal journal = new CommandJournal(Path.of(cfg.stateDir, "command-journal.log"));
    ColdChainMonitor monitor = new ColdChainMonitor(cfg.tempMin, cfg.tempMax, cfg.hysteresis, cfg.confirmSamples);
    DispenseController controller = new DispenseController(cfg, hw, hw, hw, journal);
    ApiClient api = new ApiClient(cfg);
    DeviceAgent agent = new DeviceAgent(cfg, api, hw, hw, monitor, controller, journal);

    Log.info("START", "FreshVend simulator | backend=" + cfg.backendUrl + " machine=" + cfg.machineId
        + " | ngưỡng " + cfg.tempMin + ".." + cfg.tempMax + "°C | xác thực=" + (cfg.usesOAuth() ? "OAuth2 client_credentials" : "token cố định (demo)"));
    System.out.println("Lệnh: temp <C> | hum <%> | sensor on/off (DHT22) | jam on/off | exit on/off (cảm biến khay ra) | door on/off | reset [khay|all] | status | help | quit");
    Runtime.getRuntime().addShutdownHook(new Thread(agent::close));
    agent.start();

    boolean running = true;
    try (Scanner sc = new Scanner(System.in)) {
      while (running && sc.hasNextLine()) {
        String line = sc.nextLine().trim().toLowerCase(Locale.ROOT);
        if (line.isEmpty()) continue;
        String[] p = line.split("\\s+");
        try {
          switch (p[0]) {
            case "temp": hw.tempC = Double.parseDouble(p[1]); Log.info("CLI", "nhiệt độ = " + hw.tempC); break;
            case "hum": hw.humidity = Double.parseDouble(p[1]); Log.info("CLI", "độ ẩm = " + hw.humidity); break;
            case "sensor": hw.dhtFailed = p[1].equals("off"); Log.info("CLI", "DHT22 " + (hw.dhtFailed ? "HỎNG" : "bình thường")); break;
            case "jam": hw.jammed = p[1].equals("on"); Log.info("CLI", "kẹt hàng = " + hw.jammed); break;
            case "exit": hw.exitSensorOk = p[1].equals("on"); Log.info("CLI", "cảm biến khay ra " + (hw.exitSensorOk ? "bình thường" : "HỎNG")); break;
            case "door": hw.doorOpen = p[1].equals("on"); Log.info("CLI", "cửa " + (hw.doorOpen ? "MỞ" : "đóng")); break;
            case "reset":
              if (p.length >= 2 && !p[1].equals("all")) controller.unlock(Integer.parseInt(p[1])); else controller.unlock();
              break;
            case "status":
              System.out.println("temp=" + hw.tempC + " hum=" + hw.humidity + " dht=" + (hw.dhtFailed ? "LỖI" : "ok")
                  + " jam=" + hw.jammed + " exit=" + (hw.exitSensorOk ? "ok" : "LỖI") + " door=" + (hw.doorOpen ? "mở" : "đóng")
                  + " servoRuns=" + hw.servoRuns.get() + "\n  " + agent.status());
              break;
            case "help":
              System.out.println("temp 10 | temp 5 | sensor off | jam on | exit off | door on | reset [khay|all] (sau khi xử lý kẹt) | status | quit");
              break;
            case "quit": running = false; break;
            default: System.out.println("Lệnh không hợp lệ, gõ 'help'");
          }
        } catch (ArrayIndexOutOfBoundsException | NumberFormatException e) {
          System.out.println("Sai cú pháp, gõ 'help'");
        }
      }
    }
    if (running && cfg.headless) { Log.info("START", "stdin đóng, chạy nền (headless)"); Thread.currentThread().join(); }
    agent.close();
  }
}
