package app.room;

public enum Permissions {
    READ_MESSAGES(1<<0),
    SEND_MESSAGES(1<<1),
    DELETE_MESSAGES(1<<2),
    ADD_USERS(1<<3),
    REMOVE_USERS(1<<4),
    CHANGE_PERMISSIONS(1<<5);

    private final int permission;

    public int getOwnerPerms() {
        return READ_MESSAGES.permission
                | SEND_MESSAGES.permission
                | DELETE_MESSAGES.permission
                | ADD_USERS.permission
                | REMOVE_USERS.permission
                | CHANGE_PERMISSIONS.permission;
    }

    public int getPermission() { return this.permission; }

    Permissions(int perm) { this.permission = perm; }
}
