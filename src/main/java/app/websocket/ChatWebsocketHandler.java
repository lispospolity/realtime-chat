package app.websocket;

import app.chat.ChatService;
import app.dto.response.GenericResponse;
import app.logging.Debug;
import app.dto.Cookies;
import app.websocket.dto.MessageType;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.java_websocket.server.WebSocketServer;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import static app.websocket.dto.MessageType.SERVER_RESPONSE;

public class ChatWebsocketHandler extends WebSocketServer {
    private final WebsocketAuthService wsAuthService;
    private final ChatService chatService;
    private final Gson gson;

    private final HashMap<WebSocket, Session> connectionsAuthenticated = new HashMap<>();
    private final HashSet<WebSocket> connections = new HashSet<>();

    public ChatWebsocketHandler(int port, WebsocketAuthService websocketAuthService, ChatService chatService) {
        super(new InetSocketAddress(port));
        this.wsAuthService = websocketAuthService;
        this.chatService = chatService;
        gson = new Gson();
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        connections.add(conn);
        String cookieHeader = handshake.getFieldValue("Cookie");

        HashMap<String, String> cookies = Cookies.parse(cookieHeader);

        String token = cookies.get("session_token");
        boolean tokenIncorrect = wsAuthService.tokenIncorrect(token);

        if (token == null || tokenIncorrect) {
            conn.close(4001, "Incorrect session");
            return;
        }
        String userId = wsAuthService.getUserId(token);
        Long expiresAt = wsAuthService.sessionExpiryMillis(token);

        connectionsAuthenticated.put(
                conn,
                new Session(userId, expiresAt)
        );
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        connections.remove(conn);
        connectionsAuthenticated.remove(conn);
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        if (message == null) return;
        Map<?, ?> msg;
        try {
            msg = gson.fromJson(message, Map.class);
        } catch (JsonSyntaxException e) {
            Debug.log("ChatWebsocketHandler: onMessage: JsonSyntaxException from: " + conn);
            return;
        }
        if (!(msg.get("t") instanceof Double type)) return;
        MessageType msgType = MessageType.fromCode(type);

        boolean sessionExists = connectionsAuthenticated.containsKey(conn);
        Long expiresAt = connectionsAuthenticated.get(conn).expiresIn();
        boolean sessionActive;
        if (expiresAt == null)
            sessionActive = sessionExists;
        else
            sessionActive = sessionExists && expiresAt > System.currentTimeMillis();

        if (!sessionActive) return;

        switch (msgType) {
            case JOIN_ROOM -> {
                GenericResponse response = chatService.joinRoom(
                    conn,
                    connectionsAuthenticated.get(conn).userId(),
                    msg
                );
                sendTo(conn, SERVER_RESPONSE.code, response);
            }
            case SEND_MESSAGE -> {

            }
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        Debug.log("ChatWebsocketHandler: onError(): "+ex);
    }

    @Override
    public void onStart() {
        System.out.println("WebSocket server successfully started.");
    }

    public void sendTo(WebSocket conn, double type, Object message) {
        HashMap<String, Object> data = new HashMap<>();
        data.put("t", type);
        data.put("data", message);
        String payload = gson.toJson(data);
        conn.send(payload);
    }

    public void sendTo(Collection<WebSocket> conns, double type, Object message) {
        HashMap<String, Object> data = new HashMap<>();
        data.put("t", type);
        data.put("data", message);
        String payload = gson.toJson(data);
        for (WebSocket conn : conns) {
            conn.send(payload);
        }
    }

    private void disconnectUnauthenticated(int code, String message) {
        for (WebSocket conn : connections) {
            if (!connectionsAuthenticated.containsKey(conn))
                conn.close();
        }
    }

    private void disconnectAll(int code, String message) {
        for (WebSocket conn : connections) {
            conn.close(code, message);
        }
    }

}
