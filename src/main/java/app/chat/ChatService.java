package app.chat;

import app.dto.response.ErrorResponse;
import app.dto.response.GenericResponse;
import app.dto.response.Response;
import app.room.Permissions;
import app.room.RoomService;
import app.room.dto.response.HasPermissionsResponse;
import app.room.dto.response.RoomMembersResponse;
import app.websocket.dto.MessageDispatch;
import app.websocket.dto.response.OutgoingMessage;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ChatService {
    private final RoomService roomService;

    public ChatService(RoomService roomService) {
        this.roomService = roomService;
    }

    //TODO figure out how to build this so its maintainable, maybe build messageService first
    public @NotNull MessageDispatch<?> sendMessage(String userId, int roomId, String messageId, String message) {
        return toSender(false, "Message under construction.");
        /*
        Response mResponse = roomService.getRoomMembers(userId, roomId).response();
        if (!(mResponse instanceof RoomMembersResponse membersResponse)) {

        }

        Response pResponse = roomService.hasPermissions(userId, roomId, Permissions.SEND_MESSAGES).response();


        if (!(pResponse instanceof HasPermissionsResponse permsResponse)) {
            if (pResponse instanceof ErrorResponse(boolean success, String error)) {
                return toSender(success, error);
            }
            return toSender(false, "Something is not yes");
        }


        if (message == null || message.isBlank()) {
            return toSender(false, "OutgoingMessage cannot be empty.");
        } //this should live in messageService

        if (messageId == null || messageId.isBlank()) {
            return toSender(false, "MessageId cannot be empty.");
        } //this should live in messageService

        //messageId also cannot be null, or blank, its user supplied
        //check if messageId is in the room in the database
        //put into the database WAITING FOR MESSAGE REPOSITORY AND MESSAGE SERVICE



        OutgoingMessage msg = new OutgoingMessage(roomId, userId, message, messageId);

        GenericResponse endResponse = new GenericResponse(true, "OutgoingMessage successfully sent.");

        return new MessageDispatch<OutgoingMessage>(recipients, msg, endResponse);
        */
    }

    private MessageDispatch<Void> toSender(boolean success, String message) {
        return new MessageDispatch<Void>(null, null, new ErrorResponse(success, message));
    }
}
