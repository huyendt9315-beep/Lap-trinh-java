# 07 – IoT Design | FreshVend (Thành viên 3)

## 1. Phạm vi và kiến trúc
Theo đề FreshVend: ESP32 giám sát nhiệt độ bằng DHT22, điều khiển servo để xuất từng khay, ghi nhận trạng thái/kẹt hàng, truyền dữ liệu về Java backend và dashboard. Bản bàn giao này gồm **mô phỏng Java chạy được** và **firmware ESP32 tham khảo**; chưa có bằng chứng kiểm thử phần cứng thật. Cần giảng viên đồng ý nếu dùng mô phỏng thay thiết bị thật.

```text
Khách chọn hàng -> thanh toán được BACKEND xác nhận
                 -> Backend tạo lệnh DISPENSE duy nhất
                 -> ESP32 (hoặc Java simulator) nhận lệnh
                 -> Servo mở/chốt -> cảm biến cửa ra xác nhận
                 -> gửi kết quả -> Backend cập nhật FIFO, giao dịch
DHT22 -> ESP32 -> POST telemetry -> Spring Boot -> DB -> WebSocket -> Dashboard
```

## 2. Thành phần và đấu nối đề xuất
| Thiết bị | ESP32 | Lưu ý |
|---|---|---|
| DHT22 DATA | GPIO 4 | điện trở kéo lên 10 kΩ về 3.3 V nếu module chưa có; cấp 3.3 V và GND |
| Servo tín hiệu PWM | GPIO 18 | cấp nguồn riêng phù hợp thông số servo, nối chung GND; **không** lấy nguồn servo trực tiếp từ chân ESP32 |
| Cảm biến xác nhận khay ra (digital) | GPIO 27 | INPUT_PULLUP; mức LOW khi phát hiện khay (tùy module, cần hiệu chỉnh) |
| ESP32 | USB/nguồn ổn định | kết nối Wi-Fi, HTTPS khi triển khai thật |

```text
ESP32 3V3 ---- DHT22 VCC
ESP32 GND ---- DHT22 GND ---- Servo GND ---- Sensor GND ---- PSU GND
ESP32 GPIO4 -- DHT22 DATA (+ pull-up 10k đến 3V3)
ESP32 GPIO18 ---------------- Servo PWM
Nguồn servo riêng ------------ Servo V+
ESP32 GPIO27 ---------------- Exit sensor OUT
ESP32 3V3 ------------------- Exit sensor VCC (nếu module hỗ trợ 3.3V)
```

**Lưu ý an toàn:** DHT22 chỉ đo nhiệt độ/độ ẩm, không điều khiển máy nén; không nối máy nén điện lưới trực tiếp với ESP32. Mô hình này chưa có cảm biến cửa tủ, công tắc giới hạn và hệ thống an toàn cơ khí. Chỉ thử cơ cấu servo ở mô hình nhỏ, không có người chạm vào bộ phận chuyển động.

## 3. FIFO theo lô
FIFO được **backend** quản lý theo `batchId` và thứ tự nhập kho (trong hệ thống thật dùng `receivedAt`, `expiryAt`, `columnId` và giao dịch DB). Demo có hai lô `lot-20261101` (2 khay), `lot-20261105` (3 khay); chỉ tạo lệnh từ lô đầu còn hàng. Lệnh mang `batchId`; backend chỉ trừ 1 khi nhận `SUCCESS` có `verified=true`. Với cơ cấu xếp chồng thực tế, FIFO chỉ đúng khi thứ tự vật lý được nạp phù hợp thứ tự lô trong DB; nếu một cột không thể chọn lô, backend không được tự giả định khả năng xuất lô khác.

## 4. Máy trạng thái xuất hàng
`QUEUED -> CLAIMED -> FINISHED(SUCCESS | JAM_SUSPECTED | UNVERIFIED)`.

- `commandId` là khóa chống xuất trùng. Lệnh đã CLAIMED có thể được trả lại khi thiết bị hỏi tiếp; thiết bị phải ghi nhớ đã thực thi để **không quay servo lần hai**.
- Chỉ xác nhận SUCCESS khi cảm biến cửa ra xác nhận. Không có cảm biến => UNVERIFIED, không tự trừ tồn.
- Trong mô phỏng, `jam on` biểu diễn kẹt liên tục qua tối đa 2 lần thử; ngoài đời **không tự thử lại chuyển động** nếu chưa xác nhận cơ cấu an toàn, tránh xuất trùng hoặc làm hỏng hàng.
- Khi gửi kết quả thất bại, thử lại **HTTP gửi kết quả**, không lặp lại xuất hàng. Mô phỏng lưu hàng đợi kết quả trong RAM; thiết bị thật cần lưu bền vững (NVS/flash có giới hạn ghi) và quy trình đối soát khi khởi động lại.
- Nếu lỗi kẹt: khóa luồng bán cho cột liên quan, cảnh báo vận hành và chỉ cho phép nhân viên xử lý, xác nhận lại; hoàn tiền do backend nghiệp vụ phụ trách.

## 5. Cảnh báo và chế độ lỗi
- Nhiệt độ ngoài 2–8°C: `temperatureAlert=true`, dashboard cần thông báo và cân nhắc dừng bán; ngưỡng chỉ là mục tiêu đề bài, phải xác định theo yêu cầu bảo quản thực tế.
- `sensorOk=false`: báo lỗi DHT22, không suy luận rằng nhiệt độ an toàn.
- `JAM_SUSPECTED` / `UNVERIFIED`: gửi cảnh báo, không trừ tồn tự động.
- Mất Wi-Fi/backend: ghi nhận offline; không nhận lệnh mới. Cần cơ chế timeout, đối soát và phát lại kết quả khi nối lại.
- Không có cảm biến cửa tủ trong bản demo: `doorStatus` chưa được triển khai; không báo cáo là đã có.

## 6. Quy trình hoạt động
1. Operator nhập lô và xác nhận bố trí vật lý phù hợp FIFO.
2. Khách thanh toán; backend xác minh giao dịch và tạo lệnh duy nhất cho cột/lô.
3. Thiết bị lấy lệnh; kiểm tra cảm biến, nhiệt độ, trạng thái cơ cấu và khóa an toàn.
4. Servo vận hành; cảm biến cửa ra xác minh khay đã đi qua; giới hạn thời gian chờ.
5. Thành công: gửi SUCCESS verified; thất bại: JAM_SUSPECTED/UNVERIFIED và cảnh báo.
6. Backend lưu kết quả idempotent, cập nhật tồn kho/đơn hàng, phát sự kiện WebSocket cho UI.


