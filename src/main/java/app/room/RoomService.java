package app.room;

import app.dto.Result;
import app.room.dto.response.*;
import app.dto.response.ErrorResponse;
import app.dto.response.GenericResponse;
import app.logging.Debug;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Service;
import java.sql.SQLException;
import java.util.List;

@Service
public class RoomService {
    private final RoomRepository roomRepo;
    private final RoomMemberRepository roomMemberRepo;

    public RoomService(RoomRepository roomRepo, RoomMemberRepository roomMemberRepo) {
        this.roomRepo = roomRepo;
        this.roomMemberRepo = roomMemberRepo;
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
            roomMemberRepo.addToRoom(roomId, userId, Permissions.getMaxPerms());
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
        Result permsResult = hasPermissions(userId, roomId, Permissions.ADD_USERS);
        if (!(permsResult.response() instanceof
                HasPermissionsResponse(boolean success, boolean isOwner, Permissions userPerms, String message)))
            return permsResult;
        if (!success) return permsResult;

        try {
            Permissions newMemberPermissions = new Permissions(permissions);
            if (!isOwner) newMemberPermissions.crunchTo(userPerms);

            if (roomMemberRepo.userInRoom(roomId, newMemberId)) {
                var response = new ErrorResponse(
                        false,
                        "This user is already in the room.");
                return new Result(409, response);
            }

            roomMemberRepo.addToRoom(roomId, newMemberId, permissions);
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

    public Result updateMember(@NotNull String userId, int roomId, @NotNull String existingMemberId, int newPermissions) {
        Result permsResult = hasPermissions(userId, roomId, Permissions.CHANGE_PERMISSIONS);
        if (!(permsResult.response() instanceof
                HasPermissionsResponse(boolean success, boolean isOwner, Permissions requesterPerms, String message)))
            return permsResult;
        if (!success) return permsResult;

        try {
            if (!roomMemberRepo.userInRoom(roomId, existingMemberId)) {
                var response = new ErrorResponse(
                        false,
                        "This member is not in the room.");
                return new Result(404, response);
            }

            Permissions newPerms = new Permissions(newPermissions);
            newPerms.crunchTo(requesterPerms);

            roomMemberRepo.changePerms(roomId, existingMemberId, newPerms.intValue());
        } catch (IllegalArgumentException ex) {
            var response = new ErrorResponse(
                    false,
                    "Permissions out of bounds.");
            return new Result(400, response);
        } catch (SQLException ex) { return serverError(500, ex); }

        var response = new GenericResponse(
                true,
                "Successfully modified user permissions.");
        return new Result(200, response);
    }

    public Result removeMember(@NotNull String userId, int roomId, @NotNull String existingMemberId) {
        Result permsResult = hasPermissions(userId, roomId, Permissions.REMOVE_USERS);
        if (!(permsResult.response() instanceof
                HasPermissionsResponse(boolean success, boolean isOwner, Permissions requesterPerms, String message)))
            return permsResult;
        if (!success) return permsResult;

        try {
            if (!roomMemberRepo.userInRoom(roomId, existingMemberId)) {
                var response = new ErrorResponse(
                        false,
                        "This member is not in the room.");
                return new Result(404, response);
            }

            roomMemberRepo.removeFromRoom(roomId, existingMemberId);

            var response = new GenericResponse(
                    true,
                    "Successfully removed the user from the room.");
            return new Result(200, response);

        } catch (SQLException | IllegalArgumentException ex) { return serverError(500, ex); }
    }

    public Result getRoomMembers(@NotNull String userId, int roomId) {
        try {
            if (!roomRepo.roomExists(roomId)) {
                var response = new ErrorResponse(
                        false,
                        "This room does not exist");
                return new Result(404, response);
            }

            if (!roomMemberRepo.userInRoom(roomId, userId)) {
                var response = new ErrorResponse(
                        false,
                        "You're not a member of this room.");
                return new Result(403, response);
            }

            List<String> members = roomMemberRepo.getRoomMembers(roomId);

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

            if (!roomMemberRepo.userInRoom(roomId, userId)) {
                var response = new ErrorResponse(
                        false,
                        "You're not a member of this room.");
                return new Result(403, response);
            }

            if (userId.equals(roomRepo.getOwner(roomId)))
                perms = new Permissions(Permissions.getMaxPerms());
            else
                perms = new Permissions(roomMemberRepo.getUserPermissions(roomId, userId));

        } catch (SQLException ex) { return serverError(500, ex); }

        var response = new UserPermsResponse(
                true,
                perms,
                "");
        return new Result(200, response);
    }

    public Result getUserRooms(@NotNull String userId) {
        try {
            List<Integer> rooms = roomMemberRepo.getUserRooms(userId);

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

    /**
     * Best use case is inputting only one permission, because it will be checking all of them.
     */
    public Result hasPermissions(@NotNull String userId, int roomId, int permissions) {
        boolean isOwner = false;
        Permissions perms;
        try {
            String ownerId = roomRepo.getOwner(roomId);
            if (ownerId != null && ownerId.equals(userId)) isOwner = true;

            if (!roomRepo.roomExists(roomId)) {
                var response = new ErrorResponse(
                        false,
                        "This room does not exist.");
                return new Result(404, response);
            }

            perms = new Permissions(roomMemberRepo.getUserPermissions(roomId, userId));
            if (!perms.allow(permissions) && !isOwner) {
                var response = new ErrorResponse(
                        false,
                        "You do not have the required permissions for this action.");
                return new Result(403, response);
            }

            var response = new HasPermissionsResponse(
                    true,
                    isOwner,
                    perms,
                    ""
            );
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
