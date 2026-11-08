package roomService;

import app.dto.Result;
import app.config.AppConfig;
import app.config.Env;
import app.database.DataSourceFactory;
import app.dto.response.ErrorResponse;
import app.room.Permissions;
import app.room.RoomMemberRepository;
import app.room.RoomRepository;
import app.room.RoomService;
import app.room.dto.response.RoomMembersResponse;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.*;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;

public class RoomsTests {
    private final static Path testEnvDir = Paths.get("src/test/resources/.env.db.test");
    private static DataSource dataSource;

    private RoomRepository roomRepo;
    private RoomMemberRepository roomMemberRepo;
    private RoomService roomService;

    private final String testOwner = "12345678-1234-1234-1234-b3588392f1c8";
    private final String testMemberAllPerms = "87654321-1234-1234-1234-b3588392f1c8";
    private final String testMemberNoPerms = "87654321-4321-1234-1234-b3588392f1c8";

    private final String userNotInRoom = "d11c5281-afe0-485d-ae55-b3588392f1c8";

    private int testingRoom;

    @BeforeAll
    static void setupDatabase() throws IOException {
        Env env = new Env(testEnvDir, null);
        AppConfig testConfig = new AppConfig(env);
        dataSource = DataSourceFactory.create(testConfig);
    }

    @BeforeEach
    void setupRoomService() throws SQLException {
        roomRepo = new RoomRepository(dataSource);
        roomMemberRepo = new RoomMemberRepository(dataSource);
        roomService = new RoomService(roomRepo, roomMemberRepo);

        testingRoom = roomRepo.create(testOwner, System.currentTimeMillis());
        roomMemberRepo.addToRoom(testingRoom, testOwner, Permissions.getMaxPerms()); //roomService does that

        roomMemberRepo.addToRoom(testingRoom, testMemberAllPerms, Permissions.getMaxPerms());
        roomMemberRepo.addToRoom(testingRoom, testMemberNoPerms, 0);
    }

    @Test
    void addToRoom_noPerms() {
        Result result = roomService.addToRoom(userNotInRoom, testingRoom, userNotInRoom, Permissions.READ_MESSAGES);

        ErrorResponse response = Assertions.assertInstanceOf(ErrorResponse.class, result.response());

        Assertions.assertFalse(response.success());
        Assertions.assertEquals("You do not have the required permissions to add users.", response.error());
    }

    @Test
    void addToRoom_badPerms() {
        Result result = roomService.addToRoom(testMemberNoPerms, testingRoom, userNotInRoom, Permissions.READ_MESSAGES);

        ErrorResponse response = Assertions.assertInstanceOf(ErrorResponse.class, result.response());

        Assertions.assertFalse(response.success());
        Assertions.assertEquals("You do not have the required permissions to add users.", response.error());
    }

    @Test
    void addToRoom_incorrectInputPerms() {
        Result result = roomService.addToRoom(testMemberAllPerms, testingRoom, userNotInRoom, -1);

        ErrorResponse response = Assertions.assertInstanceOf(ErrorResponse.class, result.response());

        Assertions.assertFalse(response.success());
        Assertions.assertEquals("Permissions out of bounds.", response.error());
    }

    @Test
    void addToRoom_userInRoom() {
        Result result = roomService.addToRoom(testMemberAllPerms, testingRoom, testMemberNoPerms, Permissions.READ_MESSAGES);

        ErrorResponse response = Assertions.assertInstanceOf(ErrorResponse.class, result.response());

        Assertions.assertFalse(response.success());
        Assertions.assertEquals("This user is already in the room.", response.error());
    }

    @Test
    void getRoomMembers_owner() {
        Result result = roomService.getRoomMembers(testOwner, testingRoom);

        RoomMembersResponse response = Assertions.assertInstanceOf(RoomMembersResponse.class, result.response());

        Assertions.assertTrue(response.success());

        Assertions.assertFalse(response.members().isEmpty());
        Assertions.assertTrue(response.members().contains(testOwner));
        Assertions.assertTrue(response.members().contains(testMemberAllPerms));
        Assertions.assertTrue(response.members().contains(testMemberNoPerms));
    }

    @Test
    void getRoomMembers_allPerms() {
        Result result = roomService.getRoomMembers(testMemberAllPerms, testingRoom);

        RoomMembersResponse response = Assertions.assertInstanceOf(RoomMembersResponse.class, result.response());

        Assertions.assertTrue(response.success());

        Assertions.assertFalse(response.members().isEmpty());
        Assertions.assertTrue(response.members().contains(testOwner));
        Assertions.assertTrue(response.members().contains(testMemberAllPerms));
        Assertions.assertTrue(response.members().contains(testMemberNoPerms));
    }

    @Test
    void getRoomMembers_noPerms() {
        Result result = roomService.getRoomMembers(testMemberNoPerms, testingRoom);

        RoomMembersResponse response = Assertions.assertInstanceOf(RoomMembersResponse.class, result.response());

        Assertions.assertTrue(response.success());

        Assertions.assertFalse(response.members().isEmpty());
        Assertions.assertTrue(response.members().contains(testOwner));
        Assertions.assertTrue(response.members().contains(testMemberAllPerms));
        Assertions.assertTrue(response.members().contains(testMemberNoPerms));
    }

    @Test
    void getRoomMembers_notInRoom() {
        Result result = roomService.getRoomMembers(userNotInRoom, testingRoom);

        ErrorResponse response = Assertions.assertInstanceOf(ErrorResponse.class, result.response());

        Assertions.assertFalse(response.success());
        Assertions.assertEquals("You're not a member of this room.", response.error());
    }

    @AfterEach
    void cleanup() throws SQLException {
        roomRepo.deleteRoom(testingRoom);
    }


    @AfterAll
    static void closeDataSource() {
        if (dataSource instanceof HikariDataSource hds) hds.close();
    }
}
