package app.room;

import jakarta.validation.constraints.NotNull;

import java.util.Objects;

public class RoomMember {
    private final String memberId;
    public String getMemberId() {
        return memberId;
    }

    private final Permissions perms;
    public Permissions getPermissions() {
        return perms;
    }

    public RoomMember(@NotNull String memberId, @NotNull Permissions permissions) {
        this.memberId = memberId;
        this.perms = permissions;
    }

    public static RoomMember createOwner(@NotNull String ownerId) {
        return new RoomMember(ownerId, new Permissions(Permissions.getMaxPerms()));
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof RoomMember that)) return false;
        return Objects.equals(memberId, that.memberId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(memberId);
    }
}