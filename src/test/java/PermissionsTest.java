import app.room.Permissions;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class PermissionsTest {

    @Test
    void ownerPerms() {
        int owner = Permissions.getOwnerPerms();

        Permissions perms = new Permissions(owner);

        Assertions.assertTrue(perms.allow(Permissions.READ_MESSAGES));
        Assertions.assertTrue(perms.allow(Permissions.SEND_MESSAGES));
        Assertions.assertTrue(perms.allow(Permissions.DELETE_MESSAGES));
        Assertions.assertTrue(perms.allow(Permissions.ADD_USERS));
        Assertions.assertTrue(perms.allow(Permissions.REMOVE_USERS));
        Assertions.assertTrue(perms.allow(Permissions.CHANGE_PERMISSIONS));
        Assertions.assertTrue(perms.allow(Permissions.CHANGE_OWNER));
    }

    @Test
    void notUserPerms() {
        int notOwner = 0;

        Permissions perms = new Permissions(notOwner);

        Assertions.assertFalse(perms.allow(Permissions.READ_MESSAGES));
        Assertions.assertFalse(perms.allow(Permissions.SEND_MESSAGES));
        Assertions.assertFalse(perms.allow(Permissions.DELETE_MESSAGES));
        Assertions.assertFalse(perms.allow(Permissions.ADD_USERS));
        Assertions.assertFalse(perms.allow(Permissions.REMOVE_USERS));
        Assertions.assertFalse(perms.allow(Permissions.CHANGE_PERMISSIONS));
        Assertions.assertFalse(perms.allow(Permissions.CHANGE_OWNER));
    }


}
