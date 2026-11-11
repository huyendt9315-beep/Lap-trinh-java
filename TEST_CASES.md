# Test plan – kết quả mong đợi 
| ID | Thao tác | Kỳ vọng |
|---|---|---|
| T01 | Chạy demo + simulator | telemetry mỗi 2 giây, backend nhận dữ liệu |
| T02 | `temp 10`, sau đó `temp 5` | cảnh báo nhiệt bật rồi tắt |
| T03 | `sensor off` | `sensorOk=false`, cảnh báo bật |
| T04 | `POST /demo/commands` | có lệnh và kết quả SUCCESS verified nếu exit on, jam off |
| T05 | `jam on` rồi tạo lệnh | JAM_SUSPECTED, attempts=2; không trừ tồn |
| T06 | `exit off` rồi tạo lệnh | UNVERIFIED; không trừ tồn |
| T07 | Tạo 2 lệnh SUCCESS tuần tự | lô cũ giảm 2 rồi lô tiếp theo được chọn (FIFO) |
| T08 | Gửi lại cùng dispense-result | accepted duplicate, không trừ tồn hai lần |
| T09 | Backend tạm mất kết nối lúc gửi kết quả | simulator thử gửi lại HTTP; không chạy servo lại |
| T10 | Restart simulator sau khi đã chạy servo | **giới hạn**: RAM mất processed/pending, cần NVS để đảm bảo thực tế |
| T11 | Dùng Spring Boot của TV2 | **chưa kiểm thử**: cần ghép API, DB, WebSocket, security |
