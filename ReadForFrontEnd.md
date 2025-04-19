# Backend API Documentation for Chat Anonymous App Frontend

## Overview

Tài liệu này mô tả cách frontend tương tác với backend của ứng dụng Chat Anonymous chạy trên `localhost:8080`. Ứng dụng sử dụng cả REST API endpoints và WebSocket để hỗ trợ chức năng chat thời gian thực.

## REST API Endpoints

### 1. Thống Kê Cơ Bản

**Endpoint:** `GET /api/stats`

**Mô tả:** Cung cấp thông tin thống kê cơ bản về số người dùng đang chờ và số cặp đang trò chuyện.

**Response:**
```json
{
  "status": "online", 
  "waitingUsers": 5,
  "activePairs": 10,
  "onlineCount": 25,
  "chattingCount": 20
}
```

### 2. Thống Kê Nâng Cao

**Endpoint:** `GET /api/stats/advanced`

**Mô tả:** Cung cấp thông tin thống kê chi tiết về hoạt động của hệ thống.

**Response:**
```json
{
  "waitingUsers": 5,
  "activePairs": 10,
  "totalConnections": 1245,
  "totalMessages": 3560,
  "totalPairings": 450,
  "peakConcurrentUsers": 100,
  "totalReconnections": 35,
  "startTime": "2023-04-17T09:30:00",
  "uptime": 120
}
```

### 3. Thống Kê Theo Giờ

**Endpoint:** `GET /api/stats/hourly?hours=24`

**Mô tả:** Cung cấp thông tin thống kê theo giờ, mặc định cho 24 giờ qua.

**Query Parameters:**
- `hours`: Số giờ muốn xem thống kê (mặc định: 24)

**Response:**
```json
{
  "2023-04-17 09": {
    "users": 25,
    "messages": 350
  },
  "2023-04-17 08": {
    "users": 20,
    "messages": 280
  },
  ...
}
```

### 4. Kiểm Tra Sức Khỏe Hệ Thống

**Endpoint:** `GET /api/health`

**Mô tả:** Kiểm tra trạng thái hoạt động của hệ thống.

**Response:**
```json
{
  "status": "UP",
  "service": "Chat Anonymous Service",
  "version": "1.0.0",
  "timestamp": 1681756456789
}
```

### 5. Thống Kê Phiên Làm Việc

**Endpoint:** `GET /api/sessions`

**Mô tả:** Cung cấp thông tin về các phiên làm việc hiện tại của người dùng (yêu cầu xác thực quản trị viên).

**Response:**
```json
{
  "sessions": [
    {
      "id": "session123",
      "connectionTime": "2023-04-17T09:30:00",
      "lastActivityTime": "2023-04-17T09:45:30",
      "nickname": "Anonymous123",
      "userAgent": "Mozilla/5.0...",
      "paired": true,
      "pairedWith": "session456"
    },
    ...
  ],
  "totalCount": 25
}
```

### 6. Xóa Phiên Làm Việc

**Endpoint:** `DELETE /api/sessions/{sessionId}`

**Mô tả:** Xóa một phiên làm việc cụ thể (yêu cầu xác thực quản trị viên).

**Response:**
```json
{
  "success": true,
  "message": "Session successfully terminated"
}
```

## WebSocket Connection

### Connection URL

Kết nối tới: `ws://localhost:8080/chat`

### Message Format

Tất cả các tin nhắn WebSocket sử dụng định dạng JSON sau:

```json
{
  "type": "message_type",
  "content": "message_content",
  "sender": "sender_identifier",
  "imageData": "base64_encoded_image_optional",
  "senderId": "session_id_optional",
  "receiver": "receiver_optional"
}
```

### Message Types

1. **Tin Nhắn Hệ Thống (Nhận từ Server)**
   - `type`: "status" - Cập nhật về trạng thái kết nối
   - `type`: "connected" - Đã kết nối thành công với một người lạ
   - `type`: "leave" - Người lạ đã rời đi

2. **Tin Nhắn Người Dùng (Gửi tới Server)**
   - `type`: "message" - Tin nhắn văn bản thông thường
   - `type`: "image" - Tin nhắn chứa hình ảnh (dạng base64)
   - `type`: "leave" - Người dùng muốn kết thúc cuộc trò chuyện hiện tại
   - `type`: "find" - Người dùng muốn tìm đối tác trò chuyện mới
   - `type`: "connection_status" - Kiểm tra trạng thái kết nối hiện tại

### WebSocket Workflow

1. **Thiết Lập Kết Nối**
   - Khi kết nối WebSocket được thiết lập, người dùng tự động được thêm vào hàng đợi chờ
   - Server gửi một thông báo trạng thái: `{"type":"status","content":"Đang chờ kết nối với người lạ...","sender":"system"}`

2. **Ghép Cặp với Người Lạ**
   - Khi được ghép cặp với người dùng khác, server gửi: `{"type":"connected","content":"Đã kết nối với người lạ","sender":"system","senderId":"partner_session_id"}`
   - Thuộc tính `senderId` chứa ID phiên của đối tác, hữu ích khi cần khôi phục kết nối

3. **Gửi Tin Nhắn**
   - Để gửi tin nhắn văn bản đến người lạ đã ghép cặp:
   ```json
   {
     "type": "message",
     "content": "Xin chào, bạn khỏe không?",
     "sender": "user"
   }
   ```

   - Để gửi tin nhắn hình ảnh:
   ```json
   {
     "type": "image",
     "content": "Mô tả hình ảnh (tùy chọn)",
     "imageData": "data:image/jpeg;base64,/9j/4AAQSkZJRgABAQEAYABgA...",
     "sender": "user"
   }
   ```

4. **Nhận Tin Nhắn**
   - Khi người lạ gửi tin nhắn, bạn sẽ nhận được:
   ```json
   {
     "type": "message",
     "content": "Tôi khỏe, cảm ơn bạn!",
     "sender": "stranger"
   }
   ```

5. **Tìm Người Mới**
   - Để ngắt kết nối với người lạ hiện tại và tìm người mới:
   ```json
   {
     "type": "find",
     "content": "",
     "sender": "user"
   }
   ```

6. **Rời Khỏi Cuộc Trò Chuyện**
   - Để ngắt kết nối hoàn toàn:
   ```json
   {
     "type": "leave",
     "content": "",
     "sender": "user"
   }
   ```

7. **Kiểm Tra Trạng Thái Kết Nối**
   - Để kiểm tra trạng thái kết nối hiện tại (hữu ích khi tải lại trang):
   ```json
   {
     "type": "connection_status",
     "content": "",
     "sender": "user"
   }
   ```

## Tính Năng Mới Cho Giao Diện Frontend

### Hiển Thị Thống Kê Chi Tiết

Trang thống kê (/stats) hiện có thể hiển thị:

1. **Thống Kê Tổng Quan**
   - Tổng số kết nối
   - Số kết nối hiện tại
   - Tổng số tin nhắn
   - Tổng số cặp đã tạo

2. **Hoạt Động Theo Giờ**
   - Biểu đồ hiển thị người dùng và tin nhắn theo từng giờ
   - Dữ liệu được cập nhật theo thời gian thực

3. **Sức Khỏe Hệ Thống**
   - Thời gian hoạt động của máy chủ
   - Phiên bản phần mềm
   - Trạng thái hệ thống

### Đảm Bảo Tương Thích Với Bảo Mật

Khi tích hợp các tính năng quản trị, lưu ý:

1. **Xác Thực CSRF**
   - Tất cả các yêu cầu POST, PUT và DELETE cần gửi kèm CSRF token
   - Token có thể lấy từ cookie XSRF-TOKEN
   - Thêm header 'X-XSRF-TOKEN' với token vào yêu cầu

2. **Xác Thực Người Dùng**
   - Các endpoint quản trị yêu cầu xác thực
   - Sử dụng phương thức xác thực cơ bản HTTP (HTTP Basic Authentication)

```javascript
// Ví dụ xác thực CSRF và admin cho yêu cầu API
async function deleteSession(sessionId) {
  // Lấy CSRF token từ cookie
  const csrfToken = getCookie('XSRF-TOKEN');
  
  const response = await fetch(`/api/sessions/${sessionId}`, {
    method: 'DELETE',
    headers: {
      'X-XSRF-TOKEN': csrfToken,
      'Authorization': 'Basic ' + btoa('admin:password') // Thay thế bằng thông tin xác thực thực tế
    }
  });
  
  return await response.json();
}

function getCookie(name) {
  const value = `; ${document.cookie}`;
  const parts = value.split(`; ${name}=`);
  if (parts.length === 2) return parts.pop().split(';').shift();
}
```

### Cập Nhật Liên Tục

Để đảm bảo hiển thị dữ liệu cập nhật nhất, nên cài đặt:

1. **Polling Định Kỳ**
   ```javascript
   // Cập nhật thống kê mỗi 30 giây
   setInterval(() => {
     this.fetchStats();
   }, 30000);
   ```

2. **Cập Nhật Khi Tab Được Kích Hoạt**
   ```javascript
   document.addEventListener('visibilitychange', () => {
     if (!document.hidden) {
       this.fetchStats();
     }
   });
   ```

## Xử Lý Reload Trang và Trạng Thái Kết Nối

### Xử Lý Reload Trang

Khi người dùng reload trang, kết nối WebSocket sẽ được thiết lập lại. Sử dụng cơ chế sau để duy trì trạng thái kết nối:

1. **Kiểm Tra Trạng Thái Kết Nối Khi Tải Trang**
   ```javascript
   function checkConnectionStatus() {
     if (socket && socket.readyState === WebSocket.OPEN) {
       socket.send(JSON.stringify({
         type: 'connection_status',
         content: '',
         sender: 'user'
       }));
     }
   }
   
   // Gọi sau khi kết nối websocket thành công
   socket.onopen = function() {
     // ... code khác ...
     checkConnectionStatus();
   };
   ```

2. **Theo Dõi Sự Kiện Visibility Change**
   ```javascript
   document.addEventListener('visibilitychange', function() {
     if (!document.hidden && socket && socket.readyState === WebSocket.OPEN) {
       checkConnectionStatus();
     }
   });
   ```

3. **Xử Lý Khôi Phục Kết Nối**
   ```javascript
   socket.onmessage = function(event) {
     const message = JSON.parse(event.data);
     
     if (message.type === 'connected') {
       // Cập nhật UI - đã kết nối với người lạ
       showChatInterface();
       hideWaitingMessage();
     } else if (message.type === 'status' && 
                message.content.includes('Đang chờ kết nối')) {
       // Hiển thị giao diện chờ kết nối
       showWaitingInterface();
       hideChatInterface();
     }
   };
   ```

### Xử Lý Mất Kết Nối

```javascript
// Cài đặt tự động kết nối lại
let reconnectAttempts = 0;
const MAX_RECONNECT_ATTEMPTS = 5;

socket.onclose = function(event) {
  console.log('WebSocket connection closed');
  
  if (reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
    reconnectAttempts++;
    showReconnectingMessage();
    setTimeout(connect, 2000 * reconnectAttempts); // Tăng dần thời gian chờ
  } else {
    showConnectionErrorMessage();
  }
};

// Reset số lần thử kết nối lại khi kết nối thành công
socket.onopen = function() {
  reconnectAttempts = 0;
  // ... code khác ...
};
```

## Gợi Ý Kiến Trúc Frontend

1. **Quản Lý Trạng Thái**
   - Sử dụng Vuex/Pinia (cho Vue) hoặc Redux/Context API (cho React) để quản lý trạng thái kết nối và tin nhắn
   - Lưu trữ lịch sử tin nhắn cho cuộc trò chuyện hiện tại

2. **Các Component Chính**
   - `ChatContainer`: Component chính quản lý kết nối WebSocket
   - `MessageList`: Hiển thị các tin nhắn chat
   - `MessageInput`: Ô nhập và gửi tin nhắn
   - `StatusBar`: Hiển thị trạng thái kết nối và thống kê người dùng
   - `ImageUploader`: Xử lý tải lên hình ảnh

3. **Xử Lý Người Dùng Không Hoạt Động**
   - Gửi tin "ping" định kỳ để duy trì kết nối
   - Hiển thị thông báo đối tác không hoạt động sau một khoảng thời gian không có tương tác

4. **Tối Ưu Giao Diện**
   - Hiển thị rõ ràng trạng thái đang chờ/đã kết nối
   - Hiệu ứng "đang gõ" khi đối tác đang nhập tin nhắn
   - Âm thanh thông báo khi có tin nhắn mới

## Ví Dụ Code Kết Nối WebSocket Đầy Đủ

```javascript
// Ví dụ trong component Vue.js
export default {
  data() {
    return {
      socket: null,
      messages: [],
      connectionStatus: 'disconnected',
      reconnectAttempts: 0,
      partnerId: null,
      inputMessage: '',
      isConnecting: false
    }
  },
  methods: {
    connect() {
      this.isConnecting = true;
      
      // Sử dụng URL tương đối thay vì hardcode
      const wsProtocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
      const wsUrl = `${wsProtocol}//${window.location.host}/chat`;
      
      this.socket = new WebSocket(wsUrl);
      
      this.socket.onopen = () => {
        console.log('WebSocket connected');
        this.isConnecting = false;
        this.reconnectAttempts = 0;
        this.connectionStatus = 'waiting';
        
        // Kiểm tra trạng thái kết nối hiện tại
        this.checkConnectionStatus();
      };
      
      this.socket.onmessage = (event) => {
        const message = JSON.parse(event.data);
        console.log('Received message:', message);
        
        // Xử lý các loại tin nhắn khác nhau
        if (message.type === 'connected') {
          this.connectionStatus = 'connected';
          this.partnerId = message.senderId;
          // Xóa tin nhắn trước đó nếu là kết nối mới
          if (!this.reconnecting) {
            this.messages = [];
          }
        } else if (message.type === 'status') {
          if (message.content.includes('Đang chờ')) {
            this.connectionStatus = 'waiting';
            this.partnerId = null;
          }
        } else if (message.type === 'leave') {
          this.connectionStatus = 'waiting';
          this.partnerId = null;
        }
        
        // Thêm tin nhắn vào danh sách
        this.messages.push(message);
      };
      
      this.socket.onclose = (event) => {
        console.log('WebSocket closed', event);
        this.connectionStatus = 'disconnected';
        
        // Xử lý kết nối lại
        if (this.reconnectAttempts < 5) {
          this.reconnecting = true;
          this.reconnectAttempts++;
          setTimeout(() => this.connect(), 2000 * this.reconnectAttempts);
        }
      };
      
      this.socket.onerror = (error) => {
        console.error('WebSocket error:', error);
      };
    },
    
    // Kiểm tra trạng thái kết nối
    checkConnectionStatus() {
      if (this.socket && this.socket.readyState === WebSocket.OPEN) {
        this.socket.send(JSON.stringify({
          type: 'connection_status',
          content: '',
          sender: 'user'
        }));
      }
    },
    
    // Gửi tin nhắn văn bản
    sendMessage() {
      if (!this.inputMessage.trim()) return;
      
      if (this.socket && this.socket.readyState === WebSocket.OPEN) {
        const message = {
          type: 'message',
          content: this.inputMessage,
          sender: 'user'
        };
        this.socket.send(JSON.stringify(message));
        this.messages.push(message);
        this.inputMessage = '';
      }
    },
    
    // Gửi hình ảnh
    sendImage(base64Data) {
      if (this.socket && this.socket.readyState === WebSocket.OPEN) {
        const message = {
          type: 'image',
          content: 'Đã gửi một hình ảnh',
          imageData: base64Data,
          sender: 'user'
        };
        this.socket.send(JSON.stringify(message));
        this.messages.push(message);
      }
    },
    
    // Tìm người mới
    findNewPartner() {
      if (this.socket && this.socket.readyState === WebSocket.OPEN) {
        this.socket.send(JSON.stringify({
          type: 'find',
          content: '',
          sender: 'user'
        }));
        this.messages = [];
        this.connectionStatus = 'waiting';
        this.partnerId = null;
      }
    },
    
    // Rời khỏi cuộc trò chuyện
    leaveChat() {
      if (this.socket && this.socket.readyState === WebSocket.OPEN) {
        this.socket.send(JSON.stringify({
          type: 'leave',
          content: '',
          sender: 'user'
        }));
        this.connectionStatus = 'disconnected';
        this.partnerId = null;
      }
    }
  },
  mounted() {
    this.connect();
    
    // Theo dõi visibility change
    document.addEventListener('visibilitychange', () => {
      if (!document.hidden && this.connectionStatus === 'connected') {
        this.checkConnectionStatus();
      }
    });
  },
  beforeUnmount() {
    // Đóng kết nối khi component bị hủy
    if (this.socket) {
      this.socket.close();
    }
  }
}
``` 