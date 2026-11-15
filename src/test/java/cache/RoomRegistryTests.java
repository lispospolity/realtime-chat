package cache;

import app.config.AppConfig;
import app.config.Env;
import app.database.DataSourceFactory;
import app.room.*;
import app.user.UserRepository;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.*;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class RoomRegistryTests {
    private final static Path testEnvDir = Paths.get("src/test/resources/.env.db.test");
    private static AppConfig testConfig;

    private static DataSource ds;

    private static UserRepository userRepo;
    private static RoomRepository roomRepo;
    private static RoomMemberRepository roomMemberRepo;

    private RoomRegistry roomRegistry;

    private int testingRoom;

    private final String testOwner = "cache_testOwnerId";
    private final String testMemberAllPerms = "cache_testMemberAllPermsId";
    private final String testMemberNoPerms = "cache_testMemberNoPermsId";
    private final String testMemberNotInRoom = "cache_testMemberNotInRoomId";



    @BeforeAll
    static void setupDatabase() throws IOException {
        Env env = new Env(testEnvDir, null);
        testConfig = new AppConfig(env);
        ds = DataSourceFactory.create(testConfig);

        userRepo = new UserRepository(ds);
        roomRepo = new RoomRepository(ds);
        roomMemberRepo = new RoomMemberRepository(ds);
    }

    @BeforeEach
    void setup() throws SQLException{
        setupRegistry();
        setupTestUsers();
        setupTestRooms();
        setupTestMembers();
    }
    void setupRegistry() {
        roomRegistry = new RoomRegistry(roomRepo, roomMemberRepo);
    }
    void setupTestUsers() throws SQLException {
        userRepo.create(testOwner, "cache_room1", "qwertyuiop", System.currentTimeMillis());
        userRepo.create(testMemberAllPerms, "cache_room2", "qwertyuiop", System.currentTimeMillis());
        userRepo.create(testMemberNoPerms, "cache_room3", "qwertyuiop", System.currentTimeMillis());
        userRepo.create(testMemberNotInRoom, "cache_room4", "qwertyuiop", System.currentTimeMillis());
    }
    void setupTestRooms() throws SQLException {
        testingRoom = roomRepo.create(testOwner, System.currentTimeMillis());
        roomMemberRepo.addToRoom(testingRoom, testOwner, Permissions.MINIMAL); //usually done by roomservice
    }
    void setupTestMembers() throws SQLException{
        roomMemberRepo.addToRoom(testingRoom, testMemberAllPerms, Permissions.getMaxPerms());
        roomMemberRepo.addToRoom(testingRoom, testMemberNoPerms, Permissions.MINIMAL);
    }

    @Test
    void getRoomById_withChange() throws SQLException {
        Room roomBefore = roomRegistry.getRoomById(testingRoom);
        Assertions.assertNotNull(roomBefore);

        roomRegistry.removeRoom(testingRoom);

        Room roomAfter = roomRegistry.getRoomById(testingRoom);
        Assertions.assertNull(roomAfter);
    }

    @Test
    void getMemberById_withChange() throws SQLException {
        Room room = roomRegistry.getRoomById(testingRoom);
        Assertions.assertNotNull(room);

        RoomMember memberBefore = roomRegistry.getRoomMember(testingRoom, testMemberNoPerms);
        Assertions.assertNotNull(memberBefore);
        Permissions permsCopy = new Permissions(memberBefore.getPermissions().intValue());

        roomRegistry.updateMemberPerms(room, memberBefore, new Permissions(Permissions.READ_MESSAGES));

        RoomMember memberAfter = roomRegistry.getRoomMember(testingRoom, testMemberNoPerms);
        Assertions.assertNotNull(memberAfter);

        Assertions.assertEquals(memberBefore, memberAfter);
        Assertions.assertNotEquals(permsCopy, memberAfter.getPermissions());
    }


    @Test
    void getMembers_dbAndCacheLimitation() throws SQLException {
        List<RoomMember> membersBefore = roomRegistry.getRoomMembers(testingRoom);
        Assertions.assertNotNull(membersBefore);
        Assertions.assertFalse(membersBefore.isEmpty());

        //add a user to check if the registry actually skips a db call after the first one
        roomMemberRepo.addToRoom(testingRoom, testMemberNotInRoom, Permissions.MINIMAL);

        List<RoomMember> membersAfter = roomRegistry.getRoomMembers(testingRoom);
        Assertions.assertNotNull(membersBefore);
        Assertions.assertFalse(membersBefore.isEmpty());

        List<String> memberIdsBefore = new ArrayList<>();
        for (RoomMember member : membersBefore) {
            memberIdsBefore.add(member.getMemberId());
        }
        List<String> memberIdsAfter = new ArrayList<>();
        for (RoomMember member : membersAfter) {
            memberIdsAfter.add(member.getMemberId());
        }

        Assertions.assertFalse(memberIdsBefore.contains(testMemberNotInRoom));
        Assertions.assertFalse(memberIdsAfter.contains(testMemberNotInRoom));

        Assertions.assertTrue(memberIdsBefore.containsAll(memberIdsAfter));
    }



    @AfterEach
    void cleanupDatabase() throws SQLException {
        removeRooms();
        removeUsers();
    }
    void removeRooms() throws SQLException {
        roomRepo.deleteRoom(testingRoom);
    }
    void removeUsers() throws SQLException {
        userRepo.delete(testOwner);
        userRepo.delete(testMemberAllPerms);
        userRepo.delete(testMemberNoPerms);
        userRepo.delete(testMemberNotInRoom);
    }

    @AfterAll
    static void closeDataSource() {
        if (ds instanceof HikariDataSource hds) hds.close();
    }
}
