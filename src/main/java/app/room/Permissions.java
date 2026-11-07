package app.room;

public class Permissions {
    public static final int READ_MESSAGES = 1<<0;
    public static final int SEND_MESSAGES = 1<<1;
    public static final int DELETE_MESSAGES = 1<<2;
    public static final int ADD_USERS = 1<<3;
    public static final int REMOVE_USERS = 1<<4;
    public static final int CHANGE_PERMISSIONS = 1<<5;

    private int permissions;
    public int intValue() { return permissions; }

    public Permissions(int perms) throws IllegalArgumentException {
        if (!inBounds(perms)) throw new IllegalArgumentException("Permissions out of bounds.");
        this.permissions = perms;
    }

    public boolean allow(int desiredPerm) {
        return (permissions & desiredPerm) != 0;
    }

    /**
     * CrunchTo is a method used to crunch the permissions object to only the permissions of the requesterPerms param.
     */
    public void crunchTo(Permissions requesterPerms) {
        this.permissions &= requesterPerms.intValue();
    }

    public static int getMaxPerms() {
        return READ_MESSAGES
                |SEND_MESSAGES
                |DELETE_MESSAGES
                |ADD_USERS
                |REMOVE_USERS
                |CHANGE_PERMISSIONS;
    }

    private static boolean inBounds(int perms) {
        return perms >= 0 && perms <= getMaxPerms();
    }
}
