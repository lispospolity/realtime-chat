package database;

import app.config.AppConfig;
import app.config.Env;
import app.database.DataSourceFactory;
import app.room.RoomMember;
import app.room.RoomMemberRepository;
import app.room.RoomRepository;
import org.junit.jupiter.api.*;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.List;

public class RoomMemberRepoTest {

    private final static Path testEnvDir = Paths.get("src/test/resources/.env.db.test");
    private static DataSource dataSource;
    private RoomRepository roomRepo;
    private RoomMemberRepository testRepo;

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
        roomRepo = new RoomRepository(dataSource);
        testRepo = new RoomMemberRepository(dataSource);

        emptyRoom = roomRepo.create("repo_testId", System.currentTimeMillis());

        fullRoom = roomRepo.create("repo_testId", System.currentTimeMillis());
        testRepo.addToRoom(fullRoom, "repo_roomMember", 1);
    }


    @Test
    void getMemberList() throws SQLException {
        List<RoomMember> membersFull = testRepo.getRoomMembers(fullRoom);
        List<RoomMember> membersEmpty = testRepo.getRoomMembers(emptyRoom);

        Assertions.assertEquals("repo_roomMember", membersFull.getFirst().getMemberId());
        Assertions.assertTrue(membersEmpty.isEmpty());
    }

    @Test
    void getUserRooms() throws SQLException {
        List<Integer> resultedList = testRepo.getUserRooms("repo_roomMember");

        Assertions.assertFalse(resultedList.contains(emptyRoom));
        Assertions.assertTrue(resultedList.contains(fullRoom));
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
        roomRepo.deleteRoom(emptyRoom);
        roomRepo.deleteRoom(fullRoom);
    }
}
