package app.websocket;

import app.auth.AuthService;
import app.auth.dto.response.AuthenticationResponse;
import app.chat.ChatService;
import app.dto.response.ErrorResponse;
import app.dto.response.GenericResponse;
import app.logging.Debug;
import app.dto.Cookies;
import app.session.Session;
import app.websocket.dto.MessageDispatch;
import app.websocket.dto.MessageType;
import app.websocket.dto.request.JoinRoomRequest;
import app.websocket.dto.request.SendMessageRequest;
import com.google.gson.*;
import org.java_websocket.server.WebSocketServer;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import static app.websocket.dto.MessageType.SERVER_RESPONSE;

public class ChatWebsocketHandler extends WebSocketServer {
    private final AuthService authService;
    private final ChatService chatService;
    private final Gson gson;

    private final HashMap<WebSocket, Session> connectionsAuthenticated = new HashMap<>();
    private final HashSet<WebSocket> connections = new HashSet<>();

    public ChatWebsocketHandler(int port, AuthService authService, ChatService chatService) {
        super(new InetSocketAddress(port));
        this.chatService = chatService;
        this.authService = authService;
        gson = new Gson();
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        connections.add(conn);
        String cookieHeader = handshake.getFieldValue("Cookie");

        HashMap<String, String> cookies = Cookies.parse(cookieHeader);

        String token = cookies.get("session_token");

        Object response = authService.authenticate(token).response();
        if (!(response instanceof AuthenticationResponse(boolean success, Session session, String message))) {

            if (!(response instanceof ErrorResponse errorResponse)) return;
            conn.close(4001, errorResponse.error());
            return;
        }

        if (!success) {
            conn.close(4001, message);
            return;
        }

        connectionsAuthenticated.put(
                conn,
                session
        );
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        if (connectionsAuthenticated.containsKey(conn)) chatService.userLeft(connectionsAuthenticated.get(conn));
        connections.remove(conn);
        connectionsAuthenticated.remove(conn);
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        Session session = connectionsAuthenticated.get(conn);
        if (session == null) return;
        Long expiresAt = session.expiresAtMillis();

        boolean sessionActive;
        if (expiresAt == null) sessionActive = true;
        else sessionActive = expiresAt > System.currentTimeMillis();
        if (!sessionActive) return;

        if (message == null) return;
        JsonObject json = JsonParser.parseString(message).getAsJsonObject();

        int type;
        try {
            type = json.get("t").getAsInt();
        } catch (NumberFormatException | NullPointerException | UnsupportedOperationException ex) {
            Debug.log("ChatWebsocketHandler: onMessage: Suspicious message coming from user Id: "+ session.userId() +", message:"+ message);
            return;
        }

        MessageType msgType = MessageType.fromCode(type);

        switch (msgType) {
            case JOIN_ROOM -> {
                JoinRoomRequest request;
                try {
                    request = gson.fromJson(message, JoinRoomRequest.class);
                } catch (JsonSyntaxException ex) {
                    Debug.log("ChatWebsocketHandler: onMessage: Invalid json coming from user Id: "+ session.userId());
                    return;
                }

                GenericResponse response = chatService.joinRoom(
                        session,
                        request.getRoom()
                );
                sendTo(conn, SERVER_RESPONSE.code, response);
            }
            case SEND_MESSAGE -> {
                SendMessageRequest request;
                try {
                    request = gson.fromJson(message, SendMessageRequest.class);
                } catch (JsonSyntaxException ex) {
                    Debug.log("ChatWebsocketHandler: onMessage: Invalid json coming from user Id: "+ session.userId());
                    return;
                }

                MessageDispatch<?> response = chatService.sendMessage(
                        session,
                        request.getRoom(),
                        request.getMessageId(),
                        request.getMessage()
                );
                sendTo(response.recipients(), MessageType.INCOMING_MESSAGE.code, response.response());
            }
            default -> {}
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

        if (conn == null || !conn.isOpen()) return;
        conn.send(payload);
    }

    public void sendTo(List<Session> recipients, double type, Object message) {
        HashMap<String, Object> data = new HashMap<>();
        data.put("t", type);
        data.put("data", message);
        String payload = gson.toJson(data);

        for (WebSocket conn : connectionsAuthenticated.keySet()) {
            Session sesh = connectionsAuthenticated.get(conn);
            for (Session session : recipients) {
                if (!sesh.sessionId().equals(session.sessionId())) continue;

                if (conn == null || !conn.isOpen()) continue;
                conn.send(payload);
            }
        }
    }

    private void disconnectUnauthenticated(int code, String message) {
        for (WebSocket conn : connections) {
            if (!connectionsAuthenticated.containsKey(conn))
                conn.close(code, message);
        }
    }

    private void disconnectAll(int code, String message) {
        for (WebSocket conn : connections) {
            conn.close(code, message);
        }
    }

}
