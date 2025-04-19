package thanhpro0703.ChatAnonymousApp.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Message {
    private String type; // "message", "leave", "join", "image", "connected", "status", "stats_update"
    private String content;
    private String sender;
    private String imageData; // Base64 encoded image data
    private String videoData; // Base64 encoded video data
    private String senderId; // ID của người gửi, hỗ trợ xác định khi reload
    private String receiver; // Người nhận tin nhắn
    private Boolean selfDestruct; // Whether the image should self-destruct
    private Integer selfDestructTime; // Self-destruct time in seconds
    private Map<String, Object> statsData; // Dữ liệu thống kê
    
    // Constructor không có senderId và receiver để tương thích với mã cũ
    public Message(String type, String content, String sender, String imageData) {
        this.type = type;
        this.content = content;
        this.sender = sender;
        this.imageData = imageData;
        this.videoData = null;
        this.senderId = null;
        this.receiver = null;
        this.selfDestruct = false;
        this.selfDestructTime = null;
        this.statsData = null;
    }
} 