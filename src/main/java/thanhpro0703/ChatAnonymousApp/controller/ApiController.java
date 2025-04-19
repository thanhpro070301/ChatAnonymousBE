package thanhpro0703.ChatAnonymousApp.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import thanhpro0703.ChatAnonymousApp.service.ChatStatsService;
import thanhpro0703.ChatAnonymousApp.service.RoomManager;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final RoomManager roomManager;
    private final ChatStatsService chatStatsService;

    public ApiController(RoomManager roomManager, ChatStatsService chatStatsService) {
        this.roomManager = roomManager;
        this.chatStatsService = chatStatsService;
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getBasicStats() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "online");
        
        int waitingUsers = roomManager.getWaitingCount();
        int activePairs = roomManager.getPairedCount();
        int onlineCount = chatStatsService.getActiveConnectionsCount();
        
        // Đảm bảo onlineCount không bao giờ là 0
        if (onlineCount == 0) {
            // Nếu có người waiting hoặc ghép cặp thì dùng cách tính cũ
            if (waitingUsers > 0 || activePairs > 0) {
                onlineCount = waitingUsers + (activePairs * 2);
            } else {
                // Nếu không có ai thì giả định ít nhất có 1 người
                onlineCount = 1;
            }
            // Cập nhật lại thống kê sau khi tính toán
            chatStatsService.updateActiveConnections(onlineCount);
        }
        
        // Cập nhật các giá trị vào response
        response.put("waitingUsers", waitingUsers);
        response.put("activePairs", activePairs);
        response.put("onlineCount", onlineCount);
        response.put("chattingCount", activePairs * 2);
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/stats/advanced")
    public ResponseEntity<Map<String, Object>> getAdvancedStats() {
        Map<String, Object> stats = chatStatsService.getAllStats();
        
        // Đảm bảo onlineCount không bao giờ là 0
        if ((Integer)stats.getOrDefault("onlineCount", 0) == 0) {
            int waitingUsers = (Integer)stats.getOrDefault("waitingUsers", 0);
            int activePairs = (Integer)stats.getOrDefault("activePairs", 0);
            
            if (waitingUsers > 0 || activePairs > 0) {
                stats.put("onlineCount", waitingUsers + (activePairs * 2));
            } else {
                stats.put("onlineCount", 1);
            }
            
            // Cập nhật lại thống kê
            chatStatsService.updateActiveConnections((Integer)stats.get("onlineCount"));
        }
        
        return ResponseEntity.ok(stats);
    }
    
    @GetMapping("/stats/hourly")
    public ResponseEntity<Map<String, Map<String, Integer>>> getHourlyStats(
            @RequestParam(defaultValue = "24") int hours) {
        return ResponseEntity.ok(chatStatsService.getHourlyStats(hours));
    }
    
    @PostMapping("/reset")
    public ResponseEntity<Map<String, Object>> resetSessions(
            @RequestHeader(value = "X-Admin-Key", required = false) String adminKey) {
        
        // Kiểm tra xác thực admin
        if (adminKey == null || !adminKey.equals(System.getenv("CHAT_ADMIN_KEY"))) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Unauthorized access");
            return ResponseEntity.status(401).body(errorResponse);
        }
        
        // Thực hiện reset
        roomManager.resetAll();
        chatStatsService.resetStats();
        
        // Đảm bảo có ít nhất 1 người online sau khi reset
        chatStatsService.updateActiveConnections(1);
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "All sessions and statistics have been reset");
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "Chat Anonymous Service");
        response.put("version", "1.0.0");
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }
} 