import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;

/** ESP32 simulator. Physical success is NEVER inferred from servo movement. */
public class IoTSimulator {
  static final String DEVICE=System.getProperty("device.id","FRESHVEND-001");
  static final String TOKEN=System.getProperty("device.token","demo-device-token");
  static final String BASE=System.getProperty("backend.url","http://127.0.0.1:8080/api/v1/iot");
  static final HttpClient HTTP=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
  static volatile double temp=5, humidity=65;
  static volatile boolean jam=false, sensorOk=true, exitSensor=true, running=true;
  static final Map<String,String> pending=new ConcurrentHashMap<>();
  static final Set<String> processed=ConcurrentHashMap.newKeySet();
  static final int MAX_ATTEMPTS=2;
  static String field(String json,String key){
    Matcher m=Pattern.compile("\\\""+Pattern.quote(key)+"\\\"\\s*:\\s*\\\"([^\\\"]*)\\\"").matcher(json);
    return m.find()?m.group(1):"";
  }
  static String esc(String s){return s.replace("\\","\\\\").replace("\"","\\\"");}
  static String request(String method,String path,String body)throws Exception{
    HttpRequest.Builder b=HttpRequest.newBuilder(URI.create(BASE+path)).timeout(Duration.ofSeconds(3))
      .header("X-Device-Id",DEVICE).header("X-Device-Token",TOKEN);
    if("POST".equals(method)) b.header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(body,StandardCharsets.UTF_8));
    else b.GET();
    HttpResponse<String> r=HTTP.send(b.build(),HttpResponse.BodyHandlers.ofString());
    if(r.statusCode()/100!=2)throw new IllegalStateException("HTTP "+r.statusCode()+" "+r.body());
    return r.body();
  }
  static String result(String id,String status,int attempts,boolean verified,String batch){
    return String.format(Locale.US,"{\"deviceId\":\"%s\",\"commandId\":\"%s\",\"result\":\"%s\",\"attempts\":%d,\"verified\":%s,\"batchId\":\"%s\"}",
      esc(DEVICE),esc(id),status,attempts,verified,esc(batch));
  }
  static void flushResults(){
    for(var entry:pending.entrySet())try{
      request("POST","/dispense-result",entry.getValue());
      pending.remove(entry.getKey(),entry.getValue());
      System.out.println("[RESULT ACK] "+entry.getKey());
    }catch(Exception e){System.out.println("[RETRY REPORT LATER] "+entry.getKey()+" "+e.getMessage());}
  }
  static void dispense(String id,String batch){
    if(!processed.add(id))return;
    boolean confirmed=false;
    int attempts=0;
    if(exitSensor){
      for(int i=1;i<=MAX_ATTEMPTS;i++){
        attempts=i;
        System.out.println("[SERVO SIM] "+id+" batch="+batch+" attempt="+i);
        if(!jam){confirmed=true;break;}
        // Simulator models a persistent jam; on real hardware retry requires safe mechanical verification.
      }
    }
    String status=confirmed?"SUCCESS":exitSensor?"JAM_SUSPECTED":"UNVERIFIED";
    if(!confirmed)System.out.println("[ALERT] "+status+" command="+id);
    pending.put(id,result(id,status,attempts,confirmed,batch));
    flushResults();
  }
  static void tick(){
    flushResults(); // never repeat the physical dispense on network failure
    boolean alert=!sensorOk||temp<2||temp>8;
    String payload=String.format(Locale.US,"{\"deviceId\":\"%s\",\"sensorOk\":%s,\"temperatureC\":%.2f,\"humidityPercent\":%.2f,\"temperatureAlert\":%s,\"wifiConnected\":true,\"dispenseSensorInstalled\":%s}",
      esc(DEVICE),sensorOk,temp,humidity,alert,exitSensor);
    try{request("POST","/telemetry",payload);if(alert)System.out.println("[ALERT] TEMPERATURE_OR_SENSOR");}
    catch(Exception e){System.out.println("[OFFLINE] telemetry "+e.getMessage());return;}
    try{
      String cmd=request("GET","/commands/next?deviceId="+URLEncoder.encode(DEVICE,StandardCharsets.UTF_8),"");
      String id=field(cmd,"commandId"),type=field(cmd,"type"),batch=field(cmd,"batchId");
      if(!id.isEmpty()&&"DISPENSE".equals(type))dispense(id,batch);
    }catch(Exception e){System.out.println("[OFFLINE] command "+e.getMessage());}
  }
  public static void main(String[] args){
    System.out.println("FreshVend Simulator: temp <number> | jam on/off | sensor on/off | exit on/off | status | quit");
    ScheduledExecutorService scheduler=Executors.newSingleThreadScheduledExecutor();
    scheduler.scheduleWithFixedDelay(()->{try{tick();}catch(Exception e){e.printStackTrace();}},0,2,TimeUnit.SECONDS);
    try(Scanner sc=new Scanner(System.in)){
      while(running&&sc.hasNextLine()){
        String[] p=sc.nextLine().trim().toLowerCase(Locale.ROOT).split("\\s+");
        try{
          if(p[0].equals("temp")&&p.length==2)temp=Double.parseDouble(p[1]);
          else if(p[0].equals("jam")&&p.length==2)jam=p[1].equals("on");
          else if(p[0].equals("sensor")&&p.length==2)sensorOk=p[1].equals("on");
          else if(p[0].equals("exit")&&p.length==2)exitSensor=p[1].equals("on");
          else if(p[0].equals("status"))System.out.println("temp="+temp+" jam="+jam+" sensor="+sensorOk+" exit="+exitSensor+" pending="+pending.size());
          else if(p[0].equals("quit"))running=false;
          else System.out.println("Unknown command");
        }catch(Exception e){System.out.println("Invalid input: "+e.getMessage());}
      }
    }
    scheduler.shutdownNow();
  }
}
