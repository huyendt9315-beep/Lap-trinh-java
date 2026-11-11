import com.sun.net.httpserver.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.*;

/** Demo ONLY: in-memory FIFO batches, idempotent results, claim-until-result. Not Spring Boot. */
public class DemoBackend {
  static final String DEVICE="FRESHVEND-001",TOKEN="demo-device-token",ADMIN="demo-admin-token";
  static final class Batch {String id;int qty;Batch(String id,int qty){this.id=id;this.qty=qty;}}
  static final class Command {String id,batch,status="QUEUED",result="";Command(String id,String batch){this.id=id;this.batch=batch;}}
  static final Deque<Batch> inventory=new ArrayDeque<>();
  static final LinkedHashMap<String,Command> commands=new LinkedHashMap<>();
  static final AtomicInteger ids=new AtomicInteger();
  static volatile String lastTelemetry="{}";
  static {inventory.add(new Batch("lot-20261101",2));inventory.add(new Batch("lot-20261105",3));}
  static String field(String json,String key){Matcher m=Pattern.compile("\\\""+Pattern.quote(key)+"\\\"\\s*:\\s*\\\"([^\\\"]*)\\\"").matcher(json);return m.find()?m.group(1):"";}
  static String escape(String s){return s.replace("\\","\\\\").replace("\"","\\\"");}
  static void reply(HttpExchange x,int code,String json)throws Exception{
    byte[] data=json.getBytes(StandardCharsets.UTF_8);x.getResponseHeaders().set("Content-Type","application/json; charset=utf-8");
    x.sendResponseHeaders(code,data.length);try(var out=x.getResponseBody()){out.write(data);}
  }
  static String body(HttpExchange x)throws Exception{return new String(x.getRequestBody().readAllBytes(),StandardCharsets.UTF_8);}
  static boolean deviceAuth(HttpExchange x){return DEVICE.equals(x.getRequestHeaders().getFirst("X-Device-Id"))&&TOKEN.equals(x.getRequestHeaders().getFirst("X-Device-Token"));}
  static boolean adminAuth(HttpExchange x){return ADMIN.equals(x.getRequestHeaders().getFirst("X-Admin-Token"));}
  static synchronized String next(){
    for(Command c:commands.values())if(!c.status.equals("FINISHED")){
      c.status="CLAIMED";return "{\"commandId\":\""+c.id+"\",\"type\":\"DISPENSE\",\"batchId\":\""+c.batch+"\"}";
    }
    return "{}";
  }
  static synchronized String create(){
    for(Command c:commands.values())if(!c.status.equals("FINISHED"))return "{\"error\":\"device busy\"}";
    while(!inventory.isEmpty()&&inventory.peek().qty<=0)inventory.remove();
    if(inventory.isEmpty())return "{\"error\":\"out of stock\"}";
    String id="cmd-"+ids.incrementAndGet();Command c=new Command(id,inventory.peek().id);commands.put(id,c);
    return "{\"commandId\":\""+id+"\",\"batchId\":\""+c.batch+"\",\"queued\":true}";
  }
  static synchronized String acceptResult(String b){
    String id=field(b,"commandId"),status=field(b,"result");Command c=commands.get(id);
    if(c==null)return "{\"error\":\"unknown command\"}";
    if(c.status.equals("FINISHED"))return "{\"accepted\":true,\"duplicate\":true}";
    if(!c.status.equals("CLAIMED"))return "{\"error\":\"command not claimed\"}";
    if(!Set.of("SUCCESS","JAM_SUSPECTED","UNVERIFIED").contains(status))return "{\"error\":\"invalid result\"}";
    if("SUCCESS".equals(status)&&!b.contains("\"verified\":true"))return "{\"error\":\"success requires verification\"}";
    if("SUCCESS".equals(status)){
      Batch head=inventory.peek();if(head==null||!head.id.equals(c.batch)||head.qty<=0)return "{\"error\":\"inventory mismatch\"}";
      head.qty--;if(head.qty==0)inventory.remove();
    }
    c.result=status;c.status="FINISHED";
    System.out.println("[RESULT] "+id+" "+status+" batch="+c.batch);
    return "{\"accepted\":true}";
  }
  static synchronized String status(){
    StringBuilder b=new StringBuilder("{\"telemetry\":"+lastTelemetry+",\"batches\":[");
    boolean first=true;for(Batch lot:inventory){if(!first)b.append(',');first=false;b.append("{\"batchId\":\"").append(lot.id).append("\",\"quantity\":").append(lot.qty).append('}');}
    b.append("],\"commands\":[");first=true;
    for(Command c:commands.values()){if(!first)b.append(',');first=false;b.append("{\"commandId\":\"").append(c.id).append("\",\"status\":\"").append(c.status).append("\",\"result\":\"").append(c.result).append("\"}");}
    return b.append("]}").toString();
  }
  static void handle(HttpExchange x){
    try{
      String p=x.getRequestURI().getPath(),m=x.getRequestMethod();
      if(p.startsWith("/api/v1/iot/")&&!deviceAuth(x)){reply(x,401,"{\"error\":\"unauthorized device\"}");return;}
      if(p.equals("/api/v1/iot/telemetry")&&m.equals("POST")){
        String b=body(x);if(!DEVICE.equals(field(b,"deviceId"))){reply(x,400,"{\"error\":\"device mismatch\"}");return;}
        lastTelemetry=b;System.out.println("[TELEMETRY] "+b);reply(x,200,"{\"accepted\":true}");
      }else if(p.equals("/api/v1/iot/commands/next")&&m.equals("GET"))reply(x,200,next());
      else if(p.equals("/api/v1/iot/dispense-result")&&m.equals("POST")){
        String b=body(x);if(!DEVICE.equals(field(b,"deviceId"))){reply(x,400,"{\"error\":\"device mismatch\"}");return;}
        String r=acceptResult(b);reply(x,r.contains("\"error\"")?409:200,r);
      }else if(p.equals("/demo/commands")&&m.equals("POST")){
        if(!adminAuth(x)){reply(x,401,"{\"error\":\"unauthorized admin\"}");return;}
        String r=create();reply(x,r.contains("\"error\"")?409:200,r);
      }else if(p.equals("/demo/status")&&m.equals("GET"))reply(x,200,status());
      else reply(x,404,"{\"error\":\"not found\"}");
    }catch(Exception e){e.printStackTrace();try{reply(x,500,"{\"error\":\"internal\"}");}catch(Exception ignored){}}
    finally{x.close();}
  }
  public static void main(String[] args)throws Exception{
    HttpServer s=HttpServer.create(new InetSocketAddress("127.0.0.1",Integer.getInteger("server.port",8080)),0);
    s.createContext("/",DemoBackend::handle);s.setExecutor(Executors.newCachedThreadPool());s.start();
    System.out.println("DemoBackend running at http://127.0.0.1:8080");
  }
}
