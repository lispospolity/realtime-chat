package app.websocket;

import app.logging.Debug;
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
    Messages messages;
    Gson gson;

    HashMap<WebSocket, String> connectionsAuthenticated = new HashMap<>();
    HashSet<WebSocket> connections = new HashSet<>();
    HashMap<String, HashSet<WebSocket>> rooms = new HashMap<>();

    public ChatWebsocketHandler(int port) {
        super(new InetSocketAddress(port));
        gson = new Gson();
        messages = new Messages();
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        connections.add(conn);
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

        if (msgType == MessageType.AUTHORIZE) {

        }

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
