package app.room;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Component;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;

@Component
public class RoomRegistry {
    private final RoomRepository roomRepo;
    private final RoomMemberRepository roomMemberRepo;

    private final HashMap<Integer, Room> roomsById;
    private final HashMap<Room, String> roomsWithOwnerId;

    public RoomRegistry(RoomRepository roomRepo, RoomMemberRepository roomMemberRepo) {
        this.roomRepo = roomRepo;
        this.roomMemberRepo = roomMemberRepo;
        roomsById = new HashMap<>();
        roomsWithOwnerId = new HashMap<>();
    }

    public int createRoom(String requesterId) throws SQLException {
        int roomId = roomRepo.create(requesterId, System.currentTimeMillis());
        roomMemberRepo.addToRoom(roomId, requesterId, Permissions.getMaxPerms());

        cacheNewRoom(roomId, requesterId);
        return roomId;
    }

    public boolean isOwner(int roomId, @Nullable String ownerId) throws SQLException {
        if (ownerId == null || ownerId.isBlank()) return false;

        Room room = getRoomById(roomId);
        if (room == null) return false;

        return ownerId.equals(room.getOwner());
    }

    public void removeRoom(int roomId) throws SQLException {
        roomRepo.deleteRoom(roomId);

        Room room = roomsById.remove(roomId);
        if (room == null) return;

        roomsWithOwnerId.remove(room, room.getOwner());
        room.dispose();
    }

    public void createMember(int roomId, String memberId, Permissions perms) throws SQLException {
        Room room = getRoomById(roomId);
        if (room == null) return;

        roomMemberRepo.addToRoom(roomId, memberId, perms.intValue());

        cacheNewMember(room, new RoomMember(memberId, perms));
    }

    /**
     * @return Null only when room is nonexistent (checks both cache and db)
     */
    public @Nullable Room getRoomById(int roomId) throws SQLException {
        Room room = roomsById.get(roomId);
        if (room != null) return room;

        Room dbRoom = pullRoomFromDatabase(roomId);
        if (dbRoom == null) return null;

        return cacheNewRoom(roomId, dbRoom.getOwner());
    }

    private @NotNull Room cacheNewRoom(int roomId, String owner) {
        Room newRoomObj = new Room(roomId, owner);
        roomsById.put(roomId, newRoomObj);
        roomsWithOwnerId.put(newRoomObj, owner);

        return newRoomObj;
    }

    private @Nullable Room pullRoomFromDatabase(@NotNull int roomId) throws SQLException {
        String owner = roomRepo.getOwner(roomId);
        if (owner == null) return null;

        return new Room(roomId, owner);
    }

    /**
     * @return Null when room is nonexistent, or user is not in the room
     */
    public @Nullable RoomMember getRoomMember(int roomId, @NotNull String userId) throws SQLException {
        Room room = getRoomById(roomId);
        if (room == null) return null;

        RoomMember member = room.getMemberById(userId);
        if (member != null) return member;

        RoomMember dbMember = pullMemberFromDatabase(room, userId);
        if (dbMember == null) return null;

        return cacheNewMember(room, dbMember);
    }

    private @NotNull RoomMember cacheNewMember(@NotNull Room room, @NotNull RoomMember newMember) {
        room.addMember(newMember);
        return newMember;
    }

    private @Nullable RoomMember pullMemberFromDatabase(@NotNull Room room, @NotNull String userId) throws SQLException {
        if (!roomMemberRepo.userInRoom(room.getRoomId(), userId)) return null;
        int perms = roomMemberRepo.getUserPermissions(room.getRoomId(), userId);

        return new RoomMember(userId, new Permissions(perms));
    }

    public void populateRoomsCache(List<Room> rooms) {
        for (Room room : rooms) {
            roomsById.put(room.getRoomId(), room);
        }
    }
}
