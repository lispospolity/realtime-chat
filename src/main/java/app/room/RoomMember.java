package app.room;

import jakarta.validation.constraints.NotNull;

import java.util.Objects;

public class RoomMember {
    private final String memberId;
    public String getMemberId() {
        return memberId;
    }

    private Permissions permissions;
    public Permissions getPermissions() {
        return permissions;
    }

    public RoomMember(@NotNull String memberId, @NotNull Permissions permissions) {
        this.memberId = memberId;
        this.permissions = permissions;
    }

    public void overwritePerms(@NotNull Permissions newPerms) {
        this.permissions = newPerms;
    }

    public static RoomMember createOwner(@NotNull String ownerId) {
        return new RoomMember(ownerId, new Permissions(Permissions.getMaxPerms()));
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof RoomMember that)) return false;
        return Objects.equals(memberId, that.memberId) && Objects.equals(permissions, that.permissions);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(memberId);
    }
}