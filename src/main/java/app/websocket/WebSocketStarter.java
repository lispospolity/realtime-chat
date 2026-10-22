package app.websocket;

import org.jspecify.annotations.NonNull;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class WebSocketStarter implements ApplicationRunner {
    private final ChatWebsocketHandler server;

    public WebSocketStarter(ChatWebsocketHandler server) {
        this.server = server;
    }

    @Override
    public void run(@NonNull ApplicationArguments args) {
        server.start();
    }
}
