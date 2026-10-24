package database;

import app.config.AppConfig;
import app.config.Env;
import app.database.DataSourceFactory;
import app.room.RoomRepository;
import org.junit.jupiter.api.*;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;

public class RoomRepoTest {
    private final static Path testEnvDir = Paths.get("src/test/resources/.env.db.test");
    private static DataSource dataSource;
    private RoomRepository testRepo;

    private boolean cleanupNeeded = false;
    private int newRoomId;

    private int emptyRoom;
    private int fullRoom;

    @BeforeAll
    static void setupDatabase() throws IOException {
        Env env = new Env(testEnvDir, null);
        AppConfig testConfig = new AppConfig(env);
        dataSource = DataSourceFactory.create(testConfig);
    }

    @BeforeEach
    void setupPreparedValues() throws SQLException {
        testRepo = new RoomRepository(dataSource);

        emptyRoom = testRepo.create("repo_testId", System.currentTimeMillis());

        fullRoom = testRepo.create("repo_testId", System.currentTimeMillis());
        testRepo.addToRoom(fullRoom, "repo_roomMember", 1);
    }

    @Test
    void createRoom() throws SQLException {
        newRoomId = testRepo.create("repo_testId", System.currentTimeMillis());

        String owner = testRepo.getOwner(newRoomId);

        Assertions.assertNotNull(owner);
        Assertions.assertEquals("repo_testId", testRepo.getOwner(newRoomId));

        cleanupNeeded = true;
    }

    @Test
    void createRoomFail() {
        Assertions.assertThrows(SQLException.class,
                () -> newRoomId = testRepo.create("repo_nonexistent", System.currentTimeMillis()));
        cleanupNeeded = true;
    }

    @Test
    void deleteFullRoom() throws SQLException {
        Assertions.assertNotNull(testRepo.getOwner(fullRoom));

        testRepo.deleteRoom(fullRoom);

        Assertions.assertNull(testRepo.getOwner(fullRoom));
    }

    @Test
    void deleteEmptyRoom() throws SQLException {
        Assertions.assertNotNull(testRepo.getOwner(emptyRoom));

        testRepo.deleteRoom(emptyRoom);

        Assertions.assertNull(testRepo.getOwner(emptyRoom));
    }

    @Test
    void getOwner() throws SQLException {
        Assertions.assertEquals("repo_testId", testRepo.getOwner(emptyRoom));
    }

    @Test
    void getOwnerNoRoom() throws SQLException {
        Assertions.assertNull(testRepo.getOwner(0));
    }



    @Test
    void addNewMember() throws SQLException{
        Assertions.assertEquals(0, testRepo.getUserPermissions(emptyRoom, "repo_roomMember"));

        Assertions.assertDoesNotThrow(() -> testRepo.addToRoom(emptyRoom, "repo_roomMember", 1));

        Assertions.assertEquals(1, testRepo.getUserPermissions(emptyRoom, "repo_roomMember"));
    }

    @Test
    void addExistingMember() {
        Assertions.assertThrows(SQLException.class, () -> {testRepo.addToRoom(fullRoom, "repo_roomMember", 1);});
    }

    @Test
    void changeMemberPerms() throws SQLException {
        Assertions.assertEquals(1, testRepo.getUserPermissions(fullRoom, "repo_roomMember"));

        Assertions.assertDoesNotThrow(() -> testRepo.changePerms(fullRoom, "repo_roomMember", 7));

        Assertions.assertEquals(7, testRepo.getUserPermissions(fullRoom, "repo_roomMember"));
    }

    @Test
    void getUserPerms() throws SQLException {
        Assertions.assertEquals(1, testRepo.getUserPermissions(fullRoom, "repo_roomMember"));
    }

    @Test
    void getNonexistentPerms() throws SQLException {
        Assertions.assertEquals(0, testRepo.getUserPermissions(emptyRoom, "repo_nonexistent"));
    }

    @Test
    void removeMember() throws SQLException {
        Assertions.assertEquals(1, testRepo.getUserPermissions(fullRoom, "repo_roomMember"));

        Assertions.assertDoesNotThrow(() -> testRepo.removeFromRoom(fullRoom, "repo_roomMember"));

        Assertions.assertEquals(0, testRepo.getUserPermissions(fullRoom, "repo_roomMember"));
    }

    @AfterEach
    void cleanup() throws SQLException {
        testRepo.deleteRoom(emptyRoom);
        testRepo.deleteRoom(fullRoom);
        if (!cleanupNeeded) return;

        testRepo.deleteRoom(newRoomId);

        cleanupNeeded = false;
    }
}
