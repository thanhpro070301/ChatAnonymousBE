package thanhpro0703.ChatAnonymousApp.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import thanhpro0703.ChatAnonymousApp.service.RoomManager;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ChatController {

    private final RoomManager roomManager;

    public ChatController(RoomManager roomManager) {
        this.roomManager = roomManager;
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "online");
        response.put("message", "Chat server is running");
        response.put("waitingUsers", roomManager.getWaitingCount());
        response.put("activePairs", roomManager.getPairedCount());
        return ResponseEntity.ok(response);
    }
} 