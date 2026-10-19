package authService;

import auth.*;
import com.zaxxer.hikari.HikariDataSource;
import config.AppConfig;
import config.Env;
import database.DataSourceFactory;
import database.repository.SessionRepository;
import database.repository.UserRepository;
import org.junit.jupiter.api.*;
import utils.TokenUtils;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;

public class AuthServiceSessionsTests {
    private final static Path testEnvDir = Paths.get("src/test/resources/.env.db.test");
    private static AppConfig testConfig;
    private static DataSource dataSource;
    private UserRepository userRepo;
    private SessionRepository sessionRepo;
    private AuthService authService;

    String deleteMeToken = "ABC";
    String expiredToken = "CBA";

    private boolean cleanupNeeded = false;
    private String deleteSession = null;

    @BeforeAll
    static void setupDatabase() throws IOException {
        Env env = new Env(testEnvDir, null);
        testConfig = new AppConfig(env);
        dataSource = DataSourceFactory.create(testConfig);
    }

    @BeforeEach
    void setupAuthService() {
        userRepo = new UserRepository(dataSource);
        sessionRepo = new SessionRepository(dataSource);
        authService = new AuthService(testConfig, userRepo, sessionRepo);
    }

    @Test
    void loginSuccess() {
        Result<?> result = authService.login("auth_testUser", "qwertyuiop");
        LoginResponse response =
                Assertions.assertInstanceOf(LoginResponse.class, result.response());

        Assertions.assertFalse(response.token().isBlank());
        Assertions.assertFalse(response.sessionId().isBlank());
        cleanupNeeded = true;
        deleteSession = response.sessionId();
    }

    @Test
    void loginUserNonexistent() {
        Result<?> result = authService.login("auth_nonexistent", "qwertyuiop");
        ErrorResponse response =
                Assertions.assertInstanceOf(ErrorResponse.class, result.response());

        Assertions.assertEquals("Username or password is incorrect.", response.error());

        cleanupNeeded = true;
    }

    @Test
    void loginWrongPassword() {
        Result<?> result = authService.login("auth_testUser", "");
        ErrorResponse response =
                Assertions.assertInstanceOf(ErrorResponse.class, result.response());

        Assertions.assertEquals("Username or password is incorrect.", response.error());

        cleanupNeeded = true;
    }

    @Test
    void logoutOneSuccess() {
        Result<?> result = authService.logout(deleteMeToken, "auth_deleteMe");
        GenericResponse response =
                Assertions.assertInstanceOf(GenericResponse.class, result.response());

        Assertions.assertEquals("Logout successful.", response.message());

        cleanupNeeded = true;
    }

    @Test
    void logoutOneSeshTimedOut() {
        Result<?> result = authService.logout(expiredToken, "auth_deleteMe");
        ErrorResponse response =
                Assertions.assertInstanceOf(ErrorResponse.class, result.response());

        Assertions.assertEquals("Session timed out.", response.error());

        cleanupNeeded = true;
    }

    @Test
    void logoutAllSuccess() {
        Result<?> result = authService.logoutAll(deleteMeToken);
        GenericResponse response =
                Assertions.assertInstanceOf(GenericResponse.class, result.response());

        Assertions.assertEquals("Logout successful.", response.message());

        cleanupNeeded = true;
    }

    @Test
    void logoutAllWrongKey() {
        Result<?> result = authService.logoutAll(expiredToken);
        ErrorResponse response =
                Assertions.assertInstanceOf(ErrorResponse.class, result.response());

        Assertions.assertEquals("Incorrect session key.", response.error());

        cleanupNeeded = true;
    }



    @AfterEach
    void cleanup() throws SQLException, NoSuchAlgorithmException {
        if (!cleanupNeeded) return;
        if (deleteSession != null) {
            sessionRepo.deleteSessionBySessionId(deleteSession);
            deleteSession = null;
        }
        sessionRepo.deleteSessionBySessionId("auth_deleteMe");
        sessionRepo.deleteSessionBySessionId("auth_deleteAll");

        sessionRepo.newSession(
                "auth_deleteMe",
                "d11c5281-afe0-485d-ae55-b3588392f1c8",
                TokenUtils.hashToken(deleteMeToken),
                System.currentTimeMillis(),
                null
        );
        sessionRepo.newSession(
                "auth_deleteAll",
                "d11c5281-afe0-485d-ae55-b3588392f1c8",
                TokenUtils.hashToken(expiredToken),
                System.currentTimeMillis(),
                0L
        );
        cleanupNeeded = false;
    }

    @AfterAll
    static void closeDataSource() {
        if (dataSource instanceof HikariDataSource hds) hds.close();
    }
}
