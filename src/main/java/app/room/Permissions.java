package app.room;

public class Permissions {
    public static final int READ_MESSAGES = 1<<0;
    public static final int SEND_MESSAGES = 1<<1;
    public static final int DELETE_MESSAGES = 1<<2;
    public static final int ADD_USERS = 1<<3;
    public static final int REMOVE_USERS = 1<<4;
    public static final int CHANGE_PERMISSIONS = 1<<5;

    private int permissions;
    public Permissions(int perms) {
        this.permissions = perms;
    }

    public boolean letUser(int desiredPerm) {
        return (permissions & desiredPerm) != 0;
    }

    public int getOwnerPerms() {
        return READ_MESSAGES
                |SEND_MESSAGES
                |DELETE_MESSAGES
                |ADD_USERS
                |REMOVE_USERS
                |CHANGE_PERMISSIONS;
    }
}
