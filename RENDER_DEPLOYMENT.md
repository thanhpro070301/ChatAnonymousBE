# Hướng dẫn triển khai ChatAnonymousApp lên Render

## Các bước triển khai

1. **Đăng ký tài khoản Render**
   - Truy cập [render.com](https://render.com) và đăng ký tài khoản mới
   - Kết nối tài khoản GitHub của bạn với Render

2. **Chuẩn bị repository**
   - Đảm bảo rằng các file sau đã được thêm vào repository:
     - `Dockerfile`
     - `render.yaml`
     - `src/main/resources/application-prod.properties`

3. **Tạo mới Web Service trên Render**
   - Chọn "New +" và chọn "Web Service"
   - Kết nối repository GitHub của bạn
   - Render sẽ tự động phát hiện file `render.yaml` và sử dụng cấu hình từ file đó
   - Nếu không tự động, hãy chọn:
     - Environment: Docker
     - Branch: main (hoặc branch bạn muốn triển khai)
     - Plan: Free (hoặc plan phù hợp với nhu cầu của bạn)

4. **Cấu hình**
   - Tên dịch vụ: chat-anonymous-app (hoặc tên khác tùy chọn)
   - Region: Chọn region gần với người dùng của bạn nhất
   - Environment Variables:
     - KEY: `PORT`, VALUE: `8080`
     - KEY: `SPRING_PROFILES_ACTIVE`, VALUE: `prod`

5. **Triển khai**
   - Nhấn "Create Web Service"
   - Render sẽ tự động build và triển khai ứng dụng của bạn

## Theo dõi và quản lý

- Sau khi triển khai, bạn có thể theo dõi logs từ dashboard của Render
- Để cập nhật ứng dụng, chỉ cần push các thay đổi lên repository và Render sẽ tự động build lại

## URL của ứng dụng

Sau khi triển khai thành công, ứng dụng của bạn sẽ có URL dạng:
- `https://chat-anonymous-app.onrender.com`

## Xử lý sự cố

Nếu gặp vấn đề khi triển khai:
1. Kiểm tra logs từ dashboard của Render
2. Đảm bảo file Dockerfile đã được cấu hình đúng
3. Kiểm tra file application-prod.properties
4. Đảm bảo ứng dụng chạy đúng trên local trước khi triển khai 