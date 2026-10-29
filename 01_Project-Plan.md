PHÂN CÔNG CÔNG VIỆC PROJECT FRESHVEND

Tên đề tài: FreshVend – Máy bán trái cây tự động thông minh tích hợp IoT, quản lý tồn kho thời gian thực và chuỗi lạnh.

I. Mục tiêu dự án

Xây dựng hệ thống máy bán trái cây tự động có khả năng hiển thị sản phẩm, đặt hàng, thanh toán, điều khiển xuất hàng, theo dõi nhiệt độ bảo quản và quản lý tồn kho từ xa. Hệ thống gồm ứng dụng khách hàng, backend Java Web, cơ sở dữ liệu, mô-đun IoT và giao diện quản trị, vận hành.

Lập trình java (1).docx
Nhóm cần hướng đến các sản phẩm chính theo đề bài:

• Centralized Management API – API quản lý tập trung.
• Vending Machine Client Application – ứng dụng giao diện khách hàng.
• Automated Hardware Controller – mô-đun điều khiển phần cứng ESP32.
• Operator Mobile App – ứng dụng cho nhân viên vận hành.
• Admin Business Dashboard – giao diện quản trị và báo cáo kinh doanh.
Các yêu cầu kỹ thuật trong đề còn bao gồm PostgreSQL, REST API, JWT/OAuth2, RBAC, ESP32, DHT22, servo, cập nhật trạng thái IoT qua WebSocket, kiểm thử, Docker và tài liệu thiết kế.

Lập trình java (1).docx
II. Phân công công việc cho 5 thành viên

Thành viên 1 – Nhóm trưởng: Quản lý dự án, phân tích yêu cầu và tài liệu

Vai trò: Quản lý tiến độ, điều phối công việc, tổng hợp tài liệu và kiểm soát chất lượng. Không phụ trách lập trình chính.

Công việc cụ thể:

1. Lập Project Plan, chia công việc theo từng giai đoạn và thời hạn.
2. Theo dõi tiến độ của 4 thành viên, ghi nhận công việc đã hoàn thành và nhắc các mốc bàn giao.
3. Tổng hợp yêu cầu người dùng (URS) và đặc tả yêu cầu phần mềm (SRS) dựa trên đề tài.
4. Điều phối việc xây dựng sơ đồ UML 2.0, sơ đồ kiến trúc hệ thống, Component Diagram, Deployment Diagram và ERD; các thành viên kỹ thuật cung cấp thông tin chuyên môn.
5. Theo dõi repository Git, kiểm tra các thành viên đã commit mã nguồn và tài liệu theo tiến độ.
6. Tổng hợp báo cáo triển khai, kết quả kiểm thử, hướng dẫn cài đặt, hướng dẫn sử dụng và slide thuyết trình.
7. Kiểm tra tính thống nhất giữa báo cáo, mã nguồn, sơ đồ và sản phẩm thực tế.
8. Chuẩn bị nội dung giới thiệu kiến trúc tổng thể và phần quản lý dự án khi bảo vệ.
Sản phẩm bàn giao:

• Project Plan và bảng phân công công việc.
• Tài liệu URS/SRS.
• Báo cáo tổng kết dự án và slide thuyết trình.
• Tài liệu hướng dẫn cài đặt, sử dụng được tổng hợp từ các thành viên.
• Bảng theo dõi tiến độ, danh sách bàn giao và kiểm tra lịch sử Git.
Phần Git: Tạo hoặc phối hợp thiết lập repository; commit Project Plan, tài liệu yêu cầu, báo cáo và slide theo từng giai đoạn. Không cần nhận phần code chính nhưng phải có đóng góp thực tế được ghi nhận trong Git.

Thành viên 2 – Backend Java Web và cơ sở dữ liệu

Vai trò: Phụ trách phần xử lý nghiệp vụ và dữ liệu trung tâm.

Công việc cụ thể:

1. Xây dựng backend bằng Java theo công nghệ Java Web được môn học yêu cầu.
2. Thiết kế cơ sở dữ liệu PostgreSQL cho người dùng, vai trò, sản phẩm, tồn kho, giao dịch, dữ liệu nhiệt độ và lịch sử nhập hàng.
3. Xây dựng REST API để các giao diện khách hàng, quản trị và mô-đun IoT trao đổi dữ liệu.
4. Triển khai chức năng đăng nhập, xác thực và phân quyền theo vai trò; áp dụng JWT/OAuth2 và RBAC theo phạm vi nhóm có thể thực hiện.
5. Xây dựng nghiệp vụ quản lý sản phẩm, giá bán, số lượng tồn, giao dịch và lịch sử giao dịch.
6. Xây dựng API tiếp nhận dữ liệu nhiệt độ, trạng thái máy và lỗi xuất hàng từ mô-đun IoT.
7. Phối hợp cập nhật trạng thái lên dashboard; nghiên cứu WebSocket để đáp ứng yêu cầu cập nhật dữ liệu gần thời gian thực.
8. Viết tài liệu API, cấu trúc database và hướng dẫn chạy backend.
Sản phẩm bàn giao:

• Mã nguồn backend Java Web.
• Database PostgreSQL và file tạo cấu trúc dữ liệu.
• REST API cho sản phẩm, tồn kho, người dùng, giao dịch và dữ liệu IoT.
• Chức năng xác thực và phân quyền.
• Tài liệu API, sơ đồ cơ sở dữ liệu và kết quả kiểm thử backend.
Phần Git: Commit mã nguồn backend, script database, tài liệu API và các bản sửa lỗi theo từng giai đoạn.

Thành viên 3 – IoT, firmware và điều khiển phần cứng

Vai trò: Phụ trách kết nối giữa phần mềm với thiết bị máy bán hàng.

Công việc cụ thể:

1. Tìm hiểu và xây dựng firmware cho ESP32 theo yêu cầu đề tài.
2. Tích hợp cảm biến DHT22 để thu thập dữ liệu nhiệt độ.
3. Xây dựng chức năng điều khiển servo phục vụ cơ chế xuất từng khay trái cây.
4. Gửi dữ liệu nhiệt độ và trạng thái thiết bị về backend.
5. Xử lý trạng thái xuất hàng thành công, lỗi kẹt hàng và cơ chế thử lại trong phạm vi thiết bị hỗ trợ.
6. Xây dựng cơ chế cảnh báo khi nhiệt độ vượt ngưỡng do nhóm cấu hình.
7. Phối hợp với thành viên backend để thống nhất định dạng dữ liệu và phương thức truyền nhận.
8. Kiểm thử cảm biến, servo, dữ liệu truyền về máy chủ và ghi nhận kết quả.
Sản phẩm bàn giao:

• Mã nguồn firmware ESP32.
• Mô-đun đọc nhiệt độ DHT22.
• Mô-đun điều khiển servo xuất hàng.
• Chức năng gửi trạng thái và dữ liệu IoT.
• Tài liệu đấu nối, cấu hình và kết quả kiểm thử phần cứng.
Phần Git: Commit firmware, sơ đồ đấu nối, hướng dẫn cấu hình và kết quả kiểm thử.

Lưu ý: Nếu nhóm không có đủ phần cứng, cần xin giảng viên chấp thuận phương án mô phỏng trước khi thay thế thiết bị thật.

Thành viên 4 – Giao diện khách hàng và chức năng đặt hàng, thanh toán

Vai trò: Phụ trách ứng dụng mà khách hàng sử dụng để mua trái cây.

Công việc cụ thể:

1. Thiết kế giao diện màn hình cảm ứng hiển thị hình ảnh, tên, giá và số lượng còn lại của sản phẩm.
2. Xây dựng chức năng xem danh sách sản phẩm và chọn một hoặc nhiều khay trái cây.
3. Xây dựng giỏ hàng, tính tổng tiền và xác nhận đơn hàng.
4. Tích hợp quy trình thanh toán QR/e-wallet theo khả năng của nhóm; nếu chưa có tài khoản cổng thanh toán thật, sử dụng môi trường thử nghiệm hoặc mô phỏng có ghi rõ.
5. Kết nối giao diện với REST API để lấy sản phẩm, giá và tình trạng tồn kho.
6. Hiển thị kết quả thanh toán, trạng thái xuất hàng và thông báo nếu xảy ra lỗi.
7. Thiết kế giao diện dễ sử dụng, hướng đến mục tiêu hoàn thành thao tác mua trong tối đa 5 lần tương tác theo đề.
8. Kiểm thử luồng mua hàng từ chọn sản phẩm đến nhận kết quả giao dịch.
Sản phẩm bàn giao:

• Mã nguồn giao diện khách hàng.
• Chức năng danh sách sản phẩm và giỏ hàng.
• Luồng thanh toán QR/e-wallet hoặc bản thử nghiệm được thống nhất.
• Chức năng thông báo kết quả giao dịch và lỗi xuất hàng.
• Bản phác thảo giao diện và kết quả kiểm thử.
Phần Git: Commit giao diện, mã tích hợp API, tài liệu sử dụng và các bản sửa lỗi.

Thành viên 5 – Dashboard Admin/Operator, quản lý vận hành và hỗ trợ tích hợp

Vai trò: Phụ trách giao diện quản lý máy bán hàng và hỗ trợ tích hợp hệ thống.

Công việc cụ thể:

1. Xây dựng Java Web Dashboard hoặc triển khai giao diện quản trị kết nối với backend Java Web.
2. Xây dựng chức năng quản lý danh sách máy và trạng thái hoạt động.
3. Hiển thị tồn kho theo từng loại sản phẩm hoặc cột chứa, cảnh báo sắp hết hàng và sắp hết hạn.
4. Xây dựng chức năng ghi nhận nhập hàng, cập nhật giá, điều chỉnh vị trí sản phẩm và xử lý trạng thái lỗi theo quyền được cấp.
5. Xây dựng giao diện theo dõi nhiệt độ, trạng thái cửa và lỗi xuất hàng.
6. Xây dựng dashboard thống kê doanh thu theo sản phẩm, địa điểm và khung giờ; hỗ trợ xuất báo cáo PDF/CSV nếu kịp tiến độ.
7. Kết nối dashboard với API của thành viên 2 và phối hợp thành viên 3 để kiểm tra dữ liệu IoT.
8. Phối hợp tích hợp, kiểm thử toàn hệ thống và chuẩn bị cấu hình Docker/triển khai trong phạm vi nhóm thực hiện được.
9. Phụ trách hoặc phối hợp xây dựng Operator Mobile App theo yêu cầu đề tài.
Sản phẩm bàn giao:

• Mã nguồn dashboard Admin/Operator.
• Chức năng quản lý sản phẩm, tồn kho và nhập hàng.
• Giao diện theo dõi trạng thái máy và nhiệt độ.
• Dashboard thống kê doanh thu.
• Chức năng xuất báo cáo nếu triển khai được.
• Tài liệu chạy dashboard và kết quả kiểm thử tích hợp.
Phần Git: Commit mã nguồn dashboard, tài liệu giao diện, cấu hình triển khai và các bản sửa lỗi tích hợp.

Lưu ý: Dashboard web responsive không tự động tương đương với một ứng dụng di động riêng. Nhóm cần thống nhất với giảng viên nếu định dùng web responsive thay cho Operator Mobile App.

III. Phân công các tài liệu và đầu việc chung

Một số hạng mục không thể do một người tự làm hoàn toàn. Nhóm phân công người tổng hợp và người cung cấp nội dung như sau:

Hạng mục

Người phụ trách chính

Người phối hợp

Project Plan, lịch trình và phân công

Thành viên 1

Cả nhóm

URS/SRS

Thành viên 1

Thành viên 2, 3, 4, 5

Sơ đồ kiến trúc tổng thể

Thành viên 1 điều phối

Thành viên 2, 3, 5

Use Case Diagram, Activity Diagram

Thành viên 1 điều phối

Cả nhóm

Component Diagram

Thành viên 2

Thành viên 3, 4, 5

Deployment Diagram

Thành viên 5

Thành viên 2, 3

ERD và thiết kế database

Thành viên 2

Thành viên 1

UI Wireframes

Thành viên 4 và 5

Thành viên 1

API Specification, Swagger/OpenAPI

Thành viên 2

Thành viên 4, 5

Kiểm thử đơn vị (Unit Test)

Từng thành viên

Thành viên 2 hỗ trợ

Kiểm thử tích hợp (Integration Test)

Thành viên 5 điều phối

Thành viên 2, 3, 4

Kiểm thử hiệu năng (Performance Test)

Thành viên 2

Thành viên 5

Báo cáo kiểm thử

Thành viên 1 tổng hợp

Cả nhóm cung cấp kết quả

Hướng dẫn cài đặt

Thành viên 5 tổng hợp cấu hình

Thành viên 2, 3, 4

Hướng dẫn khách hàng

Thành viên 4

Thành viên 1

Hướng dẫn Operator/Admin

Thành viên 5

Thành viên 1

Báo cáo cuối kỳ và slide

Thành viên 1

Cả nhóm

Đây là cách phân công dựa trên các tài liệu bàn giao mà đề gốc yêu cầu, gồm URS/SRS, tài liệu kiến trúc, thiết kế chi tiết, báo cáo triển khai, kế hoạch và kết quả kiểm thử, hướng dẫn cài đặt và sử dụng.

Lập trình java (1).docx
IV. Project Plan theo từng tuần

Kế hoạch dưới đây dự kiến trong 7 tuần; nhóm có thể điều chỉnh theo hạn nộp của giảng viên.

Tuần

Công việc thực hiện

Kết quả cần đạt

Tuần 1

Đọc đề, xác định phạm vi, phân công thành viên, lập Project Plan, tạo repository Git.

Có kế hoạch, danh sách nhiệm vụ, repository và tài liệu yêu cầu ban đầu.

Tuần 2

Phân tích yêu cầu, thiết kế database, API, sơ đồ UML, kiến trúc và giao diện mẫu.

Hoàn thành bản thiết kế để các thành viên bắt đầu lập trình.

Tuần 3

Thành viên 2 phát triển backend; thành viên 3 làm firmware; thành viên 4 làm giao diện khách hàng; thành viên 5 làm dashboard; thành viên 1 cập nhật tài liệu.

Có phiên bản đầu tiên của từng module.

Tuần 4

Tiếp tục phát triển chức năng, kết nối database, API, giao diện và dữ liệu IoT.

Các module có chức năng chính và bắt đầu trao đổi dữ liệu.

Tuần 5

Tích hợp hệ thống, xử lý lỗi, hoàn thiện chức năng quản lý và giao dịch.

Có phiên bản hệ thống tích hợp để kiểm thử.

Tuần 6

Kiểm thử đơn vị, tích hợp và hiệu năng; sửa lỗi; thử triển khai Docker nếu khả thi.

Có kết quả kiểm thử, danh sách lỗi và bản triển khai thử nghiệm.

Tuần 7

Hoàn thiện báo cáo, hướng dẫn sử dụng, kiểm tra Git, chuẩn bị slide và luyện bảo vệ.

Hoàn thành mã nguồn, tài liệu và bài thuyết trình.

Nguyên tắc: Không chờ đến tuần cuối mới làm báo cáo hoặc mới đưa code lên Git. Tài liệu và mã nguồn cần được cập nhật xuyên suốt quá trình.

V. Quy định làm việc với Git

Đây là phần bắt buộc theo yêu cầu của giảng viên, nên nhóm cần thống nhất ngay từ tuần đầu.

1. Tạo một repository chung cho dự án FreshVend.
2. Mỗi thành viên sử dụng tài khoản Git của mình và commit phần công việc thực tế.
3. Chia nhánh theo chức năng, ví dụ feature/backend-api, feature/iot, feature/customer-ui, feature/admin-dashboard, docs/project-plan.
4. Ghi nội dung commit rõ ràng, ví dụ:
• docs: add project plan
• feat: implement product API
• feat: add temperature monitoring
• feat: create customer cart
• test: add inventory API tests
5. Cập nhật README hướng dẫn cấu hình, chạy chương trình và cấu trúc thư mục.
6. Kiểm tra code trước khi gộp vào nhánh chính; tránh ghi đè code của thành viên khác.
7. Nhóm trưởng theo dõi lịch sử commit và tiến độ, nhưng không tạo commit thay cho thành viên để làm giả lịch sử.
Lịch sử Git cần phản ánh quá trình làm việc thật: phân tích, thiết kế, lập trình, kiểm thử, sửa lỗi và hoàn thiện.

VI. Tiêu chí nghiệm thu công việc

Thành viên

Điều kiện hoàn thành

Thành viên 1

Có Project Plan, tài liệu yêu cầu, báo cáo và slide; theo dõi tiến độ, Git và tổng hợp đầy đủ tài liệu từ các thành viên.

Thành viên 2

Backend Java Web chạy được; có database, API và chức năng xác thực/phân quyền theo phạm vi triển khai.

Thành viên 3

Có firmware hoặc phương án mô phỏng đã được giảng viên chấp thuận; thể hiện được việc đọc nhiệt độ, điều khiển xuất hàng và gửi trạng thái.

Thành viên 4

Khách hàng có thể xem sản phẩm, chọn hàng, thao tác giỏ hàng và đi qua luồng thanh toán thử nghiệm hoặc tích hợp.

Thành viên 5

Dashboard có các chức năng quản lý và giám sát chính; kết nối được với backend và hỗ trợ kiểm thử tích hợp.

Cả nhóm cùng chịu trách nhiệm về tính thống nhất của hệ thống, tài liệu, chất lượng mã nguồn và khả năng trình bày khi bảo vệ.

VII. Các điểm cần lưu ý trước khi chốt phân công

• Java Web Application là bắt buộc: Thành viên 2 nên phụ trách backend Java Web; phần dashboard của thành viên 5 cần thống nhất công nghệ để phù hợp yêu cầu môn học.
• Không được bỏ IoT: Đề tài nêu ESP32, DHT22 và servo. Nếu không có thiết bị, cần hỏi giảng viên về phương án mô phỏng.
• Thanh toán: Không nên tuyên bố đã tích hợp cổng thanh toán thật nếu nhóm chỉ làm giao diện hoặc mô phỏng.
• Operator Mobile App: Cần làm rõ với giảng viên nếu nhóm muốn thay bằng dashboard web responsive.
• Bảo mật và hiệu năng: Các mục như TLS 1.2+, AES-256, API phản hồi trong 500 ms đối với 95% yêu cầu, dữ liệu IoT cập nhật trong 2 giây và Docker là các mục tiêu trong đề. Nhóm cần ghi rõ mục nào đã triển khai, mục nào đã kiểm thử và mục nào chưa thực hiện được; không tự nhận đạt nếu chưa có bằng chứng.
