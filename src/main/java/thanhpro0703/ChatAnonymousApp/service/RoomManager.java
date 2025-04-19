package thanhpro0703.ChatAnonymousApp.service;

import org.springframework.stereotype.Component;
import thanhpro0703.ChatAnonymousApp.model.Message;
import thanhpro0703.ChatAnonymousApp.model.UserSession;
import thanhpro0703.ChatAnonymousApp.config.ApplicationContextProvider;
import thanhpro0703.ChatAnonymousApp.service.ChatStatsService;

import java.io.IOException;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.springframework.web.socket.TextMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class RoomManager {
    private Queue<UserSession> waitingQueue = new ConcurrentLinkedQueue<>();
    private Map<String, String> pairedUsers = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public synchronized void addToQueue(UserSession user) {
        if (user == null || user.getSocket() == null || !user.getSocket().isOpen()) {
            log.warn("Attempted to add null or closed session to queue");
            return;
        }

        String sessionId = user.getSessionId();
        
        // Kiểm tra xem đã được ghép cặp chưa
        String existingPartnerId = getPartnerId(sessionId);
        if (existingPartnerId != null) {
            log.info("User {} is already paired with {}, not adding to queue", sessionId, existingPartnerId);
            return;
        }
        
        // Kiểm tra xem đã có trong hàng đợi chưa
        if (isWaiting(sessionId)) {
            log.info("User {} is already in waiting queue", sessionId);
            return;
        }
        
        log.info("Adding user {} to waiting queue (size before: {})", sessionId, waitingQueue.size());
        
        // Tìm một phiên có sẵn trong hàng đợi
        if (!waitingQueue.isEmpty()) {
            // Lấy người đầu tiên từ hàng đợi
            UserSession partner = waitingQueue.poll();
            
            // Kiểm tra session có còn hoạt động không
            if (partner == null || partner.getSocket() == null || !partner.getSocket().isOpen()) {
                // Nếu session không còn hoạt động, thêm người dùng hiện tại vào hàng đợi và bỏ qua
                waitingQueue.add(user);
                log.info("Removed inactive session from queue and added {} to waiting queue", sessionId);
                return;
            }

            try {
                // Tránh ghép cặp người dùng với chính mình
                if (partner.getSessionId().equals(sessionId)) {
                    log.warn("Attempted to pair user with themselves: {}", sessionId);
                    waitingQueue.add(user);
                    return;
                }
                
                // Tạo ghép cặp
                pairedUsers.put(sessionId, partner.getSessionId());
                pairedUsers.put(partner.getSessionId(), sessionId);
                
                log.info("Paired users: {} and {}", sessionId, partner.getSessionId());
                
                // Thông báo cho cả hai theo đúng thứ tự
                notifyPaired(user, partner);
            } catch (Exception e) {
                log.error("Error pairing users", e);
                // Đưa cả hai user trở lại hàng đợi nếu có lỗi
                if (!waitingQueue.contains(user)) {
                    waitingQueue.add(user);
                }
                if (!waitingQueue.contains(partner)) {
                    waitingQueue.add(partner);
                }
                // Xóa khỏi danh sách ghép cặp nếu có lỗi
                pairedUsers.remove(sessionId);
                pairedUsers.remove(partner.getSessionId());
            }
        } else {
            // Nếu không có ai trong hàng đợi, thêm người này vào
            waitingQueue.add(user);
            log.info("User {} added to waiting queue, new size: {}", sessionId, waitingQueue.size());
        }
    }

    private void notifyPaired(UserSession user1, UserSession user2) {
        try {
            // Tạo thông báo kết nối với type là "connected" và thêm thông tin senderId
            Message pairMessage1 = new Message("connected", "Đã kết nối với người lạ", "system", null);
            Message pairMessage2 = new Message("connected", "Đã kết nối với người lạ", "system", null);
            
            // Thêm ID của đối phương vào tin nhắn
            pairMessage1.setSenderId(user2.getSessionId());
            pairMessage2.setSenderId(user1.getSessionId());
            
            String messageJson1 = objectMapper.writeValueAsString(pairMessage1);
            String messageJson2 = objectMapper.writeValueAsString(pairMessage2);
            
            // Gửi tin nhắn với ID của đối phương
            user1.getSocket().sendMessage(new TextMessage(messageJson1));
            user2.getSocket().sendMessage(new TextMessage(messageJson2));
            
            // Log success để debug
            log.debug("Sent paired notification to users {} and {}", user1.getSessionId(), user2.getSessionId());
            
            // Gửi thông báo đến tất cả các session để cập nhật số liệu thống kê
            broadcastStats();
            
            // Ghi nhận thống kê ghép cặp nếu có dịch vụ thống kê
            try {
                ChatStatsService statsService = ApplicationContextProvider.getBean(ChatStatsService.class);
                if (statsService != null) {
                    statsService.recordNewPairing();
                }
            } catch (Exception e) {
                log.warn("Could not record pairing stats: {}", e.getMessage());
            }
        } catch (IOException e) {
            log.error("Error notifying paired users", e);
        }
    }

    // Phương thức để phát thông tin thống kê khi có thay đổi
    private void broadcastStats() {
        // Thực hiện trong luồng riêng để không ảnh hưởng đến luồng chính
        new Thread(() -> {
            try {
                Thread.sleep(500); // Đợi một chút để đảm bảo các thay đổi đã được cập nhật
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    public String getPartnerId(String sessionId) {
        return pairedUsers.get(sessionId);
    }

    public synchronized void removePair(String sessionId) {
        String partner = pairedUsers.remove(sessionId);
        if (partner != null) {
            pairedUsers.remove(partner);
            log.info("Removed pairing between {} and {}", sessionId, partner);
        }
    }

    public synchronized void removeFromQueue(UserSession user) {
        if (user != null) {
            String sessionId = user.getSessionId();
            // Tìm và xóa theo sessionId để đảm bảo loại bỏ đúng người dùng
            boolean removed = waitingQueue.removeIf(session -> 
                session != null && session.getSessionId() != null && session.getSessionId().equals(sessionId));
            
            if (removed) {
                log.info("Removed user {} from waiting queue", sessionId);
            } else {
                log.debug("User {} was not in waiting queue", sessionId);
            }
        }
    }
    
    public synchronized boolean isWaiting(String sessionId) {
        return waitingQueue.stream()
                .anyMatch(session -> 
                    session != null && 
                    session.getSessionId() != null && 
                    session.getSessionId().equals(sessionId));
    }
    
    public synchronized int getWaitingCount() {
        return waitingQueue.size();
    }
    
    public synchronized int getPairedCount() {
        return pairedUsers.size() / 2;  // Each pair is counted twice in the map
    }

    public void resetAll() {
        log.info("Resetting all chat sessions and pairings");
        waitingQueue.clear();
        pairedUsers.clear();
    }

    /**
     * Tạo cặp trực tiếp giữa hai người dùng mà không qua hàng đợi
     * Chủ yếu sử dụng khi khôi phục phiên
     */
    public synchronized void createPairDirectly(String userId1, String userId2) {
        log.info("Creating direct pair between {} and {}", userId1, userId2);
        
        // Xóa bỏ các ghép cặp hiện có nếu có
        removePair(userId1);
        removePair(userId2);
        
        // Đảm bảo không còn trong hàng đợi
        waitingQueue.removeIf(session -> 
            session != null && 
            session.getSessionId() != null && 
            (session.getSessionId().equals(userId1) || session.getSessionId().equals(userId2)));
        
        // Tạo ghép cặp mới
        pairedUsers.put(userId1, userId2);
        pairedUsers.put(userId2, userId1);
        
        log.info("Direct pair created between {} and {}", userId1, userId2);
    }
} 