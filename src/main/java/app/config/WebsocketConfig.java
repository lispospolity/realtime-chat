package app.config;

import app.auth.AuthService;
import app.chat.ChatService;
import app.websocket.ChatWebsocketHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WebsocketConfig {

    @Bean(destroyMethod = "stop")
    public ChatWebsocketHandler chatWebsocketHandler(AppConfig appConfig, AuthService authService, ChatService chatService) {
        return new ChatWebsocketHandler(appConfig.webSocketPort, authService, chatService);
    }
}
