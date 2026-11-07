# FreshVend – Đặc tả REST API

## 1. Quy ước đọc tài liệu

Đường dẫn gốc của các API nghiệp vụ là `/api/v1`.
Ví dụ, `GET /machines/3/catalog` trong tài liệu tương ứng với yêu cầu
`GET /api/v1/machines/3/catalog` để xem các sản phẩm đang bán tại máy số 3.

Các giá trị trong dấu ngoặc nhọn được thay bằng ID thực tế.
Ví dụ, `{machineId}` là ID của máy, `{orderId}` là ID của đơn hàng.

Hai đường dẫn `/oauth2/authorize` và `/oauth2/token` thuộc dịch vụ xác thực
và được sử dụng trực tiếp, không thêm `/api/v1`.

## 2. Danh sách API

### 2.1. Đăng nhập

| Phương thức | Đường dẫn | Chức năng |
|---|---|---|
| GET | `/oauth2/authorize` | Bắt đầu đăng nhập nhân viên. Sau khi đăng nhập thành công, ứng dụng nhận mã ủy quyền. |
| POST | `/oauth2/token` | Ứng dụng gửi mã ủy quyền và mã kiểm chứng PKCE để nhận access token dạng JWT. |
| GET | `/me` | Xem thông tin và vai trò của nhân viên đang đăng nhập. |

### 2.2. Sản phẩm

| Phương thức | Đường dẫn | Chức năng |
|---|---|---|
| GET | `/products` | Nhân viên xem danh mục sản phẩm của hệ thống. |
| POST | `/products` | Quản trị viên thêm sản phẩm mới. |
| PATCH | `/products/{productId}/price` | Cập nhật giá bán của một sản phẩm. |
| GET | `/machines/{machineId}/catalog` | Màn hình bán hàng hiển thị sản phẩm, giá bán và tình trạng còn hàng tại máy khách đang sử dụng. |

### 2.3. Tồn kho

| Phương thức | Đường dẫn | Chức năng |
|---|---|---|
| GET | `/machines/{machineId}/slots` | Xem các ngăn của máy, sản phẩm trong từng ngăn và số lượng còn lại. |
| PUT | `/slots/{slotId}/assignment` | Gán sản phẩm cho một ngăn. Chỉ đổi sang sản phẩm khác khi ngăn đã rỗng và không còn hàng đang được giữ cho đơn. |
| POST | `/slots/{slotId}/lots` | Ghi nhận lô hàng vừa nhập vào ngăn, gồm số lượng, hạn sử dụng và nhân viên nhập hàng. |
| GET | `/slots/{slotId}/lots` | Xem các lô đã nhập vào ngăn và số lượng còn lại của từng lô. |
| POST | `/lots/{lotId}/removals` | Ghi nhận việc nhân viên đã lấy hàng hết hạn của một lô ra khỏi máy. |

### 2.4. Giao dịch mua hàng

| Phương thức | Đường dẫn | Chức năng |
|---|---|---|
| POST | `/machines/{machineId}/orders` | Tạo đơn hàng tại máy và giữ số lượng sản phẩm khách đã chọn. |
| POST | `/orders/{orderId}/payments` | Tạo một lần thanh toán cho đơn hàng. |
| POST | `/gateway/payment-events` | Tiếp nhận kết quả thu tiền do cổng thanh toán gửi về. |
| GET | `/machines/{machineId}/commands` | Thiết bị lấy các lệnh giao sản phẩm đang chờ thực hiện. |
| POST | `/machines/{machineId}/dispense-events` | Thiết bị báo kết quả mỗi lần đưa sản phẩm ra khay, ví dụ giao thành công hoặc kẹt hàng. |
| POST | `/gateway/refund-events` | Tiếp nhận kết quả hoàn tiền do cổng thanh toán gửi về. |
| GET | `/orders/{orderId}` | Tra cứu trạng thái đơn, biên nhận và kết quả giao từng sản phẩm. |
| GET | `/transactions` | Quản trị viên tra cứu lịch sử giao dịch của hệ thống. |

### 2.5. Nhật ký nhiệt độ và cảnh báo

| Phương thức | Đường dẫn | Chức năng |
|---|---|---|
| POST | `/machines/{machineId}/telemetry` | Thiết bị gửi nhiệt độ, độ ẩm, trạng thái cửa và thời điểm đo về hệ thống. |
| GET | `/machines/{machineId}/telemetry` | Nhân viên xem lại dữ liệu cảm biến của máy theo thời gian. |
| PUT | `/machines/{machineId}/cold-chain` | Quản trị viên cấu hình ngưỡng cảnh báo nhiệt độ và nội dung hiển thị khi máy làm lạnh kém. |
| GET | `/machines/{machineId}/alerts` | Xem các cảnh báo của máy, bao gồm cảnh báo nhiệt độ. |
| POST | `/alerts/{alertId}/resolution` | Nhân viên ghi nhận cách xử lý và xác nhận sự cố đã được giải quyết. |

## 3. Chi tiết từng endpoint

Các ví dụ là hợp đồng thiết kế cho frontend, thiết bị và cổng thanh toán. Địa chỉ trang đăng nhập và các giá trị trong dấu <...> là ví dụ. ID, số tiền và thời điểm là dữ liệu minh họa.

### 3.1. Bắt đầu đăng nhập nhân viên

**GET `/oauth2/authorize`**. Frontend chuyển trình duyệt tới dịch vụ xác thực. Query bắt buộc trong thiết kế gồm `response_type=code`, `client_id`, `redirect_uri` đã đăng ký, `state` ngẫu nhiên, `code_challenge` và `code_challenge_method=S256`. Không gửi body.

```http
GET /oauth2/authorize?response_type=code&client_id=freshvend-web&redirect_uri=https%3A%2F%2Fapp.example%2Fcallback&state=<state>&code_challenge=<challenge>&code_challenge_method=S256
```

Sau khi người dùng đăng nhập và chấp thuận, ví dụ chuyển hướng thành công:

```http
HTTP/1.1 302 Found
Location: https://app.example/callback?code=<authorization-code>&state=<state>
```

Frontend kiểm tra state trước khi đổi code. Nếu người dùng từ chối, callback hợp lệ nhận `error=access_denied`. Nếu redirect_uri không hợp lệ, dịch vụ hiển thị lỗi và không chuyển hướng về địa chỉ đó.

### 3.2. Đổi mã ủy quyền lấy access token

**POST `/oauth2/token`**. Body dạng form gồm `grant_type`, `client_id`, `code`, `redirect_uri` và `code_verifier`. Dùng đúng redirect_uri và verifier của lần đăng nhập.

```http
POST /oauth2/token
Content-Type: application/x-www-form-urlencoded

grant_type=authorization_code&client_id=freshvend-web&code=<authorization-code>&redirect_uri=https%3A%2F%2Fapp.example%2Fcallback&code_verifier=<verifier>
```

Ví dụ thành công: `200 OK`.

```json
{"access_token":"<jwt>","token_type":"Bearer","expires_in":3600}
```

Ví dụ mã đã hết hạn hoặc verifier sai: `400 Bad Request`, body `{"error":"invalid_grant"}`. Thời hạn 3600 giây trong ví dụ chỉ để minh họa.

### 3.3. Tài khoản hiện tại

**GET `/api/v1/me`** · Quyền gọi: ADMIN/OPERATOR/MAINTENANCE.

Tham số ngoài body: Không có.

Request không có body.

**Request mẫu**

```http
GET /api/v1/me
Authorization: Bearer <access_token>
```

**Response mẫu · HTTP 200**

```json
{
  "id": 2,
  "login": "nv01",
  "role": "OPERATOR",
  "active": true
}
```

Mã lỗi trong OpenAPI: 400, 401, 403, 404, 409, 503. Xem bảng xử lý bên dưới.

### 3.4. Cấu hình cảnh báo

**PUT `/api/v1/machines/{machineId}/cold-chain`** · Quyền gọi: ADMIN.

Tham số ngoài body: `machineId` (path, bắt buộc).

Trường body bắt buộc: `minC`, `maxC`, `degradedMessage`.

**Request mẫu**

```http
PUT /api/v1/machines/1/cold-chain
Authorization: Bearer <access_token>
Content-Type: application/json

{"minC": 2, "maxC": 8, "degradedMessage": "Máy tạm dừng bán"}
```

**Response mẫu · HTTP 200**

```json
{
  "minC": 2,
  "maxC": 8,
  "degradedMessage": "Máy tạm dừng bán"
}
```

Mã lỗi trong OpenAPI: 400, 401, 403, 404, 409, 503. Xem bảng xử lý bên dưới.

### 3.5. Danh mục sản phẩm

**GET `/api/v1/products`** · Quyền gọi: ADMIN/OPERATOR.

Tham số ngoài body: `offset` (query, tùy chọn), `limit` (query, tùy chọn).

Request không có body.

**Request mẫu**

```http
GET /api/v1/products?offset=0&limit=50
Authorization: Bearer <access_token>
```

**Response mẫu · HTTP 200**

```json
[
  {
    "id": 9,
    "sku": "BOWL01",
    "name": "Trái cây hỗn hợp",
    "imageUrl": "https://example.org/bowl.jpg",
    "priceVnd": 30000,
    "active": true
  }
]
```

Mã lỗi trong OpenAPI: 400, 401, 403, 404, 409, 503. Xem bảng xử lý bên dưới.

### 3.6. Thêm sản phẩm

**POST `/api/v1/products`** · Quyền gọi: ADMIN.

Tham số ngoài body: Không có.

Trường body bắt buộc: `sku`, `name`, `imageUrl`, `priceVnd`.

**Request mẫu**

```http
POST /api/v1/products
Authorization: Bearer <access_token>
Content-Type: application/json

{"sku": "BOWL01", "name": "Trái cây hỗn hợp", "imageUrl": "https://example.org/bowl.jpg", "priceVnd": 30000}
```

**Response mẫu · HTTP 201**

```json
{
  "id": 9,
  "sku": "BOWL01",
  "name": "Trái cây hỗn hợp",
  "imageUrl": "https://example.org/bowl.jpg",
  "priceVnd": 30000,
  "active": true
}
```

Mã lỗi trong OpenAPI: 400, 401, 403, 404, 409, 503. Xem bảng xử lý bên dưới.

### 3.7. Điều chỉnh giá

**PATCH `/api/v1/products/{productId}/price`** · Quyền gọi: ADMIN/OPERATOR.

Tham số ngoài body: `productId` (path, bắt buộc).

Trường body bắt buộc: `priceVnd`.

**Request mẫu**

```http
PATCH /api/v1/products/9/price
Authorization: Bearer <access_token>
Content-Type: application/json

{"priceVnd": 30000}
```

**Response mẫu · HTTP 200**

```json
{
  "id": 9,
  "sku": "BOWL01",
  "name": "Trái cây hỗn hợp",
  "imageUrl": "https://example.org/bowl.jpg",
  "priceVnd": 30000,
  "active": true
}
```

Mã lỗi trong OpenAPI: 400, 401, 403, 404, 409, 503. Xem bảng xử lý bên dưới.

### 3.8. Xem tồn từng ngăn

**GET `/api/v1/machines/{machineId}/slots`** · Quyền gọi: ADMIN/OPERATOR/MAINTENANCE.

Tham số ngoài body: `machineId` (path, bắt buộc), `offset` (query, tùy chọn), `limit` (query, tùy chọn).

Request không có body.

**Request mẫu**

```http
GET /api/v1/machines/1/slots?offset=0&limit=50
Authorization: Bearer <access_token>
```

**Response mẫu · HTTP 200**

```json
[
  {
    "id": 4,
    "machineId": 1,
    "slotNo": 2,
    "productId": 9,
    "capacity": 12,
    "lowThreshold": 2,
    "availableQty": 6
  }
]
```

Mã lỗi trong OpenAPI: 400, 401, 403, 404, 409, 503. Xem bảng xử lý bên dưới.

### 3.9. Gán sản phẩm vào ngăn

**PUT `/api/v1/slots/{slotId}/assignment`** · Quyền gọi: ADMIN/OPERATOR.

Tham số ngoài body: `slotId` (path, bắt buộc).

Trường body bắt buộc: `productId`, `lowThreshold`.

**Request mẫu**

```http
PUT /api/v1/slots/4/assignment
Authorization: Bearer <access_token>
Content-Type: application/json

{"productId": 9, "lowThreshold": 2}
```

**Response mẫu · HTTP 200**

```json
{
  "id": 4,
  "machineId": 1,
  "slotNo": 2,
  "productId": 9,
  "capacity": 12,
  "lowThreshold": 2,
  "availableQty": 0
}
```

Mã lỗi trong OpenAPI: 400, 401, 403, 404, 409, 503. Xem bảng xử lý bên dưới.

### 3.10. Nhập hàng và ghi người nhập

**POST `/api/v1/slots/{slotId}/lots`** · Quyền gọi: ADMIN/OPERATOR.

Tham số ngoài body: `slotId` (path, bắt buộc), `Idempotency-Key` (header, bắt buộc).

Trường body bắt buộc: `productId`, `qtyIn`, `expiresAt`.

**Request mẫu**

```http
POST /api/v1/slots/4/lots
Authorization: Bearer <access_token>
Idempotency-Key: 123e4567-e89b-42d3-a456-426614174000
Content-Type: application/json

{"productId": 9, "qtyIn": 6, "expiresAt": "2026-11-09T10:00:00Z"}
```

**Response mẫu · HTTP 201**

```json
{
  "id": 8,
  "slotId": 4,
  "productId": 9,
  "operatorId": 2,
  "qtyIn": 6,
  "qtyOnHand": 6,
  "receivedAt": "2026-11-07T10:00:00Z",
  "expiresAt": "2026-11-09T10:00:00Z"
}
```

Mã lỗi trong OpenAPI: 400, 401, 403, 404, 409, 503. Xem bảng xử lý bên dưới.

### 3.11. Xem lịch sử nhập

**GET `/api/v1/slots/{slotId}/lots`** · Quyền gọi: ADMIN/OPERATOR.

Tham số ngoài body: `slotId` (path, bắt buộc), `offset` (query, tùy chọn), `limit` (query, tùy chọn).

Request không có body.

**Request mẫu**

```http
GET /api/v1/slots/4/lots?offset=0&limit=50
Authorization: Bearer <access_token>
```

**Response mẫu · HTTP 200**

```json
[
  {
    "id": 8,
    "slotId": 4,
    "productId": 9,
    "operatorId": 2,
    "qtyIn": 6,
    "qtyOnHand": 6,
    "receivedAt": "2026-11-07T10:00:00Z",
    "expiresAt": "2026-11-09T10:00:00Z"
  }
]
```

Mã lỗi trong OpenAPI: 400, 401, 403, 404, 409, 503. Xem bảng xử lý bên dưới.

### 3.12. Xác nhận lấy hàng hết hạn

**POST `/api/v1/lots/{lotId}/removals`** · Quyền gọi: ADMIN/OPERATOR.

Tham số ngoài body: `lotId` (path, bắt buộc), `Idempotency-Key` (header, bắt buộc).

Trường body bắt buộc: `qty`, `reason`.

**Request mẫu**

```http
POST /api/v1/lots/8/removals
Authorization: Bearer <access_token>
Idempotency-Key: 123e4567-e89b-42d3-a456-426614174000
Content-Type: application/json

{"qty": 1, "reason": "EXPIRED"}
```

**Response mẫu · HTTP 200**

```json
{
  "lotId": 8,
  "qtyOnHand": 5
}
```

Mã lỗi trong OpenAPI: 400, 401, 403, 404, 409, 503. Xem bảng xử lý bên dưới.

### 3.13. Danh sách hiển thị cho khách

**GET `/api/v1/machines/{machineId}/catalog`** · Quyền gọi: KIOSK.

Tham số ngoài body: `machineId` (path, bắt buộc).

Request không có body.

**Request mẫu**

```http
GET /api/v1/machines/1/catalog
Authorization: Bearer <access_token>
```

**Response mẫu · HTTP 200**

```json
{
  "machineId": 1,
  "status": "ONLINE",
  "message": "",
  "products": [
    {
      "id": 9,
      "sku": "BOWL01",
      "name": "Trái cây hỗn hợp",
      "imageUrl": "https://example.org/bowl.jpg",
      "priceVnd": 30000,
      "active": true,
      "availableQty": 6
    }
  ]
}
```

Mã lỗi trong OpenAPI: 400, 401, 403, 404, 409, 503. Xem bảng xử lý bên dưới.

### 3.14. Tạo đơn và giữ hàng

**POST `/api/v1/machines/{machineId}/orders`** · Quyền gọi: KIOSK.

Tham số ngoài body: `machineId` (path, bắt buộc), `Idempotency-Key` (header, bắt buộc).

Trường body bắt buộc: `items`.

**Request mẫu**

```http
POST /api/v1/machines/1/orders
Authorization: Bearer <access_token>
Idempotency-Key: 123e4567-e89b-42d3-a456-426614174000
Content-Type: application/json

{"items": [{"productId": 9, "qty": 1}]}
```

**Response mẫu · HTTP 201** – đọc header `Order-Token` và giữ cho phiên mua này.

```json
{
  "id": 31,
  "receiptNo": "FV-31",
  "status": "AWAITING_PAYMENT",
  "totalVnd": 30000,
  "reservationUntil": "2026-11-07T10:05:00Z",
  "items": [
    {
      "productId": 9,
      "qty": 1,
      "unitPriceVnd": 30000
    }
  ]
}
```

Mã lỗi trong OpenAPI: 400, 401, 403, 404, 409, 503. Xem bảng xử lý bên dưới.

### 3.15. Khởi tạo thanh toán

**POST `/api/v1/orders/{orderId}/payments`** · Quyền gọi: KIOSK.

Tham số ngoài body: `orderId` (path, bắt buộc), `Order-Token` (header, bắt buộc), `Idempotency-Key` (header, bắt buộc).

Trường body bắt buộc: `method`.

**Request mẫu**

```http
POST /api/v1/orders/31/payments
Authorization: Bearer <access_token>
Order-Token: <token nhận khi tạo đơn>
Idempotency-Key: 123e4567-e89b-42d3-a456-426614174000
Content-Type: application/json

{"method": "QR"}
```

**Response mẫu · HTTP 201**

```json
{
  "id": 70,
  "orderId": 31,
  "amountVnd": 30000,
  "status": "PENDING",
  "checkoutUrl": "https://example.org/checkout/70"
}
```

Mã lỗi trong OpenAPI: 400, 401, 403, 404, 409, 503. Xem bảng xử lý bên dưới.

### 3.16. Tra đơn và biên nhận

**GET `/api/v1/orders/{orderId}`** · Quyền gọi: KIOSK.

Tham số ngoài body: `orderId` (path, bắt buộc), `Order-Token` (header, bắt buộc).

Request không có body.

**Request mẫu**

```http
GET /api/v1/orders/31
Authorization: Bearer <access_token>
Order-Token: <token nhận khi tạo đơn>
```

**Response mẫu · HTTP 200**

```json
{
  "id": 31,
  "receiptNo": "FV-31",
  "status": "AWAITING_PAYMENT",
  "totalVnd": 30000,
  "reservationUntil": "2026-11-07T10:05:00Z",
  "items": [
    {
      "productId": 9,
      "qty": 1,
      "unitPriceVnd": 30000
    }
  ],
  "payments": [
    {
      "id": 70,
      "orderId": 31,
      "amountVnd": 30000,
      "status": "PENDING",
      "checkoutUrl": "https://example.org/checkout/70"
    }
  ],
  "dispenses": [
    {
      "id": 12,
      "status": "RESERVED"
    }
  ],
  "refunds": []
}
```

Mã lỗi trong OpenAPI: 400, 401, 403, 404, 409, 503. Xem bảng xử lý bên dưới.

### 3.17. Cổng báo kết quả thu tiền

**POST `/api/v1/gateway/payment-events`** · Quyền gọi: GATEWAY.

Tham số ngoài body: Không có.

Trường body bắt buộc: `eventKey`, `paymentId`, `providerRef`, `amountVnd`, `status`.

**Request mẫu**

```http
POST /api/v1/gateway/payment-events
X-Signature: <chữ ký do cổng thanh toán tạo>
Content-Type: application/json

{"eventKey": "123e4567-e89b-42d3-a456-426614174000", "paymentId": 70, "providerRef": "TX70", "amountVnd": 30000, "status": "SUCCEEDED"}
```

**Response mẫu · HTTP 200**

```json
{
  "accepted": true
}
```

Mã lỗi trong OpenAPI: 400, 401, 403, 404, 409, 503. Xem bảng xử lý bên dưới.

### 3.18. Cổng xác nhận hoàn tiền

**POST `/api/v1/gateway/refund-events`** · Quyền gọi: GATEWAY.

Tham số ngoài body: Không có.

Trường body bắt buộc: `eventKey`, `refundId`, `providerRef`, `amountVnd`, `status`.

**Request mẫu**

```http
POST /api/v1/gateway/refund-events
X-Signature: <chữ ký do cổng thanh toán tạo>
Content-Type: application/json

{"eventKey": "123e4567-e89b-42d3-a456-426614174000", "refundId": 6, "providerRef": "RF6", "amountVnd": 30000, "status": "SUCCEEDED"}
```

**Response mẫu · HTTP 200**

```json
{
  "accepted": true
}
```

Mã lỗi trong OpenAPI: 400, 401, 403, 404, 409, 503. Xem bảng xử lý bên dưới.

### 3.19. Máy đọc các lệnh được phép chạy

**GET `/api/v1/machines/{machineId}/commands`** · Quyền gọi: DEVICE.

Tham số ngoài body: `machineId` (path, bắt buộc), `offset` (query, tùy chọn), `limit` (query, tùy chọn).

Request không có body.

**Request mẫu**

```http
GET /api/v1/machines/1/commands?offset=0&limit=50
Authorization: Bearer <access_token>
```

**Response mẫu · HTTP 200**

```json
[
  {
    "commandKey": "123e4567-e89b-42d3-a456-426614174000",
    "dispenseId": 12,
    "slotNo": 2,
    "attemptNo": 1
  }
]
```

Mã lỗi trong OpenAPI: 400, 401, 403, 404, 409, 503. Xem bảng xử lý bên dưới.

### 3.20. Máy báo kết quả cấp phát

**POST `/api/v1/machines/{machineId}/dispense-events`** · Quyền gọi: DEVICE.

Tham số ngoài body: `machineId` (path, bắt buộc).

Trường body bắt buộc: `commandKey`, `eventKey`, `attemptNo`, `result`.

**Request mẫu**

```http
POST /api/v1/machines/1/dispense-events
Authorization: Bearer <access_token>
Content-Type: application/json

{"commandKey": "123e4567-e89b-42d3-a456-426614174000", "eventKey": "123e4567-e89b-42d3-a456-426614174000", "attemptNo": 1, "result": "DELIVERED"}
```

**Response mẫu · HTTP 200**

```json
{
  "accepted": true
}
```

Mã lỗi trong OpenAPI: 400, 401, 403, 404, 409, 503. Xem bảng xử lý bên dưới.

### 3.21. Nhận mẫu cảm biến và cửa máy

**POST `/api/v1/machines/{machineId}/telemetry`** · Quyền gọi: DEVICE.

Tham số ngoài body: `machineId` (path, bắt buộc).

Trường body bắt buộc: `eventKey`, `measuredAt`, `tempC`, `humidityPct`, `doorOpen`, `deviceState`.

**Request mẫu**

```http
POST /api/v1/machines/1/telemetry
Authorization: Bearer <access_token>
Content-Type: application/json

{"eventKey": "123e4567-e89b-42d3-a456-426614174000", "measuredAt": "2026-11-07T10:00:00Z", "tempC": 8.1, "humidityPct": 65, "doorOpen": false, "deviceState": "ONLINE"}
```

**Response mẫu · HTTP 200**

```json
{
  "accepted": true
}
```

Mã lỗi trong OpenAPI: 400, 401, 403, 404, 409, 503. Xem bảng xử lý bên dưới.

### 3.22. Đọc nhật ký thiết bị

**GET `/api/v1/machines/{machineId}/telemetry`** · Quyền gọi: ADMIN/OPERATOR/MAINTENANCE.

Tham số ngoài body: `machineId` (path, bắt buộc), `offset` (query, tùy chọn), `limit` (query, tùy chọn).

Request không có body.

**Request mẫu**

```http
GET /api/v1/machines/1/telemetry?offset=0&limit=50
Authorization: Bearer <access_token>
```

**Response mẫu · HTTP 200**

```json
[
  {
    "eventKey": "123e4567-e89b-42d3-a456-426614174000",
    "measuredAt": "2026-11-07T10:00:00Z",
    "tempC": 8.1,
    "humidityPct": 65,
    "doorOpen": false,
    "deviceState": "ONLINE"
  }
]
```

Mã lỗi trong OpenAPI: 400, 401, 403, 404, 409, 503. Xem bảng xử lý bên dưới.

### 3.23. Xem cảnh báo

**GET `/api/v1/machines/{machineId}/alerts`** · Quyền gọi: ADMIN/OPERATOR/MAINTENANCE.

Tham số ngoài body: `machineId` (path, bắt buộc), `offset` (query, tùy chọn), `limit` (query, tùy chọn).

Request không có body.

**Request mẫu**

```http
GET /api/v1/machines/1/alerts?offset=0&limit=50
Authorization: Bearer <access_token>
```

**Response mẫu · HTTP 200**

```json
[
  {
    "id": 7,
    "machineId": 1,
    "kind": "HIGH_TEMP",
    "severity": "HIGH",
    "openedAt": "2026-11-07T10:00:00Z"
  }
]
```

Mã lỗi trong OpenAPI: 400, 401, 403, 404, 409, 503. Xem bảng xử lý bên dưới.

### 3.24. Đóng sự cố

**POST `/api/v1/alerts/{alertId}/resolution`** · Quyền gọi: ADMIN/OPERATOR/MAINTENANCE.

Tham số ngoài body: `alertId` (path, bắt buộc).

Trường body bắt buộc: `resolution`.

**Request mẫu**

```http
POST /api/v1/alerts/7/resolution
Authorization: Bearer <access_token>
Content-Type: application/json

{"resolution": "Đã kiểm tra máy"}
```

**Response mẫu · HTTP 200**

```json
{
  "id": 7,
  "resolvedBy": 2,
  "resolvedAt": "2026-11-07T10:00:00Z"
}
```

Mã lỗi trong OpenAPI: 400, 401, 403, 404, 409, 503. Xem bảng xử lý bên dưới.

### 3.25. Nhật ký giao dịch

**GET `/api/v1/transactions`** · Quyền gọi: ADMIN.

Tham số ngoài body: `offset` (query, tùy chọn), `limit` (query, tùy chọn).

Request không có body.

**Request mẫu**

```http
GET /api/v1/transactions?offset=0&limit=50
Authorization: Bearer <access_token>
```

**Response mẫu · HTTP 200**

```json
[
  {
    "id": 70,
    "orderId": 31,
    "amountVnd": 30000,
    "status": "PENDING",
    "checkoutUrl": "https://example.org/checkout/70"
  }
]
```

Mã lỗi trong OpenAPI: 400, 401, 403, 404, 409, 503. Xem bảng xử lý bên dưới.

### Xử lý lỗi và gửi lại yêu cầu

| Mã | Cách xử lý |
|---|---|
| 400 | Kiểm tra trường bắt buộc, kiểu dữ liệu và giá trị gửi lên. |
| 401 | Kiểm tra token hoặc chữ ký callback. |
| 403 | Tài khoản hoặc thiết bị thiếu quyền. |
| 404 | Không tìm thấy tài nguyên trong phạm vi được phép. |
| 409 | Xung đột trạng thái, hết hàng hoặc trùng khóa với nội dung khác. Đọc mã lỗi và tải lại dữ liệu. |
| 503 | Dịch vụ tạm thời chưa sẵn sàng. |

Ví dụ khi tạo đơn nhưng không còn đủ hàng:

```json
{"code":"OUT_OF_STOCK","message":"Ngăn không còn đủ sản phẩm để giữ chỗ.","traceId":"req-01"}
```

Khi gửi lại cùng thao tác có header `Idempotency-Key`, dùng lại UUID ban đầu. Với yêu cầu mới, tạo UUID mới. API có `offset` và `limit` dùng mặc định 0 và 50; giới hạn tối đa 200 bản ghi.

## 4. Đăng nhập, xác thực và phân quyền

### 4.1. Nhân viên đăng nhập và gọi API

FreshVend dùng OAuth 2.0 Authorization Code với PKCE (S256) để ứng dụng nhận quyền gọi API sau khi nhân viên đăng nhập. Access token có định dạng JWT. RBAC xác định những thao tác mỗi vai trò được phép thực hiện.

1. Giao diện tạo giá trị ngẫu nhiên `state` và `code_verifier` cho phiên đăng nhập. Từ verifier, giao diện tính `code_challenge = BASE64URL(SHA256(code_verifier))` và giữ verifier cho bước đổi mã.
2. Giao diện chuyển trình duyệt đến `GET /oauth2/authorize` với `response_type=code`, `client_id`, `redirect_uri` đã đăng ký, `state`, `code_challenge` và `code_challenge_method=S256`. Nhân viên nhập thông tin đăng nhập tại dịch vụ xác thực.
3. Sau khi đăng nhập thành công, trình duyệt quay về redirect_uri với `code` và `state`. Giao diện so sánh state với giá trị của phiên đã mở. Nếu sai, dừng đăng nhập.
4. Giao diện gửi form đến `POST /oauth2/token`, gồm `grant_type=authorization_code`, `client_id`, `redirect_uri`, `code` và `code_verifier`. Thành công nhận `access_token`, `token_type=Bearer`, `expires_in`. Ứng dụng web dùng PKCE và không chứa client secret trong mã frontend.
5. Khi gọi API, giao diện gửi JWT trong header Bearer qua HTTPS. Ví dụ kiểm tra tài khoản sau đăng nhập:

```http
GET /api/v1/me
Authorization: Bearer <access_token>
```

Spring Boot kiểm tra chữ ký bằng khóa của dịch vụ xác thực, thuật toán được phép, nơi phát hành (`iss`), đối tượng nhận (`aud`) và hạn dùng (`exp`). Với nhân viên, backend lấy danh tính từ `sub`, kiểm tra tài khoản đang hoạt động và đối chiếu quyền trong `roles` với vai trò hiện hành. Nếu tài khoản bị khóa hoặc đổi quyền, backend phải áp dụng trạng thái mới. Vai trò do giao diện tự gửi không cấp quyền. Khi token hết hạn, giao diện yêu cầu đăng nhập lại.

### 4.2. Phiên bán hàng, thiết bị và cổng thanh toán

| Bên gọi | Cách xác thực | Giới hạn truy cập |
|---|---|---|
| KIOSK – ứng dụng bán hàng tại máy | Dịch vụ tin cậy tại máy dùng thông tin client để nhận token qua `POST /oauth2/token` với `grant_type=client_credentials`. | Khách mua hàng không cần tài khoản nhân viên. Token ràng buộc với máy. Khi tạo đơn, kiosk nhận header `Order-Token` và gửi lại khi đọc hoặc thanh toán chính đơn đó. |
| DEVICE – bộ điều khiển máy | Dùng client_credentials của thiết bị để nhận token loại DEVICE. | Chỉ đọc lệnh và gửi sự kiện của chính máy ghi trong token. Backend đối chiếu machineId trên đường dẫn với machineId đã xác thực. |
| GATEWAY – cổng thanh toán | Gửi chữ ký trong header `X-Signature`; backend xác minh chữ ký trên nội dung gốc theo quy ước nhà cung cấp. | Chỉ gửi kết quả thanh toán và hoàn tiền. Backend kiểm tra mã giao dịch, số tiền và sự kiện đã xử lý trước khi cập nhật trạng thái. |

KIOSK và DEVICE có đối tượng nhận token riêng. Thông tin xác thực client được giữ trong dịch vụ tin cậy tại máy hoặc bộ điều khiển. Các đường dẫn OAuth ở trên nằm ngoài tiền tố `/api/v1`; quyền sử dụng chúng phụ thuộc loại client và luồng cấp token.

### 4.3. Bảng phân quyền theo từng endpoint

Ba vai trò nhân viên là ADMIN (quản trị viên), OPERATOR (nhân viên vận hành) và MAINTENANCE (nhân viên bảo trì). KIOSK, DEVICE và GATEWAY là các bên gọi kỹ thuật. Tất cả đường dẫn trong bảng dùng tiền tố `/api/v1`. Dấu ✓ nghĩa là được gọi khi đáp ứng các điều kiện ở mục 4.1–4.2; dấu — nghĩa là không được gọi.

| Phương thức và đường dẫn | ADMIN | OPERATOR | MAINTENANCE | KIOSK | DEVICE | GATEWAY |
|---|---|---|---|---|---|---|
| `GET /me` | ✓ | ✓ | ✓ | — | — | — |
| `GET /users` | ✓ | — | — | — | — | — |
| `POST /users` | ✓ | — | — | — | — | — |
| `PATCH /users/{userId}` | ✓ | — | — | — | — | — |
| `GET /machines` | ✓ | ✓ | ✓ | — | — | — |
| `PUT /machines/{machineId}/cold-chain` | ✓ | — | — | — | — | — |
| `GET /products` | ✓ | ✓ | — | — | — | — |
| `POST /products` | ✓ | — | — | — | — | — |
| `PATCH /products/{productId}/price` | ✓ | ✓ | — | — | — | — |
| `GET /machines/{machineId}/slots` | ✓ | ✓ | ✓ | — | — | — |
| `PUT /slots/{slotId}/assignment` | ✓ | ✓ | — | — | — | — |
| `POST /slots/{slotId}/lots` | ✓ | ✓ | — | — | — | — |
| `GET /slots/{slotId}/lots` | ✓ | ✓ | — | — | — | — |
| `POST /lots/{lotId}/removals` | ✓ | ✓ | — | — | — | — |
| `GET /machines/{machineId}/catalog` | — | — | — | ✓ | — | — |
| `POST /machines/{machineId}/orders` | — | — | — | ✓ | — | — |
| `POST /orders/{orderId}/payments` | — | — | — | ✓ | — | — |
| `GET /orders/{orderId}` | — | — | — | ✓ | — | — |
| `POST /gateway/payment-events` | — | — | — | — | — | ✓ |
| `POST /gateway/refund-events` | — | — | — | — | — | ✓ |
| `GET /machines/{machineId}/commands` | — | — | — | — | ✓ | — |
| `POST /machines/{machineId}/dispense-events` | — | — | — | — | ✓ | — |
| `POST /machines/{machineId}/telemetry` | — | — | — | — | ✓ | — |
| `GET /machines/{machineId}/telemetry` | ✓ | ✓ | ✓ | — | — | — |
| `GET /machines/{machineId}/alerts` | ✓ | ✓ | ✓ | — | — | — |
| `POST /alerts/{alertId}/resolution` | ✓ | ✓ | ✓ | — | — | — |
| `GET /transactions` | ✓ | — | — | — | — | — |
| `GET /reports/sales` | ✓ | — | — | — | — | — |
| `GET /reports/export` | ✓ | — | — | — | — | — |

### 4.4. Kết quả kiểm tra quyền

| Tình huống | Kết quả mong đợi |
|---|---|
| OPERATOR gọi API đọc nhật ký nhiệt độ với token hợp lệ. | `200 OK`, trả nhật ký. |
| OPERATOR gọi `GET /api/v1/transactions`, chỉ dành cho ADMIN. | `403 Forbidden`, không trả lịch sử giao dịch. |
| Gọi API nhân viên mà thiếu token, token hết hạn hoặc sai chữ ký. | `401 Unauthorized`. |
| DEVICE của máy 1 gửi số đo vào đường dẫn của máy 2. | `403 Forbidden`, không ghi số đo. |
| Cổng thanh toán gửi callback có chữ ký sai. | `401 Unauthorized`, không cập nhật giao dịch. |

Các trường hợp trên là tiêu chí để kiểm thử khi triển khai backend.

Tài liệu tham khảo: [OAuth 2.0 Security Best Current Practice](https://www.rfc-editor.org/rfc/rfc9700) và [JWT Profile for OAuth 2.0 Access Tokens](https://www.rfc-editor.org/rfc/rfc9068).

## 5. Đặc tả API bằng Swagger/OpenAPI

Đặc tả FreshVend sử dụng OpenAPI 3.0.3. Xem [openapi.yaml](./openapi.yaml)
để tra cứu phương thức HTTP, đường dẫn, tham số, dữ liệu gửi lên,
dữ liệu trả về, mã trạng thái và quyền gọi của từng API.

Để xem bằng Swagger Editor, mở https://editor.swagger.io/ và dán nội dung
của `openapi.yaml` vào khung soạn thảo bên trái.

### 5.1. Đường dẫn và kiểu dữ liệu

Các API nghiệp vụ dùng đường dẫn gốc `/api/v1`. Ví dụ, thao tác
`GET /machines/{machineId}/catalog` với `machineId = 3` tương ứng với
`GET /api/v1/machines/3/catalog`.

| Dữ liệu | Kiểu trong OpenAPI | Ví dụ |
|---|---|---|
| ID của máy, sản phẩm, đơn hàng | `integer`, định dạng `int64` | `3` |
| Số lượng sản phẩm | `integer`, định dạng `int32` | `2` |
| Số tiền, tính bằng đồng Việt Nam | `integer`, định dạng `int64` | `30000` |
| Nhiệt độ và độ ẩm | `number` | `7.5` |
| Trạng thái cửa máy | `boolean` | `true` nghĩa là cửa mở |
| Thời điểm đo | `string`, định dạng `date-time` | `2026-11-07T10:00:00Z` |
| Mã sự kiện thiết bị | `string`, định dạng `uuid` | `123e4567-e89b-42d3-a456-426614174000` |

### 5.2. Xác thực và phân quyền

| Bên gọi | Cách khai báo trong OpenAPI |
|---|---|
| Nhân viên | `staffOAuth`: OAuth2 Authorization Code với PKCE S256. Gửi access token JWT trong header `Authorization: Bearer <token>`. |
| Kiosk và thiết bị | `machineOAuth`: OAuth2 Client Credentials. Token gắn với máy và loại bên gọi tương ứng. |
| Cổng thanh toán | `gatewaySignature`: gửi chữ ký trong header `X-Signature`. |

Mỗi thao tác ghi các vai trò được phép gọi trong phần mô tả và trường
`x-roles`, đối chiếu với bảng RBAC ở mục 4. Backend Spring Boot chịu trách
nhiệm kiểm tra quyền khi nhận yêu cầu.

Hai đường dẫn `/oauth2/authorize` và `/oauth2/token` thuộc dịch vụ xác thực.
Các đường dẫn này không có tiền tố `/api/v1`.

### 5.3. Cách tra cứu

Mở `openapi.yaml` bằng Swagger Editor. Chọn một thao tác để xem tham số
cần truyền, các header bắt buộc, request body và các response tương ứng.



