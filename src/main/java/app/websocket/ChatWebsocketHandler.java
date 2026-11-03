package app.websocket;

import app.auth.AuthService;
import app.chat.ChatService;
import app.logging.Debug;
import app.dto.Cookies;
import app.websocket.dto.MessageDispatch;
import app.websocket.dto.MessageType;
import app.websocket.dto.request.JoinRoomRequest;
import app.websocket.dto.request.SendMessageRequest;
import app.auth.dto.response.AuthenticationResponse;
import app.dto.response.ErrorResponse;

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

    private final HashMap<WebSocket, String> connToUserId = new HashMap<>();
    private final HashMap<WebSocket, String> connectionsAuthenticated = new HashMap<>();
    private final HashSet<WebSocket> waiting = new HashSet<>();

    public ChatWebsocketHandler(int port, AuthService authService, ChatService chatService) {
        super(new InetSocketAddress(port));
        this.chatService = chatService;
        this.authService = authService;
        gson = new Gson();
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        waiting.add(conn);
        String cookieHeader = handshake.getFieldValue("Cookie");

        HashMap<String, String> cookies = Cookies.parse(cookieHeader);

        String token = cookies.get("session_token");

        Object response = authService.authenticate(token).response();
        if (!(response instanceof AuthenticationResponse(boolean success, String userId, String message))) {

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
                token
        );
        connToUserId.put(
                conn,
                userId
        );

        waiting.remove(conn);
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        if (connectionsAuthenticated.containsKey(conn)) chatService.userLeft(connectionsAuthenticated.get(conn));
        waiting.remove(conn);
        connectionsAuthenticated.remove(conn);
        connToUserId.remove(conn);
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        String token = connectionsAuthenticated.get(conn);
        Object authResponse = authService.authenticate(token).response();
        if (!(authResponse instanceof AuthenticationResponse(boolean authSuccess, String userId, String authMessage))) {

            if (!(authResponse instanceof ErrorResponse errorResponse)) return;
            conn.close(4001, errorResponse.error());
            return;
        }
        if (!authSuccess) {
            conn.close(4001, authMessage);
            return;
        }

        if (message == null) return;
        JsonObject json = JsonParser.parseString(message).getAsJsonObject();

        int type;
        try {
            type = json.get("t").getAsInt();
        } catch (NumberFormatException | NullPointerException | UnsupportedOperationException ex) {
            Debug.log("ChatWebsocketHandler: onMessage: Suspicious message coming from user Id: "+ userId +", message:"+ message);
            return;
        }

        MessageType msgType = MessageType.fromCode(type);

        switch (msgType) {
            case JOIN_ROOM -> {
                JoinRoomRequest request;
                try {
                    request = gson.fromJson(message, JoinRoomRequest.class);
                } catch (JsonSyntaxException ex) {
                    Debug.log("ChatWebsocketHandler: onMessage: Invalid json coming from user Id: "+ userId);
                    return;
                }

                Object response = chatService.joinRoom(
                        userId,
                        request.getRoom()
                );
                sendTo(conn, SERVER_RESPONSE.code, response);
            }
            case SEND_MESSAGE -> {
                SendMessageRequest request;
                try {
                    request = gson.fromJson(message, SendMessageRequest.class);
                } catch (JsonSyntaxException ex) {
                    Debug.log("ChatWebsocketHandler: onMessage: Invalid json coming from user Id: "+ userId);
                    return;
                }

                MessageDispatch<?, ?> dispatch = chatService.sendMessage(
                        userId,
                        request.getRoom(),
                        request.getMessageId(),
                        request.getMessage()
                );

                sendTo(conn, SERVER_RESPONSE.code, dispatch.responseToSender());

                if (dispatch.recipients() == null) return;
                sendTo(dispatch.recipients(), MessageType.INCOMING_MESSAGE.code, dispatch.messageToRecipients());
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

    public void sendTo(List<String> recipients, double type, Object message) {
        HashMap<String, Object> data = new HashMap<>();
        data.put("t", type);
        data.put("data", message);
        String payload = gson.toJson(data);

        for (WebSocket conn : connectionsAuthenticated.keySet()) {
            String userId = connToUserId.get(conn);

            for (String recipientUserId : recipients) {
                if (!userId.equals(recipientUserId)) continue;

                if (conn == null || !conn.isOpen()) return;
                conn.send(payload);
            }
        }
    }

    private void disconnectUnauthenticated(int code, String message) {
        for (WebSocket conn : waiting) {
            conn.close(code, message);
        }
    }
}
