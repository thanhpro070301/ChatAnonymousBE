package thanhpro0703.ChatAnonymousApp.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import thanhpro0703.ChatAnonymousApp.service.ChatStatsService;
import thanhpro0703.ChatAnonymousApp.service.RoomManager;

@Controller
public class HomeController {
    
    private final RoomManager roomManager;
    private final ChatStatsService chatStatsService;
    
    public HomeController(RoomManager roomManager, ChatStatsService chatStatsService) {
        this.roomManager = roomManager;
        this.chatStatsService = chatStatsService;
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("currentPage", "home");
        int onlineCount = Math.max(1, chatStatsService.getActiveConnectionsCount());
        model.addAttribute("onlineCount", onlineCount);
        return "home";
    }
    
    @GetMapping("/backup")
    public String backup() {
        return "redirect:/index-backup.html";
    }
    
    @GetMapping("/test")
    @ResponseBody
    public String test() {
        return "Server is working!";
    }
    
    @GetMapping("/websocket-test")
    public String websocketTest() {
        return "websocket-test";
    }
    
    @GetMapping("/chat/random")
    public String randomChat(Model model) {
        model.addAttribute("currentPage", "chat");
        return "chat";
    }
    
    @GetMapping("/chat/create")
    public String createChat(Model model) {
        // Generate a unique room ID
        String roomId = "room_" + System.currentTimeMillis();
        model.addAttribute("roomId", roomId);
        model.addAttribute("currentPage", "chat");
        return "chat";
    }
    
    @GetMapping("/error-page")
    public String errorPage(Model model) {
        model.addAttribute("errorMessage", "Đã xảy ra lỗi! Hãy thử làm mới trang.");
        model.addAttribute("errorDetails", "Nếu lỗi vẫn tiếp tục, hãy liên hệ với quản trị viên.");
        return "error-simple";
    }
} 