package thanhpro0703.ChatAnonymousApp.model;

import lombok.Data;
import org.springframework.web.socket.WebSocketSession;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class UserSession {
    private String sessionId;
    private WebSocketSession socket;
    private LocalDateTime connectedTime;
    private LocalDateTime lastActiveTime;
    private String nickname;
    private String clientIp;
    private String userAgent;
    private boolean isActive;
    private String roomId;
    
    public UserSession(String sessionId, WebSocketSession session) {
        this.sessionId = sessionId;
        this.socket = session;
        this.connectedTime = LocalDateTime.now();
        this.lastActiveTime = LocalDateTime.now();
        this.isActive = true;
        
        // Tạo nickname ngẫu nhiên
        this.nickname = "User" + UUID.randomUUID().toString().substring(0, 5);
        
        // Lấy thông tin từ session nếu có
        if (session.getAttributes().containsKey("remoteAddress")) {
            this.clientIp = session.getAttributes().get("remoteAddress").toString();
        }
        
        if (session.getHandshakeHeaders().containsKey("user-agent")) {
            this.userAgent = session.getHandshakeHeaders().getFirst("user-agent");
        }
    }
    
    public void updateActivity() {
        this.lastActiveTime = LocalDateTime.now();
    }
    
    public boolean isInactive(int timeoutMinutes) {
        return !isActive || LocalDateTime.now().minusMinutes(timeoutMinutes).isAfter(lastActiveTime);
    }
} 