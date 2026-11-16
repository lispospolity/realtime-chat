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

    public int createRoom(String requesterId, boolean e2ee) throws SQLException {
        int roomId = roomRepo.create(requesterId, System.currentTimeMillis(), e2ee);
        roomMemberRepo.addToRoom(roomId, requesterId, Permissions.getMaxPerms());

        cacheNewRoom(roomId, requesterId, e2ee);
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

    public void updateMemberPerms(Room room, RoomMember member, Permissions newPerms) throws SQLException {
        roomMemberRepo.changePerms(room.getRoomId(), member.getMemberId(), newPerms.intValue());

        member.overwritePerms(newPerms);
    }

    public void removeMember(Room room, RoomMember member) throws SQLException {
        roomMemberRepo.removeFromRoom(room.getRoomId(), member.getMemberId());

        room.removeMember(member);
    }

    /**
     * @return Null only when room is nonexistent (checks both cache and db)
     */
    public @Nullable Room getRoomById(int roomId) throws SQLException {
        Room room = roomsById.get(roomId);
        if (room != null) return room;

        Room dbRoom = pullRoomFromDatabase(roomId);
        if (dbRoom == null) return null;

        return cacheRoom(dbRoom);
    }

    private @NotNull Room cacheRoom(Room room) {
        roomsById.put(room.getRoomId(), room);
        roomsWithOwnerId.put(room, room.getOwner());

        return room;
    }

    private @NotNull Room cacheNewRoom(int roomId, String owner, boolean e2ee) {
        Room newRoomObj = new Room(roomId, owner, e2ee);
        roomsById.put(roomId, newRoomObj);
        roomsWithOwnerId.put(newRoomObj, owner);

        return newRoomObj;
    }

    private @Nullable Room pullRoomFromDatabase(@NotNull int roomId) throws SQLException {
        return roomRepo.getRoom(roomId);
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
        Integer perms = roomMemberRepo.getUserPermissions(room.getRoomId(), userId);
        if (perms == null) return null;

        return new RoomMember(userId, new Permissions(perms));
    }

    public List<RoomMember> getRoomMembers(int roomId) throws SQLException {
        Room room = getRoomById(roomId);
        if (room == null) return null;

        if (!room.membersCacheExpired()) return room.getMembers();

        List<RoomMember> dbMembers = pullRoomMembersFromDatabase(room);
        return overwriteRoomMemberCache(room, dbMembers);
    }

    private List<RoomMember> overwriteRoomMemberCache(@NotNull Room room, List<RoomMember> newMemberList) {
        room.overwriteMembers(newMemberList);
        return newMemberList;
    }

    private List<RoomMember> pullRoomMembersFromDatabase(@NotNull Room room) throws SQLException {
        return roomMemberRepo.getRoomMembers(room.getRoomId());
    }
}
