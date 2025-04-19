package thanhpro0703.ChatAnonymousApp.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import thanhpro0703.ChatAnonymousApp.model.Message;
import thanhpro0703.ChatAnonymousApp.model.UserSession;
import thanhpro0703.ChatAnonymousApp.service.ChatStatsService;
import thanhpro0703.ChatAnonymousApp.service.RoomManager;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Slf4j
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final RoomManager roomManager;
    private final ChatStatsService chatStatsService;
    private final Map<String, UserSession> sessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;
    private final AtomicInteger activeConnections = new AtomicInteger(0);

    public ChatWebSocketHandler(RoomManager roomManager, ChatStatsService chatStatsService) {
        this.roomManager = roomManager;
        this.chatStatsService = chatStatsService;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String sessionId = session.getId();
        log.info("New connection established: {}", sessionId);
        
        int currentActive = activeConnections.incrementAndGet();
        log.info("Active connections: {}", currentActive);
        
        try {
            UserSession user = new UserSession(sessionId, session);
            
            String partnerId = roomManager.getPartnerId(sessionId);
            
            if (partnerId != null && sessions.containsKey(partnerId)) {
                log.info("Session reconnecting: {} (paired with {})", sessionId, partnerId);
                sessions.put(sessionId, user);
                
                chatStatsService.recordReconnection();
                
                sendMessage(session, new Message("connected", "Đã kết nối với người lạ", "system", null));
                
                WebSocketSession partnerSession = sessions.get(partnerId).getSocket();
                if (partnerSession != null && partnerSession.isOpen()) {
                    sendMessage(partnerSession, new Message("status", "Người lạ đã kết nối lại", "system", null));
                }
            } else {
                sessions.put(sessionId, user);
                
                chatStatsService.recordNewConnection();
                
                roomManager.addToQueue(user);
            }
            
            chatStatsService.updateActiveConnections(currentActive);
            
            broadcastStats();
            
        } catch (Exception e) {
            log.error("Error during connection establishment", e);
            activeConnections.decrementAndGet();
            throw e;
        }
    }

    public int getActiveConnectionsCount() {
        return activeConnections.get();
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String sessionId = session.getId();
        log.info("Connection closed: {} - {}", sessionId, status);
        
        int currentActive = activeConnections.updateAndGet(current -> Math.max(1, current - 1));
        log.info("Active connections after close: {}", currentActive);
        
        UserSession user = sessions.remove(sessionId);
        
        if (user != null) {
            roomManager.removeFromQueue(user);
            
            String partnerId = roomManager.getPartnerId(sessionId);
            
            if (partnerId != null) {
                UserSession partner = sessions.get(partnerId);
                if (partner != null && partner.getSocket().isOpen()) {
                    try {
                        Message disconnectMessage = new Message("status", "Người lạ đã ngắt kết nối", "system", null);
                        String messageJson = objectMapper.writeValueAsString(disconnectMessage);
                        partner.getSocket().sendMessage(new TextMessage(messageJson));
                    } catch (IOException e) {
                        log.error("Error sending disconnect notification", e);
                    }
                }
            }
            
            roomManager.removePair(sessionId);
        }
        
        chatStatsService.updateActiveConnections(currentActive);
        
        broadcastStats();
        
        super.afterConnectionClosed(session, status);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage textMessage) throws Exception {
        String sessionId = session.getId();
        
        if (sessions.containsKey(sessionId)) {
            sessions.get(sessionId).updateActivity();
        }
        
        try {
            String payload = textMessage.getPayload();
            int payloadSize = payload.length();
            log.info("Message received from session {}, size: {} bytes", sessionId, payloadSize);
            
            if (payloadSize > 50000000) {
                log.warn("Message too large from session {}, size: {} bytes", sessionId, payloadSize);
                sendMessage(session, new Message("status", "Không thể xử lý tin nhắn vì kích thước quá lớn (tối đa 50MB)", "system", null));
                return;
            }
            
            Message message;
            try {
                message = objectMapper.readValue(payload, Message.class);
            } catch (Exception e) {
                log.error("Invalid JSON format from session {}: {}", sessionId, e.getMessage());
                sendMessage(session, new Message("status", "Tin nhắn không đúng định dạng", "system", null));
                return;
            }
            
            boolean hasImage = message.getImageData() != null && !message.getImageData().isEmpty();
            boolean hasVideo = message.getVideoData() != null && !message.getVideoData().isEmpty();
            
            if (hasImage || hasVideo) {
                if (hasImage) {
                    int imageSize = message.getImageData().length();
                    if (imageSize > 500000) {
                        log.warn("Image data too large from session {}: {} bytes", sessionId, imageSize);
                        sendMessage(session, new Message("status", "Hình ảnh quá lớn, không thể gửi (tối đa 500KB)", "system", null));
                        return;
                    }
                    
                    log.info("Message contains image data, size: {} bytes, type: {}, selfDestruct: {}, selfDestructTime: {}", 
                            imageSize, message.getType(), message.getSelfDestruct(), message.getSelfDestructTime());
                }
                
                if (hasVideo) {
                    int videoSize = message.getVideoData().length();
                    if (videoSize > 50000000) {
                        log.warn("Video data too large from session {}: {} bytes", sessionId, videoSize);
                        sendMessage(session, new Message("status", "Video quá lớn, không thể gửi (tối đa 50MB)", "system", null));
                        return;
                    }
                    
                    log.info("Message contains video data, size: {} bytes, type: {}, selfDestruct: {}, selfDestructTime: {}", 
                            videoSize, message.getType(), message.getSelfDestruct(), message.getSelfDestructTime());
                }
            }
            
            String partnerId = roomManager.getPartnerId(sessionId);
            
            log.debug("Processing message type: {}, from session: {}, to partner: {}, has image: {}, has video: {}", 
                   message.getType(), sessionId, partnerId, hasImage, hasVideo);
            
            try {
                switch (message.getType()) {
                    case "message":
                    case "image":
                        chatStatsService.recordNewMessage();
                        
                        if (partnerId != null && sessions.containsKey(partnerId)) {
                            WebSocketSession partnerSession = sessions.get(partnerId).getSocket();
                            
                            if (partnerSession != null && partnerSession.isOpen()) {
                                try {
                                    Message outMessage = new Message(
                                        hasImage || hasVideo ? "message" : message.getType(), 
                                        message.getContent(), 
                                        "stranger", 
                                        message.getImageData()
                                    );
                                    
                                    if (hasVideo) {
                                        outMessage.setVideoData(message.getVideoData());
                                    }
                                    
                                    if (message.getSenderId() != null) {
                                        outMessage.setSenderId(message.getSenderId());
                                    }
                                    if (message.getReceiver() != null) {
                                        outMessage.setReceiver(message.getReceiver());
                                    }
                                    
                                    if (message.getSelfDestruct() != null) {
                                        outMessage.setSelfDestruct(message.getSelfDestruct());
                                        log.info("Setting selfDestruct: {} for outgoing message", message.getSelfDestruct());
                                    }
                                    if (message.getSelfDestructTime() != null) {
                                        outMessage.setSelfDestructTime(message.getSelfDestructTime());
                                        log.info("Setting selfDestructTime: {} for outgoing message", message.getSelfDestructTime());
                                    }
                                    
                                    log.info("Outgoing message to partner {}: type={}, hasImage={}, hasVideo={}, selfDestruct={}, selfDestructTime={}", 
                                            partnerId, outMessage.getType(), (outMessage.getImageData() != null), 
                                            (outMessage.getVideoData() != null), outMessage.getSelfDestruct(), outMessage.getSelfDestructTime());
                                    
                                    boolean sent = sendMessage(partnerSession, outMessage);
                                    if (!sent) {
                                        log.warn("Failed to send message to partner {}", partnerId);
                                        sendMessage(session, new Message("status", "Đối tượng chat không thể nhận tin nhắn", "system", null));
                                    }
                                } catch (Exception e) {
                                    log.error("Error sending message to partner: {}", e.getMessage(), e);
                                    sendMessage(session, new Message("status", "Không thể gửi tin nhắn tới người nhận: " + e.getMessage(), "system", null));
                                }
                            } else {
                                log.warn("Partner session {} is not available or closed", partnerId);
                                sendMessage(session, new Message("status", "Đối tượng chat có thể đã offline, tin nhắn có thể không được nhận", "system", null));
                            }
                        } else {
                            if (sessions.containsKey(sessionId)) {
                                roomManager.addToQueue(sessions.get(sessionId));
                            }
                        }
                        break;
                    case "leave":
                        log.info("Session {} requested to leave chat", sessionId);
                        disconnect(sessionId, true);
                        break;
                    case "find":
                        log.info("Session {} requested to find new partner", sessionId);
                        if (partnerId != null) {
                            notifyPartnerLeft(partnerId);
                            roomManager.removePair(sessionId);
                        }
                        
                        if (sessions.containsKey(sessionId)) {
                            roomManager.addToQueue(sessions.get(sessionId));
                        }
                        break;
                    case "connection_status":
                        log.debug("Session {} requested connection status", sessionId);
                        if (partnerId != null && sessions.containsKey(partnerId)) {
                            WebSocketSession partnerSession = sessions.get(partnerId).getSocket();
                            if (partnerSession != null && partnerSession.isOpen()) {
                                sendMessage(session, new Message("connected", "Đã kết nối với người lạ", "system", null));
                            } else {
                                sendMessage(session, new Message("status", "Đối tượng chat đang offline", "system", null));
                            }
                        } else {
                            if (sessions.containsKey(sessionId)) {
                                roomManager.addToQueue(sessions.get(sessionId));
                            }
                        }
                        break;
                    case "read_receipt":
                    case "typing":
                    case "reaction":
                        if (partnerId != null && sessions.containsKey(partnerId)) {
                            WebSocketSession partnerSession = sessions.get(partnerId).getSocket();
                            if (partnerSession != null && partnerSession.isOpen()) {
                                sendMessage(partnerSession, message);
                            }
                        }
                        break;
                    default:
                        log.warn("Unknown message type from session {}: {}", sessionId, message.getType());
                        sendMessage(session, new Message("status", "Loại tin nhắn không được hỗ trợ: " + message.getType(), "system", null));
                }
            } catch (Exception e) {
                log.error("Error processing message type {}: {}", message.getType(), e.getMessage(), e);
                sendMessage(session, new Message("status", "Lỗi xử lý tin nhắn loại " + message.getType() + ": " + e.getMessage(), "system", null));
            }
        } catch (Exception e) {
            log.error("Error processing message from session {}: {}", sessionId, e.getMessage(), e);
            try {
                sendMessage(session, new Message("status", "Đã xảy ra lỗi khi xử lý tin nhắn: " + e.getMessage(), "system", null));
            } catch (Exception ex) {
                log.error("Could not send error message to session {}: {}", sessionId, ex.getMessage());
            }
        }
    }

    private void disconnect(String sessionId, boolean reconnect) {
        if (sessionId == null) {
            return;
        }
        
        String partnerId = roomManager.getPartnerId(sessionId);
        
        if (partnerId != null && sessions.containsKey(partnerId)) {
            notifyPartnerLeft(partnerId);
        }
        
        if (roomManager.isWaiting(sessionId) && sessions.containsKey(sessionId)) {
            roomManager.removeFromQueue(sessions.get(sessionId));
        }
        
        roomManager.removePair(sessionId);
        
        if (!reconnect) {
            sessions.remove(sessionId);
        }
    }
    
    private void notifyPartnerLeft(String partnerId) {
        if (partnerId != null && sessions.containsKey(partnerId)) {
            WebSocketSession partnerSession = sessions.get(partnerId).getSocket();
            if (partnerSession != null && partnerSession.isOpen()) {
                sendMessage(partnerSession, new Message("leave", "Người lạ đã rời đi", "system", null));
            } else {
                log.warn("Partner session {} is already closed, can't notify about leaving", partnerId);
            }
        }
    }
    
    private boolean sendMessage(WebSocketSession session, Message message) {
        try {
            if (session == null || !session.isOpen()) {
                log.warn("Cannot send message - WebSocket session is null or closed");
                return false;
            }
            
            String messageJson = objectMapper.writeValueAsString(message);
            
            if (messageJson.length() > 750000) {
                log.warn("Message too large to send, size: {} bytes", messageJson.length());
                
                if (message.getImageData() != null && !message.getImageData().isEmpty()) {
                    log.info("Attempting to send message without image data");
                    message.setImageData(null);
                    message.setContent(message.getContent() + " [Hình ảnh quá lớn, không thể gửi]");
                    String reducedJson = objectMapper.writeValueAsString(message);
                    
                    if (session.isOpen()) {
                        session.sendMessage(new TextMessage(reducedJson));
                        return true;
                    } else {
                        log.warn("WebSocket session closed while trying to send reduced image message");
                        return false;
                    }
                }
                
                if (message.getVideoData() != null && !message.getVideoData().isEmpty()) {
                    log.info("Attempting to send message without video data");
                    message.setVideoData(null);
                    message.setContent(message.getContent() + " [Video quá lớn, không thể gửi]");
                    String reducedJson = objectMapper.writeValueAsString(message);
                    
                    if (session.isOpen()) {
                        session.sendMessage(new TextMessage(reducedJson));
                        return true;
                    } else {
                        log.warn("WebSocket session closed while trying to send reduced video message");
                        return false;
                    }
                }
                
                return false;
            }
            
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(messageJson));
                return true;
            } else {
                log.warn("WebSocket session closed before sending message");
                return false;
            }
        } catch (IOException e) {
            log.error("Error sending message: {}", e.getMessage(), e);
            return false;
        }
    }

    private void broadcastStats() {
        try {
            Map<String, Object> stats = chatStatsService.getAllStats();
            
            Message statsMessage = new Message("stats_update", "Thống kê đã được cập nhật", "system", null);
            statsMessage.setStatsData(stats);
            
            for (UserSession userSession : sessions.values()) {
                if (userSession.getSocket() != null && userSession.getSocket().isOpen()) {
                    try {
                        sendMessage(userSession.getSocket(), statsMessage);
                    } catch (Exception e) {
                        log.error("Error sending stats to session {}: {}", userSession.getSessionId(), e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error broadcasting stats: {}", e.getMessage());
        }
    }
} 