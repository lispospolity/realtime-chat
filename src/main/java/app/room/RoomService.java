package app.room;

import app.dto.Result;
import app.room.dto.RoomMembersResponse;
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

    /**
     * Creating rooms adds 2 records to tables:
     * 1. rooms (obviously),
     * 2. room_members as a member with all privileges
     */
    public Result createRoom(@NotNull String userId) {
        int roomId;
        try {
            roomId = roomRepo.create(userId, System.currentTimeMillis());
            roomRepo.addToRoom(roomId, userId, Permissions.getMaxPerms());
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
            if (ownerId == null || !ownerId.equals(userId)) {
                var response = new ErrorResponse(
                        false,
                        "This room either does not exist, or you are not the owner.");
                return new Result(404, response);
            }

            roomRepo.deleteRoom(roomId);

        } catch (SQLException ex) { return serverError(500, ex); }

        var response = new ErrorResponse(
                true,
                "Successfully deleted the room.");
        return new Result(200, response);
    }

    /**
     * newMemberId needs to be additionally authenticated before reaching this point
     */
    public Result addToRoom(@NotNull String userId, int roomId, @NotNull String newMemberId, int permissions) {
        try {
            if (!roomRepo.roomExists(roomId)) {
                var response = new ErrorResponse(
                        false,
                        "This room does not exist.");
                return new Result(404, response);
            }

            Permissions perms = new Permissions(roomRepo.getUserPermissions(roomId, userId));
            if (!perms.allow(Permissions.ADD_USERS)) {
                var response = new ErrorResponse(
                        false,
                        "You do not have the required permissions to add users.");
                return new Result(403, response);
            }

            Permissions newMemberPermissions = new Permissions(permissions);
            newMemberPermissions.crunchTo(perms);

            if (roomRepo.userInRoom(roomId, newMemberId)) {
                var response = new ErrorResponse(
                        false,
                        "This user is already in the room.");
                return new Result(409, response);
            }

            roomRepo.addToRoom(roomId, newMemberId, permissions);
        } catch (IllegalArgumentException ex) {
            var response = new ErrorResponse(
                    false,
                    "Permissions out of bounds.");
            return new Result(400, response);
        } catch (SQLException ex) { return serverError(500, ex); }


        var response = new GenericResponse(
                true,
                "Successfully added user to the room.");
        return new Result(201, response);
    }

    public Result getRoomMembers(@NotNull String userId, int roomId) {
        try {
            if (!roomRepo.roomExists(roomId)) {
                var response = new ErrorResponse(
                        false,
                        "This room does not exist");
                return new Result(404, response);
            }

            if (!roomRepo.userInRoom(roomId, userId)) {
                var response = new ErrorResponse(
                        false,
                        "You're not a member of this room.");
                return new Result(403, response);
            }

            List<String> members = roomRepo.getRoomMembers(roomId);

            var response = new RoomMembersResponse(
                    true,
                    members,
                    "Successfully fetched room members."
            );
            return new Result(200, response);
        } catch (SQLException ex) { return serverError(500, ex); }
    }

    public Result getUserPerms(@NotNull String userId, int roomId) {
        Permissions perms;
        try {
            if (!roomRepo.roomExists(roomId)) {
                var response = new ErrorResponse(
                        false,
                        "This room does not exist");
                return new Result(404, response);
            }

            if (!roomRepo.userInRoom(roomId, userId)) {
                var response = new ErrorResponse(
                        false,
                        "You're not a member of this room.");
                return new Result(403, response);
            }

            if (userId.equals(roomRepo.getOwner(roomId)))
                perms = new Permissions(Permissions.getMaxPerms());
            else
                perms = new Permissions(roomRepo.getUserPermissions(roomId, userId));

        } catch (SQLException ex) { return serverError(500, ex); }

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
