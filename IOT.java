import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;

/** FreshVend device simulator; follows the team's 05_API-Specification.md. Java 17+. */
public class IoTSimulator {
  static final String BASE=System.getProperty("backend.url","http://127.0.0.1:8080/api/v1");
  static final String MACHINE=System.getProperty("machine.id","1");
  static final String TOKEN=System.getProperty("device.token","demo-device-token");
  static final HttpClient HTTP=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
  static volatile double temp=5, humidity=65;
  static volatile boolean sensor=true, jam=false, door=false, running=true;
  static final double MIN_TEMP=Double.parseDouble(System.getProperty("temp.min","2"));
  static final double MAX_TEMP=Double.parseDouble(System.getProperty("temp.max","8"));
  static final Set<String> processed=ConcurrentHashMap.newKeySet();
  static final java.nio.file.Path STATE_FILE=java.nio.file.Path.of(System.getProperty("device.state.file","device-state.log"));
  static final java.nio.file.Path DONE_FILE=java.nio.file.Path.of(System.getProperty("device.done.file","processed-commands.log"));
  static synchronized void remember(String key,String event){
    try {
      String line=key+"|"+event+System.lineSeparator();
      java.nio.file.Files.writeString(STATE_FILE,line,java.nio.file.StandardOpenOption.CREATE,java.nio.file.StandardOpenOption.APPEND);
    }catch(Exception ex){throw new IllegalStateException("Cannot persist command; actuation disabled",ex);}
    try{java.nio.file.Files.writeString(DONE_FILE,key+System.lineSeparator(),java.nio.file.StandardOpenOption.CREATE,java.nio.file.StandardOpenOption.APPEND);}catch(Exception ex){throw new IllegalStateException("Cannot persist processed key",ex);}
    processed.add(key);pending.put(key,event);
  }
  static void restore(){
    try{if(java.nio.file.Files.exists(DONE_FILE))processed.addAll(java.nio.file.Files.readAllLines(DONE_FILE));}catch(Exception ex){throw new IllegalStateException("Cannot restore processed keys",ex);}
    if(!java.nio.file.Files.exists(STATE_FILE))return;
    try {for(String line:java.nio.file.Files.readAllLines(STATE_FILE)){
      int i=line.indexOf('|');if(i>0){String key=line.substring(0,i);processed.add(key);pending.put(key,line.substring(i+1));}
    }}catch(Exception ex){throw new IllegalStateException("Cannot restore state",ex);}
  }
  static synchronized void ack(String key){
    pending.remove(key);
    try {java.util.List<String> lines=new java.util.ArrayList<>();
      for(String line:java.nio.file.Files.readAllLines(STATE_FILE)){if(!line.startsWith(key+"|"))lines.add(line);}
      java.nio.file.Files.write(STATE_FILE,lines);
    }catch(Exception ex){System.err.println("[WARN] state file cleanup: "+ex.getMessage());}
  }
  static final Map<String,String> pending=new ConcurrentHashMap<>();
  static String path(){return "/machines/"+MACHINE;}
  static String extract(String s,String key){
    Matcher m=Pattern.compile("\\\""+Pattern.quote(key)+"\\\"\\s*:\\s*(?:\\\"([^\\\"]*)\\\"|(-?\\d+))").matcher(s);
    return m.find()?(m.group(1)!=null?m.group(1):m.group(2)):"";
  }
  static String call(String method,String endpoint,String json)throws Exception{
    HttpRequest.Builder b=HttpRequest.newBuilder(URI.create(BASE+endpoint)).timeout(Duration.ofSeconds(4))
      .header("Authorization","Bearer "+TOKEN);
    if(method.equals("POST")) b.header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(json,StandardCharsets.UTF_8));
    else b.GET();
    HttpResponse<String> r=HTTP.send(b.build(),HttpResponse.BodyHandlers.ofString());
    if(r.statusCode()/100!=2)throw new IllegalStateException("HTTP "+r.statusCode()+" "+r.body());
    return r.body();
  }
  static void tick(){
    try {
      if(!sensor){System.out.println("[SENSOR ERROR] DHT22 unavailable; no fabricated measurement sent");}
      else {
      String state="ONLINE";
      String payload=String.format(Locale.US,"{\"eventKey\":\"%s\",\"measuredAt\":\"%s\",\"tempC\":%.2f,\"humidityPct\":%.2f,\"doorOpen\":%s,\"deviceState\":\"%s\"}",UUID.randomUUID(),Instant.now(),temp,humidity,door,state);
      call("POST",path()+"/telemetry",payload);
      System.out.println("[TELEMETRY] "+temp+" C; sensor="+sensor+"; alert(local)="+(temp<MIN_TEMP||temp>MAX_TEMP));
      }
    } catch(Exception e){System.out.println("[OFFLINE] telemetry: "+e.getMessage());}
    // Re-send exactly the same eventKey on a failed acknowledgement; never actuate again.
    for(var entry:new ArrayList<>(pending.entrySet())){
      try{call("POST",path()+"/dispense-events",entry.getValue());ack(entry.getKey());System.out.println("[ACK] "+entry.getKey());}
      catch(Exception e){System.out.println("[PENDING ACK] "+entry.getKey()+": "+e.getMessage());}
    }
    try{
      String commands=call("GET",path()+"/commands?offset=0&limit=50","");
      Matcher m=Pattern.compile("\\{[^{}]*\\}").matcher(commands);
      while(m.find()){
        String cmd=m.group(),key=extract(cmd,"commandKey"),attempt=extract(cmd,"attemptNo"),slot=extract(cmd,"slotNo");
        if(key.isEmpty()||processed.contains(key))continue;
        if(attempt.isEmpty())attempt="1";
        if(slot.isEmpty())slot="?";
        System.out.println("[SERVO SIM] command="+key+" slot="+slot+" attempt="+attempt);
        // No physical exit sensor: NEVER claim delivery verified. Failure simulation is explicit.
        String result=jam?"JAMMED":"UNVERIFIED";
        String event=String.format(Locale.US,"{\"commandKey\":\"%s\",\"eventKey\":\"%s\",\"attemptNo\":%s,\"result\":\"%s\"}",key,UUID.randomUUID(),attempt,result);
        remember(key,event);
        try{call("POST",path()+"/dispense-events",event);ack(key);System.out.println("[RESULT] "+result+" (simulation, no exit sensor)");}
        catch(Exception e){System.out.println("[PENDING ACK] "+key+": "+e.getMessage());}
      }
    }catch(Exception e){System.out.println("[OFFLINE] commands: "+e.getMessage());}
  }
  public static void main(String[] args){
    restore();
    System.out.println("FreshVend simulator | backend="+BASE+" machine="+MACHINE+" temp thresholds="+MIN_TEMP+".."+MAX_TEMP);
    System.out.println("Commands: temp 10, temp 5, jam on/off, sensor on/off, door on/off, status, quit");
    ScheduledExecutorService scheduler=Executors.newSingleThreadScheduledExecutor();
    scheduler.scheduleWithFixedDelay(IoTSimulator::tick,0,3,TimeUnit.SECONDS);
    try(Scanner sc=new Scanner(System.in)){
      while(running&&sc.hasNextLine()){
        String[] p=sc.nextLine().trim().toLowerCase(Locale.ROOT).split("\\s+");
        try{
          if(p.length==2&&p[0].equals("temp"))temp=Double.parseDouble(p[1]);
          else if(p.length==2&&p[0].equals("jam"))jam=p[1].equals("on");
          else if(p.length==2&&p[0].equals("sensor"))sensor=p[1].equals("on");
          else if(p.length==2&&p[0].equals("door"))door=p[1].equals("on");
          else if(p[0].equals("status"))System.out.println("temp="+temp+", jam="+jam+", sensor="+sensor+", door="+door+", pending="+pending.size());
          else if(p[0].equals("quit"))running=false;
          else System.out.println("Unknown command");
        }catch(Exception e){System.out.println("Invalid input: "+e.getMessage());}
      }
    }finally{scheduler.shutdownNow();}
  }
}
