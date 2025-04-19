package thanhpro0703.ChatAnonymousApp.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import thanhpro0703.ChatAnonymousApp.service.ChatStatsService;
import thanhpro0703.ChatAnonymousApp.service.RoomManager;

@Controller
public class AboutController {
    
    private final RoomManager roomManager;
    private final ChatStatsService chatStatsService;
    
    public AboutController(RoomManager roomManager, ChatStatsService chatStatsService) {
        this.roomManager = roomManager;
        this.chatStatsService = chatStatsService;
    }
    
    @GetMapping("/about")
    public String about(Model model) {
        model.addAttribute("waitingUsers", roomManager.getWaitingCount());
        model.addAttribute("activePairs", roomManager.getPairedCount());
        int onlineCount = Math.max(1, chatStatsService.getActiveConnectionsCount());
        model.addAttribute("onlineCount", onlineCount);
        model.addAttribute("currentPage", "about");
        return "about";
    }
} 