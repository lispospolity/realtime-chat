package app.room;

import app.dto.Result;
import app.room.dto.response.*;
import app.dto.response.ErrorResponse;
import app.dto.response.GenericResponse;
import app.logging.Debug;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Service;
import java.sql.SQLException;
import java.sql.SQLTimeoutException;
import java.sql.SQLTransientConnectionException;
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
    public Result createRoom(@NotNull String requesterId) {
        int roomId;
        try {
            roomId = roomRepo.create(requesterId, System.currentTimeMillis());
            roomMemberRepo.addToRoom(roomId, requesterId, Permissions.getMaxPerms());
        } catch (SQLException ex) { return serverError(ex); }

        var response = new CreateRoomResponse(
                true,
                roomId,
                "Room successfully created.");
        return new Result(201, response);
    }

    public Result deleteRoom(@NotNull String requesterId, int roomId) {
        try {
            String ownerId = roomRepo.getOwner(roomId);
            if (ownerId == null || !ownerId.equals(requesterId)) {
                var response = new ErrorResponse(
                        false,
                        "This room either does not exist, or you are not the owner.");
                return new Result(404, response);
            }

            roomRepo.deleteRoom(roomId);

        } catch (SQLException ex) { return serverError(ex); }

        var response = new ErrorResponse(
                true,
                "Successfully deleted the room.");
        return new Result(200, response);
    }

    /**
     * newMemberId needs to be additionally authenticated before reaching this point
     */
    public Result addToRoom(@NotNull String requesterId, int roomId, @NotNull String newMemberId, int inputPermissions) {
        Result permsResult = hasPermissions(requesterId, roomId, Permissions.ADD_USERS);
        if (!(permsResult.response() instanceof
                HasPermissionsResponse(boolean success, String ownerId, Permissions requesterPerms, String message)))
            return permsResult;
        if (!success) return permsResult;

        boolean ownerRequest = ownerId.equals(requesterId);
        try {
            Permissions permissions = new Permissions(inputPermissions);
            if (!ownerRequest) permissions.crunchTo(requesterPerms);

            if (roomMemberRepo.userInRoom(roomId, newMemberId)) {
                var response = new ErrorResponse(
                        false,
                        "This user is already in the room.");
                return new Result(409, response);
            }

            roomMemberRepo.addToRoom(roomId, newMemberId, permissions.intValue());
        } catch (IllegalArgumentException ex) {
            var response = new ErrorResponse(
                    false,
                    "Permissions out of bounds.");
            return new Result(400, response);
        } catch (SQLException ex) { return serverError(ex); }


        var response = new GenericResponse(
                true,
                "Successfully added user to the room.");
        return new Result(201, response);
    }

    public Result updateMember(@NotNull String requesterId, int roomId, @NotNull String existingMemberId, int inputPermissions) {
        Result permsResult = hasPermissions(requesterId, roomId, Permissions.CHANGE_PERMISSIONS);
        if (!(permsResult.response() instanceof
                HasPermissionsResponse(boolean success, String ownerId, Permissions requesterPerms, String message)))
            return permsResult;
        if (!success) return permsResult;

        boolean ownerRequest = ownerId.equals(requesterId);
        try {
            if (!roomMemberRepo.userInRoom(roomId, existingMemberId)) {
                var response = new ErrorResponse(
                        false,
                        "This member is not in the room.");
                return new Result(404, response);
            }

            Permissions permissions = new Permissions(inputPermissions);
            if (!ownerRequest) permissions.crunchTo(requesterPerms);

            roomMemberRepo.changePerms(roomId, existingMemberId, permissions.intValue());
        } catch (IllegalArgumentException ex) {
            var response = new ErrorResponse(
                    false,
                    "Permissions out of bounds.");
            return new Result(400, response);
        } catch (SQLException ex) { return serverError(ex); }

        var response = new GenericResponse(
                true,
                "Successfully modified user permissions.");
        return new Result(200, response);
    }

    public Result removeMember(@NotNull String requesterId, int roomId, @NotNull String existingMemberId) {
        Result permsResult = hasPermissions(requesterId, roomId, Permissions.REMOVE_USERS);
        if (!(permsResult.response() instanceof
                HasPermissionsResponse(boolean success, String ownerId, Permissions requesterPerms, String message)))
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

        } catch (SQLException | IllegalArgumentException ex) { return serverError(ex); }
    }

    public Result getRoomMembers(@NotNull String requesterId, int roomId) {
        try {
            if (!roomRepo.roomExists(roomId)) {
                var response = new ErrorResponse(
                        false,
                        "This room does not exist");
                return new Result(404, response);
            }

            if (!roomMemberRepo.userInRoom(roomId, requesterId)) {
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
        } catch (SQLException ex) { return serverError(ex); }
    }

    public Result getUserPerms(@NotNull String requesterId, int roomId) {
        Permissions perms;
        try {
            if (!roomRepo.roomExists(roomId)) {
                var response = new ErrorResponse(
                        false,
                        "This room does not exist");
                return new Result(404, response);
            }

            if (!roomMemberRepo.userInRoom(roomId, requesterId)) {
                var response = new ErrorResponse(
                        false,
                        "You're not a member of this room.");
                return new Result(403, response);
            }

            if (requesterId.equals(roomRepo.getOwner(roomId)))
                perms = new Permissions(Permissions.getMaxPerms());
            else
                perms = new Permissions(roomMemberRepo.getUserPermissions(roomId, requesterId));

        } catch (SQLException ex) { return serverError(ex); }

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

        } catch (SQLException ex) { return serverError(ex); }
    }

    public Result getUserOwnedRooms(@NotNull String userId) {
        try {
            List<Integer> rooms = roomRepo.getUserOwnedRooms(userId);

            var response = new UserRoomsResponse(
                    true,
                    rooms,
                    "");
            return new Result(200, response);

        } catch (SQLException ex) { return serverError(ex); }
    }

    /**
     * Will return max permissions for the owner.
     * Best use case is inputting only one permission, because it will be checking all of them.
     */
    public Result hasPermissions(@NotNull String userId, int roomId, int permissions) {
        boolean isOwner = false;
        Permissions perms;
        try {
            String ownerId = roomRepo.getOwner(roomId);
            if (ownerId == null) return serverError(new NullPointerException("hasPermissions: For whatever reason a room said its owner is null."));
            if (ownerId.equals(userId)) isOwner = true;

            if (!roomRepo.roomExists(roomId)) {
                var response = new ErrorResponse(
                        false,
                        "This room does not exist.");
                return new Result(404, response);
            }

            if (!isOwner) perms = new Permissions(roomMemberRepo.getUserPermissions(roomId, userId));
            else perms = new Permissions(Permissions.getMaxPerms());

            if (!perms.allow(permissions) && !isOwner) {
                var response = new ErrorResponse(
                        false,
                        "You do not have the required permissions for this action.");
                return new Result(403, response);
            }

            var response = new HasPermissionsResponse(
                    true,
                    ownerId,
                    perms,
                    ""
            );
            return new Result(200, response);

        } catch (SQLException ex) { return serverError(ex); }
    }

    private Result serverError(Exception ex) {
        Debug.log("RoomService:generic serverError(): "+ex.toString());
        int httpCode = switch (ex) {
            case SQLTransientConnectionException e -> 503;
            case SQLTimeoutException e -> 504;
            case SQLException e -> 500;
            default -> 500;
        };

        var response = new ErrorResponse(
                false,
                "Internal Server Error");
        return new Result(httpCode, response);
    }
}
