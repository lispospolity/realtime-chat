package app.config;

import app.chat.ChatService;
import app.websocket.ChatWebsocketHandler;
import app.websocket.WebsocketAuthService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WebsocketConfig {

    @Bean(destroyMethod = "stop")
    public ChatWebsocketHandler chatWebsocketHandler(
            AppConfig appConfig,
            WebsocketAuthService websocketAuthService,
            ChatService chatService) {
        return new ChatWebsocketHandler(
                appConfig.webSocketPort,
                websocketAuthService,
                chatService);
    }
}
