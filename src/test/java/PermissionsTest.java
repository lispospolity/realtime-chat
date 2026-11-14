import app.room.Permissions;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class PermissionsTest {

    @Test
    void ownerPerms() {
        int owner = Permissions.getMaxPerms();

        Permissions perms = new Permissions(owner);

        Assertions.assertTrue(perms.allow(Permissions.READ_MESSAGES));
        Assertions.assertTrue(perms.allow(Permissions.SEND_MESSAGES));
        Assertions.assertTrue(perms.allow(Permissions.DELETE_MESSAGES));
        Assertions.assertTrue(perms.allow(Permissions.ADD_USERS));
        Assertions.assertTrue(perms.allow(Permissions.REMOVE_USERS));
        Assertions.assertTrue(perms.allow(Permissions.CHANGE_PERMISSIONS));
    }

    @Test
    void minimalPerms() {
        int notOwner = Permissions.MINIMAL;

        Permissions perms = new Permissions(notOwner);

        Assertions.assertFalse(perms.allow(Permissions.READ_MESSAGES));
        Assertions.assertFalse(perms.allow(Permissions.SEND_MESSAGES));
        Assertions.assertFalse(perms.allow(Permissions.DELETE_MESSAGES));
        Assertions.assertFalse(perms.allow(Permissions.ADD_USERS));
        Assertions.assertFalse(perms.allow(Permissions.REMOVE_USERS));
        Assertions.assertFalse(perms.allow(Permissions.CHANGE_PERMISSIONS));
    }

    @Test
    void outOfBounds() {
        int aboveMaxPayload = Permissions.getMaxPerms()+1;
        int belowMinPayload = -1;

        Assertions.assertThrows(IllegalArgumentException.class, () -> new Permissions(aboveMaxPayload));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new Permissions(belowMinPayload));
    }

    @Test
    void crunch() {
        int requester = 7;
        int newInt = Permissions.getMaxPerms();

        Permissions requesterPerms = new Permissions(requester);
        Permissions newPerms = new Permissions(newInt);

        newPerms.crunchTo(requesterPerms);

        Assertions.assertEquals(requester, newPerms.intValue());

        Assertions.assertEquals(requester, requesterPerms.intValue());
    }

    @Test
    void allow_minimal() {
        Permissions min = new Permissions(Permissions.MINIMAL);
        Permissions mid = new Permissions(Permissions.REMOVE_USERS);
        Permissions max = new Permissions(Permissions.getMaxPerms());

        Assertions.assertTrue(min.allow(Permissions.MINIMAL));
        Assertions.assertTrue(mid.allow(Permissions.MINIMAL));
        Assertions.assertTrue(max.allow(Permissions.MINIMAL));
    }
}
