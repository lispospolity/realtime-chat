package app.chat;

import app.dto.response.GenericResponse;
import app.room.Permissions;
import app.room.RoomService;
import app.websocket.dto.MessageDispatch;
import app.websocket.dto.MessageType;
import app.websocket.dto.OutgoingMessage;
import jakarta.validation.constraints.NotNull;
import org.java_websocket.WebSocket;
import org.springframework.stereotype.Service;

import java.util.Collection;
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
    private final HashMap<Integer, HashMap<WebSocket, Permissions>> rooms = new HashMap<>();
    private final RoomService roomService;

    public ChatService(RoomService roomService) {
        this.roomService = roomService;
    }

    public @NotNull GenericResponse joinRoom(WebSocket conn, String userId, Map<?, ?> msg) {
        if (!(msg.get("room") instanceof Double room)) {
            return new GenericResponse(false, "Field 'room' needs to be an instance of Double.");
        }

        int roomId = room.intValue();
        Permissions perms = roomService.getUserPerms(roomId, userId);

        if (rooms.containsKey(roomId)) {
            rooms.get(roomId).put(conn, perms);
        } else {
            HashMap<WebSocket, Permissions> innerMap = new HashMap<>();
            innerMap.put(conn, perms);
            rooms.put(roomId, innerMap);
        }
        return new GenericResponse(true, "Successfully joined a room.");
    }


    public @NotNull MessageDispatch<?> sendMessage(WebSocket conn, String userId, Map<?, ?> msg) {
        if (!(msg.get("room") instanceof Double roomIdDouble)) {
            return error(conn, false, "Field 'room' needs to be an instance of Double.");
        }
        if (!(msg.get("message") instanceof String message)) {
            return error(conn, false, "Field 'message' needs to be an instance of Double.");
        }
        if (message.isBlank()) return error(conn, false, "Message cannot be blank.");

        int roomId = roomIdDouble.intValue();
        if (!rooms.containsKey(roomId) || !rooms.get(roomId).containsKey(conn)) {
            return error(conn, false, "You don't have access to this room.");
        }
        HashMap<WebSocket, Permissions> room = rooms.get(roomId);
        Permissions perms = room.get(conn);
        if (!perms.letUser(Permissions.SEND_MESSAGES)) {
            return error(conn, false, "You cannot send messages in this channel.");
        }

        //put into the database WAITING FOR MESSAGE REPOSITORY AND MESSAGE SERVICE

        OutgoingMessage response = new OutgoingMessage(roomId, userId, message);

        Collection<WebSocket> roomMembers = rooms.get(roomId).keySet();
        WebSocket[] recipients = new WebSocket[roomMembers.size()];

        int i = 0;
        for (WebSocket recipient : roomMembers) {
            if (!room.get(recipient).letUser(Permissions.READ_MESSAGES)) {
                continue;
            }
            recipients[i] = recipient;
            i++;
        }

        return new MessageDispatch<OutgoingMessage>(recipients, response);
    }


    private MessageDispatch<GenericResponse> error(WebSocket conn, boolean success, String message) {
        GenericResponse response = new GenericResponse(success, message);
        WebSocket[] recipient = new WebSocket[1];
        recipient[0] = conn;
        return new MessageDispatch<GenericResponse>(recipient, response);
    }
}
