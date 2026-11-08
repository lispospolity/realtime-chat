package database;

import app.config.AppConfig;
import app.config.Env;
import app.database.DataSourceFactory;
import app.room.RoomMemberRepository;
import app.room.RoomRepository;
import org.junit.jupiter.api.*;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.List;

public class RoomRepoTest {
    private final static Path testEnvDir = Paths.get("src/test/resources/.env.db.test");
    private static DataSource dataSource;

    private RoomMemberRepository roomMemberRepo;
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
        roomMemberRepo = new RoomMemberRepository(dataSource);
        testRepo = new RoomRepository(dataSource);

        emptyRoom = testRepo.create("repo_testId", System.currentTimeMillis());

        fullRoom = testRepo.create("repo_testId", System.currentTimeMillis());
        roomMemberRepo.addToRoom(fullRoom, "repo_roomMember", 1);
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
    void getOwnedRooms() throws SQLException {
        List<Integer> resultedList = testRepo.getUserOwnedRooms("repo_testId");

        Assertions.assertTrue(resultedList.contains(emptyRoom));
        Assertions.assertTrue(resultedList.contains(fullRoom));
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
