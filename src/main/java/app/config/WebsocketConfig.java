package app.config;

import app.websocket.ChatWebsocketHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WebsocketConfig {
    @Bean(destroyMethod = "stop")
    public ChatWebsocketHandler chatWebsocketHandler(AppConfig appConfig) {
        return new ChatWebsocketHandler(appConfig.webSocketPort);
    }
}
