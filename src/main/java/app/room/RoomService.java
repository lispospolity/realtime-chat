package app.room;

import app.auth.dto.Result;
import app.dto.response.ErrorResponse;
import app.dto.response.GenericResponse;
import app.logging.Debug;
import app.room.dto.CreateRoomResponse;
import org.springframework.stereotype.Service;
import java.sql.SQLException;

@Service
public class RoomService {
    private final RoomRepository roomRepo;

    public RoomService(RoomRepository roomRepo) {
        this.roomRepo = roomRepo;
    }

    public Result<?> createRoom(String userId) {
        int roomId;
        try {
            roomId = roomRepo.create(userId, System.currentTimeMillis());
        } catch (SQLException ex) { return serverError(500, ex); }

        var response = new CreateRoomResponse(
                true,
                roomId,
                "Room successfully created.");
        return new Result<CreateRoomResponse>(201, response);
    }

    public Result<?> deleteRoom(String userId, int roomId) {
        try {

            String ownerId = roomRepo.getOwner(roomId);
            if (ownerId == null || ownerId.equals(userId)) {
                var response = new ErrorResponse(
                        false,
                        "This room either does not exist, or you are not the owner.");
                return new Result<ErrorResponse>(401, response);
            }

            roomRepo.deleteRoom(roomId);

        } catch (SQLException ex) { return serverError(500, ex); }

        var response = new ErrorResponse(
                true,
                "Successfully deleted the room.");
        return new Result<ErrorResponse>(200, response);
    }

    public Result<?> addToRoom(String userId, int roomId, String newMemberId, int permissions) {
        if (Permissions.inBounds(permissions)) {
            var response = new ErrorResponse(
                    false,
                    "Permissions out of bounds.");
            return new Result<ErrorResponse>(400, response);
        }

        try {
            if (!roomRepo.roomExists(roomId)) {
                var response = new ErrorResponse(
                        false,
                        "This room does not exist.");
                return new Result<ErrorResponse>(400, response);
            }

            Permissions perms = new Permissions(roomRepo.getUserPermissions(roomId, userId));
            if (!perms.allow(Permissions.ADD_USERS)) {
                var response = new ErrorResponse(
                        false,
                        "You do not have the required permissions to add users.");
                return new Result<ErrorResponse>(400, response);
            }

            roomRepo.addToRoom(roomId, newMemberId, permissions);
        } catch (SQLException ex) { return serverError(500, ex); }

        var response = new GenericResponse(
                true,
                "Successfully added user to the room.");
        return new Result<GenericResponse>(201, response);
    }

    public Permissions getUserPerms(int roomId, String userId) {
        try {
            if (userId.equals(roomRepo.getOwner(roomId)))
                return new Permissions(Permissions.getOwnerPerms());
            else
                return new Permissions(roomRepo.getUserPermissions(roomId, userId));

        } catch (SQLException ex) {
            Debug.log("RoomService: getUserPerms: "+ex);
            return new Permissions(0);
        }
    }

    private Result<?> serverError(int httpCode, Exception ex) {
        Debug.log("RoomService:generic serverError(): "+ex.toString());

        var response = new ErrorResponse(
                false,
                "Internal Server Error");
        return new Result<ErrorResponse>(httpCode, response);
    }
}
