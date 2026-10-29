package app.chat;

import app.dto.response.GenericResponse;
import app.room.Permissions;
import app.room.RoomService;
import app.websocket.Session;
import app.websocket.dto.MessageDispatch;
import app.websocket.dto.response.OutgoingMessage;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ChatService {
    private final HashMap<Integer, HashMap<Session, Permissions>> rooms = new HashMap<>();
    private final RoomService roomService;

    public ChatService(RoomService roomService) {
        this.roomService = roomService;
    }

    public @NotNull GenericResponse joinRoom(Session session, Map<?, ?> msg) {
        if (!(msg.get("room") instanceof Double room)) {
            return new GenericResponse(false, "Field 'room' needs to be an instance of Double.");
        }

        int roomId = room.intValue();
        Permissions perms = roomService.getUserPerms(roomId, session.userId());

        if (rooms.containsKey(roomId)) {
            rooms.get(roomId).put(session, perms);
        } else {
            HashMap<Session, Permissions> innerMap = new HashMap<>();
            innerMap.put(session, perms);
            rooms.put(roomId, innerMap);
        }
        return new GenericResponse(true, "Successfully joined a room.");
    }


    public @NotNull MessageDispatch<?> sendMessage(Session session, Map<?, ?> msg) {
        if (!(msg.get("room") instanceof Double roomIdDouble)) {
            return error(session, false, "Field 'room' needs to be an instance of Double.");
        }
        if (!(msg.get("message") instanceof String message)) {
            return error(session, false, "Field 'message' needs to be an instance of String.");
        }
        if (message.isBlank()) return error(session, false, "Message cannot be blank.");

        int roomId = roomIdDouble.intValue();
        if (!rooms.containsKey(roomId) || !rooms.get(roomId).containsKey(session)) {
            return error(session, false, "You don't have access to this room.");
        }
        HashMap<Session, Permissions> room = rooms.get(roomId);
        Permissions perms = room.get(session);
        if (!perms.allow(Permissions.SEND_MESSAGES)) {
            return error(session, false, "You cannot send messages in this channel.");
        }

        //put into the database WAITING FOR MESSAGE REPOSITORY AND MESSAGE SERVICE

        OutgoingMessage response = new OutgoingMessage(roomId, session.userId(), message);

        Collection<Session> roomMembers = rooms.get(roomId).keySet();
        List<Session> recipients = new ArrayList<>();

        for (Session recipient : roomMembers) {
            if (!room.get(recipient).allow(Permissions.READ_MESSAGES)) {
                continue;
            }
            recipients.add(recipient);
        }

        return new MessageDispatch<OutgoingMessage>(recipients, response);
    }

    public void userLeft(Session session) {
        for (HashMap<Session, Permissions> room : rooms.values()) {
            room.remove(session);
        }
    }


    private MessageDispatch<GenericResponse> error(Session session, boolean success, String message) {
        GenericResponse response = new GenericResponse(success, message);
        List<Session> recipient = new ArrayList<>();
        recipient.add(session);
        return new MessageDispatch<GenericResponse>(recipient, response);
    }
}
