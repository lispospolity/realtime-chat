package app.room;

import app.auth.dto.Result;
import app.room.dto.UserPermsResponse;
import app.dto.response.ErrorResponse;
import app.dto.response.GenericResponse;
import app.logging.Debug;
import app.room.dto.CreateRoomResponse;
import app.room.dto.UserRoomsResponse;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Service;
import java.sql.SQLException;
import java.util.List;

@Service
public class RoomService {
    private final RoomRepository roomRepo;

    public RoomService(RoomRepository roomRepo) {
        this.roomRepo = roomRepo;
    }

    public Result createRoom(@NotNull String userId) {
        int roomId;
        try {
            roomId = roomRepo.create(userId, System.currentTimeMillis());
        } catch (SQLException ex) { return serverError(500, ex); }

        var response = new CreateRoomResponse(
                true,
                roomId,
                "Room successfully created.");
        return new Result(201, response);
    }

    public Result deleteRoom(@NotNull String userId, int roomId) {
        try {
            String ownerId = roomRepo.getOwner(roomId);
            if (ownerId == null || ownerId.equals(userId)) {
                var response = new ErrorResponse(
                        false,
                        "This room either does not exist, or you are not the owner.");
                return new Result(401, response);
            }

            roomRepo.deleteRoom(roomId);

        } catch (SQLException ex) { return serverError(500, ex); }

        var response = new ErrorResponse(
                true,
                "Successfully deleted the room.");
        return new Result(200, response);
    }

    /**
     * newMemberId needs to be authenticated before reaching this point
     */
    public Result addToRoom(@NotNull String userId, int roomId, String newMemberId, int permissions) {
        if (!Permissions.inBounds(permissions)) {
            var response = new ErrorResponse(
                    false,
                    "Permissions out of bounds.");
            return new Result(400, response);
        }

        try {
            if (!roomRepo.roomExists(roomId)) {
                var response = new ErrorResponse(
                        false,
                        "This room does not exist.");
                return new Result(400, response);
            }

            Permissions perms = new Permissions(roomRepo.getUserPermissions(roomId, userId));
            if (!perms.allow(Permissions.ADD_USERS)) {
                var response = new ErrorResponse(
                        false,
                        "You do not have the required permissions to add users.");
                return new Result(400, response);
            }

            if (roomRepo.userInRoom(roomId, newMemberId)) {
                var response = new ErrorResponse(
                        false,
                        "This user is already in the room.");
                return new Result(400, response);
            }

            roomRepo.addToRoom(roomId, newMemberId, permissions);
        } catch (SQLException ex) { return serverError(500, ex); }

        var response = new GenericResponse(
                true,
                "Successfully added user to the room.");
        return new Result(201, response);
    }

    public Result getUserPerms(@NotNull String userId, int roomId) {
        Permissions perms;
        try {
            if (!roomRepo.roomExists(roomId)) {
                var response = new ErrorResponse(
                        false,
                        "This room does not exist.");
                return new Result(400, response);
            }

            if (!roomRepo.userInRoom(roomId, userId)) {
                var response = new ErrorResponse(
                        false,
                        "You are not a member of this room.");
                return new Result(401, response);
            }

            if (userId.equals(roomRepo.getOwner(roomId)))
                perms = new Permissions(Permissions.getMaxPerms());
            else
                perms = new Permissions(roomRepo.getUserPermissions(roomId, userId));

        } catch (SQLException ex) {
            Debug.log("RoomService: getUserPerms: "+ex);
            return serverError(500, ex);
        }

        var response = new UserPermsResponse(
                true,
                perms,
                "");
        return new Result(200, response);
    }

    public Result getUserRooms(@NotNull String userId) {
        try {
            List<Integer> rooms = roomRepo.getUserRooms(userId);

            var response = new UserRoomsResponse(
                    true,
                    rooms,
                    "");
            return new Result(200, response);

        } catch (SQLException ex) { return serverError(500, ex); }
    }

    public Result getUserOwnedRooms(@NotNull String userId) {
        try {
            List<Integer> rooms = roomRepo.getUserOwnedRooms(userId);

            var response = new UserRoomsResponse(
                    true,
                    rooms,
                    "");
            return new Result(200, response);

        } catch (SQLException ex) { return serverError(500, ex); }
    }

    private Result serverError(int httpCode, Exception ex) {
        Debug.log("RoomService:generic serverError(): "+ex.toString());

        var response = new ErrorResponse(
                false,
                "Internal Server Error");
        return new Result(httpCode, response);
    }
}
