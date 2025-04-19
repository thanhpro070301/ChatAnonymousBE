package thanhpro0703.ChatAnonymousApp.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import thanhpro0703.ChatAnonymousApp.model.UserSession;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
public class ChatStatsService {
    private final RoomManager roomManager;
    
    // Số lượng thống kê
    private AtomicInteger totalConnections = new AtomicInteger(0);
    private AtomicInteger totalMessages = new AtomicInteger(0);
    private AtomicInteger totalPairings = new AtomicInteger(0);
    private AtomicInteger peakConcurrentUsers = new AtomicInteger(0);
    private AtomicInteger totalReconnections = new AtomicInteger(0);
    // Theo dõi chính xác số kết nối hiện tại, khởi tạo với giá trị 1 để tránh trả về 0
    private AtomicInteger activeConnections = new AtomicInteger(1);
    
    // Thời gian bắt đầu theo dõi
    private LocalDateTime startTime = LocalDateTime.now();
    
    // Lưu trữ thống kê theo thời gian (mỗi giờ)
    private ConcurrentHashMap<String, Integer> hourlyStats = new ConcurrentHashMap<>();
    
    @Autowired
    public ChatStatsService(RoomManager roomManager) {
        this.roomManager = roomManager;
        // Đảm bảo giá trị ban đầu là 1
        this.activeConnections.set(1);
        log.info("ChatStatsService initialized with active connections set to 1");
    }

    /**
     * Cập nhật thống kê khi có kết nối mới
     */
    public void recordNewConnection() {
        totalConnections.incrementAndGet();
        updatePeakConcurrentUsers();
    }
    
    /**
     * Cập nhật thống kê khi có tin nhắn mới
     */
    public void recordNewMessage() {
        totalMessages.incrementAndGet();
    }
    
    /**
     * Cập nhật thống kê khi có ghép cặp mới
     */
    public void recordNewPairing() {
        totalPairings.incrementAndGet();
    }
    
    /**
     * Cập nhật thống kê khi có kết nối lại
     */
    public void recordReconnection() {
        totalReconnections.incrementAndGet();
    }
    
    /**
     * Cập nhật số lượng kết nối hiện tại
     * @param count Số lượng kết nối hiện tại
     */
    public void updateActiveConnections(int count) {
        // Đảm bảo giá trị không bao giờ nhỏ hơn 1
        int updatedCount = Math.max(1, count);
        activeConnections.set(updatedCount);
        updatePeakConcurrentUsers();
        log.debug("Updated active connections count: {}", updatedCount);
    }
    
    /**
     * Lấy số lượng kết nối hiện tại
     */
    public int getActiveConnectionsCount() {
        // Đảm bảo không bao giờ trả về giá trị 0
        return Math.max(1, activeConnections.get());
    }
    
    /**
     * Kiểm tra và cập nhật số người dùng đồng thời cao nhất
     */
    private void updatePeakConcurrentUsers() {
        // Sử dụng activeConnections thay vì tính toán từ roomManager
        int currentUsers = activeConnections.get();
        int peak = peakConcurrentUsers.get();
        if (currentUsers > peak) {
            peakConcurrentUsers.set(currentUsers);
            log.info("New peak concurrent users: {}", currentUsers);
        }
    }
    
    /**
     * Lấy tất cả thống kê
     */
    public Map<String, Object> getAllStats() {
        Map<String, Object> stats = new HashMap<>();
        
        // Thống kê cơ bản
        stats.put("waitingUsers", roomManager.getWaitingCount());
        stats.put("activePairs", roomManager.getPairedCount());
        // Đảm bảo onlineCount không bao giờ nhỏ hơn 1
        stats.put("onlineCount", Math.max(1, activeConnections.get()));
        stats.put("totalConnections", totalConnections.get());
        stats.put("totalMessages", totalMessages.get());
        stats.put("totalPairings", totalPairings.get());
        stats.put("peakConcurrentUsers", Math.max(1, peakConcurrentUsers.get()));
        stats.put("totalReconnections", totalReconnections.get());
        stats.put("startTime", startTime.toString());
        stats.put("uptime", getUptimeInMinutes());
        
        return stats;
    }
    
    /**
     * Lấy số phút hoạt động từ khi bắt đầu
     */
    private long getUptimeInMinutes() {
        return java.time.Duration.between(startTime, LocalDateTime.now()).toMinutes();
    }
    
    /**
     * Xử lý thống kê theo lịch trình
     * Chạy mỗi giờ để lưu thống kê
     */
    @Scheduled(cron = "0 0 * * * *") // Chạy vào đầu mỗi giờ
    public void processHourlyStats() {
        String hourKey = LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH"));
        // Đảm bảo giá trị không bao giờ nhỏ hơn 1
        int currentUsers = Math.max(1, activeConnections.get());
        hourlyStats.put(hourKey + "-users", currentUsers);
        hourlyStats.put(hourKey + "-messages", totalMessages.get());
        log.info("Recorded hourly stats: {} users, {} messages", currentUsers, totalMessages.get());
    }
    
    /**
     * Lấy thống kê theo giờ
     */
    public Map<String, Map<String, Integer>> getHourlyStats(int hours) {
        Map<String, Map<String, Integer>> result = new HashMap<>();
        LocalDateTime now = LocalDateTime.now();
        
        for (int i = 0; i < hours; i++) {
            LocalDateTime time = now.minusHours(i);
            String hourKey = time.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH"));
            
            Map<String, Integer> hourData = new HashMap<>();
            // Đảm bảo giá trị không bao giờ nhỏ hơn 1
            hourData.put("users", Math.max(1, hourlyStats.getOrDefault(hourKey + "-users", 1)));
            hourData.put("messages", hourlyStats.getOrDefault(hourKey + "-messages", 0));
            
            result.put(hourKey, hourData);
        }
        
        return result;
    }
    
    /**
     * Reset thống kê
     */
    public void resetStats() {
        totalConnections.set(0);
        totalMessages.set(0);
        totalPairings.set(0);
        peakConcurrentUsers.set(0);
        totalReconnections.set(0);
        // Đảm bảo activeConnections vẫn có ít nhất 1 sau khi reset
        activeConnections.set(1);
        startTime = LocalDateTime.now();
        hourlyStats.clear();
        log.info("All statistics have been reset, active connections set to 1");
    }
} 