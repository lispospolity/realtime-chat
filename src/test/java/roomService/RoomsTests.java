package roomService;

import app.auth.dto.Result;
import app.config.AppConfig;
import app.config.Env;
import app.database.DataSourceFactory;
import app.dto.response.ErrorResponse;
import app.room.Permissions;
import app.room.RoomRepository;
import app.room.RoomService;
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
        roomService = new RoomService(roomRepo);

        testingRoom = roomRepo.create(testOwner, System.currentTimeMillis());

        roomRepo.addToRoom(testingRoom, testMemberAllPerms, Permissions.getMaxPerms());
        roomRepo.addToRoom(testingRoom, testMemberNoPerms, 0);
    }

    @Test
    void noPermsAddToRoom() {
        Result result = roomService.addToRoom(userNotInRoom, testingRoom, userNotInRoom, Permissions.READ_MESSAGES);

        ErrorResponse response = Assertions.assertInstanceOf(ErrorResponse.class, result.response());

        Assertions.assertFalse(response.success());
        Assertions.assertEquals("You do not have the required permissions to add users.", response.error());
    }

    @Test
    void badPermsAddToRoom() {
        Result result = roomService.addToRoom(testMemberNoPerms, testingRoom, userNotInRoom, Permissions.READ_MESSAGES);

        ErrorResponse response = Assertions.assertInstanceOf(ErrorResponse.class, result.response());

        Assertions.assertFalse(response.success());
        Assertions.assertEquals("You do not have the required permissions to add users.", response.error());
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
