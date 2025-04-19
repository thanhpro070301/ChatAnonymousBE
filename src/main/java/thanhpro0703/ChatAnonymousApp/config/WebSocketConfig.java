package thanhpro0703.ChatAnonymousApp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;
import thanhpro0703.ChatAnonymousApp.websocket.ChatWebSocketHandler;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final ChatWebSocketHandler chatWebSocketHandler;

    public WebSocketConfig(ChatWebSocketHandler chatWebSocketHandler) {
        this.chatWebSocketHandler = chatWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(chatWebSocketHandler, "/chat")
                .setAllowedOrigins("*"); // Cho phép tất cả các origin
        
        // Cũng đăng ký endpoint SockJS nếu client sử dụng SockJS
        registry.addHandler(chatWebSocketHandler, "/chat")
                .setAllowedOrigins("*")
                .withSockJS();
    }
    
    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        // Tăng kích thước buffer cho tin nhắn văn bản (1MB)
        container.setMaxTextMessageBufferSize(1024 * 1024);
        // Tăng kích thước buffer cho tin nhắn nhị phân (1MB)
        container.setMaxBinaryMessageBufferSize(1024 * 1024);
        // Thiết lập thời gian chờ kết nối lên 30 phút
        container.setMaxSessionIdleTimeout(30 * 60 * 1000L);
        return container;
    }
} 