package app.websocket;

import app.logging.Debug;
import app.websocket.dto.Cookies;
import app.websocket.dto.MessageType;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.java_websocket.server.WebSocketServer;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class ChatWebsocketHandler extends WebSocketServer {
    private final WebsocketAuthService wsAuthService;
    private final Messages messages;
    private final Gson gson;

    private HashMap<WebSocket, Session> connectionsAuthenticated = new HashMap<>();
    private HashSet<WebSocket> connections = new HashSet<>();
    private HashMap<Integer, HashSet<WebSocket>> rooms = new HashMap<>();

    public ChatWebsocketHandler(int port, WebsocketAuthService websocketAuthService) {
        super(new InetSocketAddress(port));
        wsAuthService = websocketAuthService;
        messages = new Messages();
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
        //continue the authorization
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

        if (!connectionsAuthenticated.containsKey(conn)) return;

        if (msgType == MessageType.SEND_MESSAGE) {

        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        Debug.log("ChatWebsocketHandler: onError(): "+ex);
    }

    @Override
    public void onStart() {

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
