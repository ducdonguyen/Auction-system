# Hệ thống Đấu giá (Auction System)

## 1. Mô tả ngắn gọn bài toán và phạm vi hệ thống
Hệ thống Đấu giá là một ứng dụng Client-Server cho phép nhiều người dùng kết nối và tham gia vào các phiên đấu giá trực tuyến theo thời gian thực. 
Phạm vi hệ thống bao gồm:
- **Người dùng:** Đăng ký, đăng nhập, xem danh sách các phiên đấu giá, tạo phiên đấu giá mới và tham gia trả giá (bidding) trong phòng đấu giá.
- **Hệ thống (Server):** Quản lý kết nối đồng thời từ nhiều client, xử lý logic đấu giá, đảm bảo tính nhất quán dữ liệu khi có nhiều người cùng trả giá, và tự động hóa việc mở/đóng phiên đấu giá dựa trên thời gian thực.
- **Quản trị viên (Admin):** Quản lý hệ thống, các phiên đấu giá và người dùng.

## 2. Công nghệ sử dụng, môi trường chạy và yêu cầu cài đặt
### Công nghệ sử dụng:
- **Ngôn ngữ lập trình:** Java 21
- **Giao diện người dùng (Client):** JavaFX 21.0.2
- **Cơ sở dữ liệu:** MySQL 8.x (Hỗ trợ kết nối Cloud Database như Aiven)
- **Giao tiếp mạng:** TCP Sockets kết hợp Java Concurrency (ThreadPool, Lock)
- **Serialization/Giao tiếp Client-Server:** Protocol Buffers (Protobuf) của Google
- **Bảo mật:** BCrypt (Hash mật khẩu)
- **Quản lý dự án & Build tool:** Maven
- **Khác:** SLF4J & Logback (Logging), JUnit 5 & Mockito (Testing)

### Yêu cầu cài đặt:
- JDK 21 trở lên được cài đặt và cấu hình biến môi trường `JAVA_HOME`.
- Apache Maven (hoặc có thể dùng IDE có tích hợp sẵn Maven).
- Kết nối Internet (nếu sử dụng Cloud Database mặc định) hoặc MySQL Server cục bộ.

## 3. Cấu trúc thư mục và các module chính
Dự án được chia thành các module chính (bên trong `auctionSystem/src/main/java/com/auction`):
- **`server/`**: Chứa toàn bộ logic xử lý phía Server.
  - `concurrency/`: Xử lý đa luồng, quản lý locks cho đấu giá, xử lý các Client handler.
  - `core/`: Logic nghiệp vụ chính (AuctionService, AuctionScheduler).
  - `dao/` & `repository/`: Truy cập cơ sở dữ liệu.
  - `ServerApplication.java`: Điểm khởi chạy của Server.
- **`client/`**: Chứa ứng dụng dành cho người dùng cuối (giao diện JavaFX).
  - `controller/`: Các điều khiển UI (Login, AuctionRoom, Admi Dashboard...).
  - `view/`: Cấu hình giao diện, file FXML.
  - `network/`: Lớp kết nối tới Server.
  - `ClientLauncher.java`: Điểm khởi chạy của Client.
- **`shared/`**: Các lớp dùng chung giữa Client và Server (Model, DTOs, Utility, Protobuf message).

## 4. Hướng dẫn chạy chương trình
Các câu lệnh dưới đây có thể chạy trên mọi hệ điều hành (Windows, Linux, MacOS) thông qua cửa sổ dòng lệnh (Terminal/Command Prompt/PowerShell).
**Yêu cầu:** Mở terminal tại thư mục chứa file `pom.xml` (thư mục `auctionSystem`).

**Build dự án (Khuyến nghị chạy lần đầu):**
```bash
mvn clean install -DskipTests
```

## 5. Hướng dẫn chạy Server/Client theo thứ tự cụ thể
Để hệ thống hoạt động chính xác, **bắt buộc** phải khởi chạy Server trước để mở cổng mạng lắng nghe, sau đó mới khởi chạy Client.

### Bước 1: Khởi động Server
Chạy lệnh sau tại thư mục `auctionSystem`:
```bash
$OutputEncoding = [System.Text.Encoding]::UTF8; [Console]::OutputEncoding = [System.Text.Encoding]::UTF8
mvn exec:java '-Dexec.mainClass=com.auction.server.ServerApplication'
```
*Lưu ý: Chờ đến khi terminal hiện dòng log `Server đang lắng nghe kết nối tại cổng: 8080` và xác nhận kết nối Database thành công.*

### Bước 2: Khởi động Client
Mở một cửa sổ terminal **mới** (hoặc tab mới) tại thư mục `auctionSystem` và chạy lệnh:
```bash
mvn javafx:run
```
*(Bạn có thể chạy câu lệnh `mvn javafx:run` nhiều lần trên nhiều terminal khác nhau để giả lập nhiều người dùng cùng kết nối)*

## 6. Danh sách chức năng đã hoàn thành
- [x] Đăng ký / Đăng nhập tài khoản an toàn (mã hóa mật khẩu BCrypt).
- [x] Phân quyền Quản trị (Admin Dashboard).
- [x] Tạo mới một phiên đấu giá với các thông tin chi tiết (giá khởi điểm, bước giá, thời gian bắt đầu/kết thúc).
- [x] Tự động chuyển trạng thái phiên đấu giá (Chưa bắt đầu -> Đang diễn ra -> Kết thúc) theo thời gian thực nhờ Scheduler.
- [x] Xem danh sách các phiên đấu giá (đang diễn ra, sắp diễn ra...).
- [x] Tham gia phòng đấu giá thời gian thực (Real-time Auction Room).
- [x] Thực hiện trả giá (Bidding) theo thời gian thực (Đồng bộ đa luồng, chặn Race condition đảm bảo chỉ một người đặt giá tại một thời điểm).
