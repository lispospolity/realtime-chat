package app.chat;

import app.dto.response.GenericResponse;
import app.room.RoomService;
import jakarta.validation.constraints.NotNull;
import org.java_websocket.WebSocket;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class ChatService {
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
}
