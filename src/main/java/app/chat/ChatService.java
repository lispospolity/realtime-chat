package app.chat;

import app.dto.response.GenericResponse;
import app.room.RoomService;
import app.websocket.dto.MessageDispatch;
import app.websocket.dto.MessageType;
import jakarta.validation.constraints.NotNull;
import org.java_websocket.WebSocket;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class ChatService {
    /**
     * WebSocket in the inner hashmap is for later room broadcast
     * WebSocket[] sendTo = new WebSocket[];
     * for (WebSocket conn : innerMap.keySet) {
     *     if (innerMap.get(conn) reading permissions)
     *          sendTo.put(conn);
     * }
     * respond with sendTo map and the response
     */
    private final HashMap<Integer, HashMap<WebSocket, Integer>> rooms = new HashMap<>();
    private final RoomService roomService;

    public ChatService(RoomService roomService) {
        this.roomService = roomService;
    }

    public @NotNull GenericResponse joinRoom(WebSocket conn, String userId, Map<?, ?> msg) {
        if (!(msg.get("room") instanceof Double room)) {
            return new GenericResponse(false, "Field 'room' needs to be an instance of Double.");
        }

        int roomId = room.intValue();
        int perms = roomService.getUserPerms(roomId, userId);

        if (rooms.containsKey(roomId)) {
            rooms.get(roomId).put(conn, perms);
        } else {
            HashMap<WebSocket, Integer> innerMap = new HashMap<>();
            innerMap.put(conn, perms);
            rooms.put(roomId, innerMap);
        }
        return new GenericResponse(true, "Successfully joined a room.");
    }

    /* THIS IS WAITING FOR PERMISSION UPDATE
    public @NotNull MessageDispatch sendMessage(WebSocket conn, String userId, Map<?, ?> msg) {
        if (!(msg.get("room") instanceof Double room)) {
            return error(conn, false, "Field 'room' needs to be an instance of Double.");
        }
        if (!(msg.get("message") instanceof String message)) {
            return error(conn, false, "Field 'message' needs to be an instance of Double.");
        }
        int roomId = room.intValue();
        if (!rooms.containsKey(roomId) || !rooms.get(roomId).containsKey(conn)) {
            return error(conn, false, "You don't have access to this room.");
        }
        int perms = rooms.get(roomId).get(conn);
    }
    */

    private MessageDispatch error(WebSocket conn, boolean success, String message) {
        GenericResponse response = new GenericResponse(success, message);
        WebSocket[] recipient = new WebSocket[1];
        recipient[0] = conn;
        return new MessageDispatch(recipient, response);
    }
}
