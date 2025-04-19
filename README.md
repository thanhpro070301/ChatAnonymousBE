# Ứng Dụng Chat Với Người Lạ

Ứng dụng Spring Boot cho phép người dùng chat ngẫu nhiên với người lạ, tương tự như Omegle.

## Tính Năng Chính

- Kết nối người dùng ngẫu nhiên thành cặp chat
- Giao tiếp theo thời gian thực qua WebSocket
- Thông báo khi người dùng rời phòng chat
- Khả năng tìm người chat mới
- Hỗ trợ gửi hình ảnh trong chat
- Giao diện người dùng sử dụng Thymeleaf với layout hiện đại

## Công Nghệ Sử Dụng

- **Backend**: Spring Boot, WebSocket, JPA
- **Frontend**: Thymeleaf, Bootstrap 5, Bootstrap Icons
- **Database**: MySQL (tùy chọn)
- **Layout**: Thymeleaf Layout Dialect

## Cấu Trúc Dự Án

```
├── src/main/java/thanhpro0703/ChatAnonymousApp/
│   ├── config/                  # Cấu hình ứng dụng
│   │   └── WebSocketConfig.java # Cấu hình WebSocket
│   ├── controller/              # Controllers
│   │   ├── ChatController.java  # API endpoints
│   │   ├── HomeController.java  # Trang chủ 
│   │   └── AboutController.java # Trang giới thiệu
│   ├── model/                   # Domain models
│   │   ├── Message.java         # Mô hình tin nhắn
│   │   └── UserSession.java     # Mô hình phiên người dùng
│   ├── service/                 # Business logic
│   │   └── RoomManager.java     # Quản lý phòng chat
│   └── websocket/               # WebSocket handling
│       └── ChatWebSocketHandler.java  # Xử lý kết nối WebSocket
└── src/main/resources/
    ├── static/                  # Tài nguyên tĩnh
    ├── templates/               # Thymeleaf templates
    │   ├── fragments/           # Các phần tái sử dụng
    │   │   └── header.html      # Fragment header 
    │   ├── layout/              # Layout templates
    │   │   └── main.html        # Layout chính
    │   ├── index.html           # Trang chủ
    │   └── about.html           # Trang giới thiệu
    └── application.properties   # Cấu hình ứng dụng
```

## Yêu Cầu

- Java 11 hoặc cao hơn
- Maven
- MySQL (tuỳ chọn, có thể thay đổi thành H2 hoặc database khác)

## Cài Đặt & Chạy

1. Clone dự án
2. Cấu hình database trong `application.properties` (tuỳ chọn)
3. Chạy ứng dụng:

```bash
./mvnw spring-boot:run
```

4. Truy cập vào ứng dụng qua trình duyệt:
```
http://localhost:8080/
```

## WebSocket API

### Kết Nối
```
ws://localhost:8080/ws/chat
```

### Định Dạng Tin Nhắn

```json
{
  "type": "message|leave|find|image",
  "content": "Nội dung tin nhắn",
  "sender": "user",
  "imageData": "Base64 encoded image (chỉ dành cho tin nhắn hình ảnh)"
}
```

## Luồng Xử Lý

1. Người dùng kết nối tới WebSocket server
2. Server đưa người dùng vào hàng đợi chờ
3. Khi có đủ 2 người, server ghép cặp họ và tạo phòng chat
4. Tin nhắn được gửi qua WebSocket và chuyển tiếp tới người nhận
5. Khi người dùng rời đi, đối tác chat được thông báo
6. Người dùng có thể tìm người chat mới bằng cách gửi tin nhắn "find"

## Phát Triển

1. Thêm xác thực người dùng
2. Thêm tính năng lọc người dùng theo tiêu chí
3. Hỗ trợ gửi hình ảnh trong chat
4. Thêm tính năng báo cáo người dùng
5. Thêm hỗ trợ gửi file đính kèm
6. Tối ưu hóa kích thước hình ảnh khi gửi 