package app.chat;

import app.dto.response.ErrorResponse;
import app.dto.response.GenericResponse;
import app.dto.response.Response;
import app.room.Permissions;
import app.room.RoomService;
import app.room.dto.UserPermsResponse;
import app.websocket.dto.MessageDispatch;
import app.websocket.dto.response.OutgoingMessage;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ChatService {
    private final HashMap<Integer, HashMap<String, Permissions>> rooms = new HashMap<>();
    private final RoomService roomService;

    public ChatService(RoomService roomService) {
        this.roomService = roomService;
    }

    public @NotNull Response joinRoom(String userId, int roomId) {
        Response response = roomService.getUserPerms(userId, roomId).response();
        if (!(response instanceof UserPermsResponse(boolean success, Permissions perms, String message)))
            return response;

        if (!success)
            return response;

        if (rooms.containsKey(roomId) && rooms.get(roomId).containsKey(userId)) {
            return new ErrorResponse(false, "You are already in this room!");
        } else if (rooms.containsKey(roomId)) {
            rooms.get(roomId).put(userId, perms);
        }
        else {
            HashMap<String, Permissions> innerMap = new HashMap<>();
            innerMap.put(userId, perms);

            rooms.put(roomId, innerMap);
        }
        return new GenericResponse(true, "Successfully connected to the room.");
    }


    public @NotNull MessageDispatch<?, ?> sendMessage(String userId, int roomId, String messageId, String message) {
        if (message == null || message.isBlank()) {
            return toSender(false, "Message cannot be empty.");
        }

        if (messageId == null || messageId.isBlank()) {
            return toSender(false, "MessageId cannot be empty.");
        }

        if (!rooms.containsKey(roomId) || !rooms.get(roomId).containsKey(userId)) {
            return toSender(false, "You don't have access to this room.");
        }
        HashMap<String, Permissions> room = rooms.get(roomId);
        Permissions perms = room.get(userId);
        if (!perms.allow(Permissions.SEND_MESSAGES)) {
            return toSender(false, "You cannot send messages in this channel.");
        }

        //messageId also cannot be null, or blank, its user supplied
        //check if messageId is in the room in the database
        //put into the database WAITING FOR MESSAGE REPOSITORY AND MESSAGE SERVICE

        OutgoingMessage msg = new OutgoingMessage(roomId, userId, message);

        Collection<String> roomMembers = rooms.get(roomId).keySet();
        List<String> recipients = new ArrayList<>();

        for (String recipientId : roomMembers) {
            if (!room.get(recipientId).allow(Permissions.READ_MESSAGES)) {
                continue;
            }
            recipients.add(recipientId);
        }

        GenericResponse response = new GenericResponse(true, "Message successfully sent.");

        return new MessageDispatch<OutgoingMessage, GenericResponse>(recipients, msg, response);
    }

    public void userLeft(String userId) {
        for (HashMap<String, Permissions> room : rooms.values()) {
            room.remove(userId);
        }
    }


    private MessageDispatch<Void, ErrorResponse> toSender(boolean success, String message) {
        return new MessageDispatch<Void, ErrorResponse>(null, null, new ErrorResponse(success, message));
    }
}
