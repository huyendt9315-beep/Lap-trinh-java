/* FreshVend ESP32 reference firmware. Bench prototype only, NOT production safe.
   Libraries: DHT sensor library (Adafruit), ESP32Servo, ArduinoJson v6.
   Pins: DHT22 GPIO4, servo GPIO18, exit switch GPIO27 active LOW.
   Servo power MUST be external with common ground.
*/
#include <WiFi.h>
#include <HTTPClient.h>
#include <DHT.h>
#include <ESP32Servo.h>
#include <ArduinoJson.h>
#define DHTPIN 4
#define DHTTYPE DHT22
#define SERVO_PIN 18
#define EXIT_PIN 27
const char* WIFI_SSID="CHANGE_ME";
const char* WIFI_PASSWORD="CHANGE_ME";
const char* BASE="http://192.168.1.10:8080/api/v1/iot"; // local lab only; HTTPS required for deployment
const char* DEVICE="FRESHVEND-001";
const char* TOKEN="demo-device-token"; // demo only
DHT dht(DHTPIN,DHTTYPE);
Servo gate;
String lastCommand="";
String pendingPayload="";
unsigned long lastPoll=0;
int sendJson(const String& endpoint,const String& payload,String& response,bool post){
  if(WiFi.status()!=WL_CONNECTED)return -1;
  HTTPClient http;
  http.begin(String(BASE)+endpoint);
  http.addHeader("X-Device-Id",DEVICE);
  http.addHeader("X-Device-Token",TOKEN);
  if(post)http.addHeader("Content-Type","application/json");
  int code=post?http.POST(payload):http.GET();
  if(code>0)response=http.getString();
  http.end();return code;
}
void telemetry(){
  float t=dht.readTemperature(),h=dht.readHumidity();
  bool ok=!isnan(t)&&!isnan(h);
  StaticJsonDocument<256> d;
  d["deviceId"]=DEVICE;d["sensorOk"]=ok;
  if(ok){d["temperatureC"]=t;d["humidityPercent"]=h;}
  else{d["temperatureC"]=nullptr;d["humidityPercent"]=nullptr;}
  d["temperatureAlert"]=!ok||t<2||t>8;
  d["wifiConnected"]=WiFi.status()==WL_CONNECTED;
  d["dispenseSensorInstalled"]=true;
  String json,response;serializeJson(d,json);
  sendJson("/telemetry",json,response,true);
}
void processCommand(const String& id,const String& batch){
  if(id==lastCommand)return;
  // IMPORTANT: NVS persistence of executed IDs + pending reports is REQUIRED before real use.
  lastCommand=id;
  bool success=false;
  int attempts=0;
  // Only ONE mechanical attempt by default: never retry blindly after a jam.
  attempts=1;
  gate.write(85);delay(450);gate.write(0);
  unsigned long start=millis();
  while(millis()-start<2000){if(digitalRead(EXIT_PIN)==LOW){success=true;break;}delay(20);}
  StaticJsonDocument<256> d;
  d["deviceId"]=DEVICE;d["commandId"]=id;d["batchId"]=batch;
  d["result"]=success?"SUCCESS":"JAM_SUSPECTED";
  d["attempts"]=attempts;d["verified"]=success;
  pendingPayload="";serializeJson(d,pendingPayload);
}
void setup(){
  Serial.begin(115200);pinMode(EXIT_PIN,INPUT_PULLUP);dht.begin();
  gate.setPeriodHertz(50);gate.attach(SERVO_PIN,500,2400);gate.write(0);
  WiFi.begin(WIFI_SSID,WIFI_PASSWORD);
}
void loop(){
  if(millis()-lastPoll<2000)return;
  lastPoll=millis();
  if(WiFi.status()!=WL_CONNECTED){WiFi.reconnect();return;}
  telemetry();
  String response;
  if(pendingPayload.length()){
    int code=sendJson("/dispense-result",pendingPayload,response,true);
    if(code>=200&&code<300)pendingPayload="";
    return; // never execute another command until result acknowledged
  }
  int code=sendJson(String("/commands/next?deviceId=")+DEVICE,"",response,false);
  if(code!=200)return;
  StaticJsonDocument<256> d;
  if(deserializeJson(d,response))return;
  String id=d["commandId"]|"",type=d["type"]|"",batch=d["batchId"]|"";
  if(id.length()&&type=="DISPENSE")processCommand(id,batch);
}
