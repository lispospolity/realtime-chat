package app.room;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public final class Room {
    private final int roomId;
    public int getRoomId() { return roomId; }

    private final String owner;
    public String getOwner() { return owner; }

    private final HashMap<String, RoomMember> membersById;

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

    public @NotNull Set<RoomMember> getMembersWithPermissions(int permission) {
        Set<RoomMember> members = getMembers();
        Set<RoomMember> permitted = new HashSet<>();

        for (RoomMember member : members) {
            if (!member.getPermissions().allow(permission)) continue;
            permitted.add(member);
        }
        return permitted;
    }

    public void populateMembers(@NotNull Set<RoomMember> members) {
        membersById.clear();
        for (RoomMember member : members) {
            membersById.put(member.getMemberId(), member);
        }
    }

    public @NotNull Set<RoomMember> getMembers() {
        return new HashSet<>(membersById.values());
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
