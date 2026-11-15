package app.room;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;

import java.util.*;

public final class Room {
    private final int roomId;
    public int getRoomId() { return roomId; }

    private final String owner;
    public String getOwner() { return owner; }

    private final HashMap<String, RoomMember> membersById;

    private static long membersCacheTtlMillis = 300_000L;
    public static void setCacheExpiryTime(long cacheExpiryTime) {
        Room.membersCacheTtlMillis = cacheExpiryTime;
    }

    private long membersCacheExpiresAt = 0;
    public boolean membersCacheExpired() { return membersCacheExpiresAt < System.currentTimeMillis(); }

    public Room(int roomId, @NotNull String creatorId) {
        this.roomId = roomId;
        this.membersById = new HashMap<>();
        this.owner = creatorId;
        membersById.put(creatorId, RoomMember.createOwner(creatorId));
    }

    public void addMember(@NotNull RoomMember member) {
        membersById.put(member.getMemberId(), member);
    }

    public boolean containsMemberId(@NotNull String userId) {
        return membersById.containsKey(userId);
    }

    public @Nullable RoomMember getMemberById(@NotNull String userId) {
        return membersById.get(userId);
    }

    public void removeMember(@NotNull RoomMember member) {
        membersById.remove(member.getMemberId(), member);
    }

    public @NotNull List<RoomMember> getMembersWithPermissions(int permission) {
        List<RoomMember> members = getMembers();
        List<RoomMember> permitted = new ArrayList<>();

        for (RoomMember member : members) {
            if (!member.getPermissions().allow(permission)) continue;
            permitted.add(member);
        }
        return permitted;
    }

    public void overwriteMembers(@NotNull List<RoomMember> members) {
        RoomMember ownerObj = getMemberById(owner);
        membersById.clear();
        for (RoomMember member : members) {
            membersById.put(member.getMemberId(), member);
        }

        if (ownerObj == null) return;
        membersById.put(owner, ownerObj);

        membersCacheExpiresAt = System.currentTimeMillis() + membersCacheTtlMillis;
    }

    public @NotNull List<RoomMember> getMembers() {
        return new ArrayList<>(membersById.values());
    }

    public int amountOfMembers() {
        return membersById.size();
    }

    public void dispose() {
        membersById.clear();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Room room)) return false;
        return roomId == room.roomId && Objects.equals(owner, room.owner);
    }

    @Override
    public int hashCode() {
        return Objects.hash(roomId, owner);
    }
}
