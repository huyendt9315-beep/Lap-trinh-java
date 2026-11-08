import com.sun.net.httpserver.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;
/** Standalone demo only; not a replacement for member 2's backend. */
public class DemoBackend {
  static final String TOKEN="demo-device-token", ADMIN="demo-admin-token";
  static final Queue<String> commands=new ConcurrentLinkedQueue<>();
  static final Map<String,String> events=new ConcurrentHashMap<>();
  static final Map<String,String> telemetry=new ConcurrentHashMap<>();
  static String field(String json,String key){Matcher m=Pattern.compile("\\\""+Pattern.quote(key)+"\\\"\\s*:\\s*\\\"([^\\\"]*)\\\"").matcher(json);return m.find()?m.group(1):"";}
  static void reply(HttpExchange x,int code,String json)throws Exception{
    byte[] data=json.getBytes(StandardCharsets.UTF_8);
    x.getResponseHeaders().set("Content-Type","application/json; charset=utf-8");
    x.sendResponseHeaders(code,data.length);try(var out=x.getResponseBody()){out.write(data);}
  }
  static String body(HttpExchange x)throws Exception{return new String(x.getRequestBody().readAllBytes(),StandardCharsets.UTF_8);}
  static void handle(HttpExchange x){
    try{
      String p=x.getRequestURI().getPath(),method=x.getRequestMethod();
      if(p.startsWith("/api/v1/")&&!"Bearer ".concat(TOKEN).equals(x.getRequestHeaders().getFirst("Authorization"))){reply(x,401,"{\"error\":\"unauthorized device\"}");return;}
      if(p.equals("/api/v1/machines/1/telemetry")&&method.equals("POST")){
        String b=body(x),id=field(b,"eventKey");if(id.isEmpty()){reply(x,400,"{\"error\":\"missing eventKey\"}");return;}
        telemetry.putIfAbsent(id,b);System.out.println("[TELEMETRY] "+b);reply(x,200,"{\"accepted\":true}");
      }else if(p.equals("/api/v1/machines/1/commands")&&method.equals("GET")){
        String cmd=commands.poll();reply(x,200,cmd==null?"[]":"["+cmd+"]");
      }else if(p.equals("/api/v1/machines/1/dispense-events")&&method.equals("POST")){
        String b=body(x),id=field(b,"eventKey");if(id.isEmpty()||field(b,"commandKey").isEmpty()){reply(x,400,"{\"error\":\"missing keys\"}");return;}
        events.putIfAbsent(id,b);System.out.println("[DISPENSE] "+b);reply(x,200,"{\"accepted\":true}");
      }else if(p.equals("/demo/commands")&&method.equals("POST")){
        if(!ADMIN.equals(x.getRequestHeaders().getFirst("X-Admin-Token"))){reply(x,401,"{\"error\":\"unauthorized admin\"}");return;}
        String key=UUID.randomUUID().toString();commands.add("{\"commandKey\":\""+key+"\",\"dispenseId\":12,\"slotNo\":2,\"attemptNo\":1}");
        reply(x,200,"{\"commandKey\":\""+key+"\",\"queued\":true}");
      }else if(p.equals("/demo/status")&&method.equals("GET")){
        reply(x,200,"{\"telemetryCount\":"+telemetry.size()+",\"pendingCommands\":"+commands.size()+",\"eventsCount\":"+events.size()+"}");
      }else reply(x,404,"{\"error\":\"not found\"}");
    }catch(Exception e){e.printStackTrace();try{reply(x,500,"{\"error\":\"internal\"}");}catch(Exception ignored){}}
    finally{x.close();}
  }
  public static void main(String[] args)throws Exception{
    int port=Integer.getInteger("server.port",8080);
    HttpServer s=HttpServer.create(new InetSocketAddress("127.0.0.1",port),0);
    s.createContext("/",DemoBackend::handle);s.setExecutor(Executors.newCachedThreadPool());s.start();
    System.out.println("DemoBackend http://127.0.0.1:"+port+" (machine 1)");
  }
}
