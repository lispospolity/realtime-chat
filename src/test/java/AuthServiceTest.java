import auth.AuthService;
import auth.Result;
import com.zaxxer.hikari.HikariDataSource;
import config.AppConfig;
import config.Env;
import database.DataSourceFactory;
import database.repository.SessionRepository;
import database.repository.UserRepository;
import org.junit.jupiter.api.*;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.Objects;

public class AuthServiceTest {
    private final static Path testEnvDir = Paths.get("src/test/resources/.env.db.test");
    private static AppConfig testConfig;
    private static DataSource dataSource;
    private UserRepository userRepo;
    private SessionRepository sessionRepo;
    private AuthService authService;

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
    void regExistingUser() throws SQLException {
        Result<?> result = authService.register("auth_testUser", "qwertyuiop");
        boolean requestFailed =
                result.httpCode() == 409 &&
                Objects.equals("d11c5281-afe0-485d-ae55-b3588392f1c8", userRepo.getId("auth_testUser"));

        Assertions.assertTrue(requestFailed);
    }

    @Test
    void regIncorrectPassword() throws SQLException {
        Result<?> result = authService.register("auth_wrongPassword", "");
        boolean requestFailed = result.httpCode() == 400 && userRepo.getId("auth_wrongPassword") == null;

        Assertions.assertTrue(requestFailed);
    }

    @Test
    void regIncorrectUsername() throws SQLException {
        Result<?> result = authService.register("", "qwertyuiop");
        boolean requestFailed = result.httpCode() == 400 && userRepo.getId("") == null;

        Assertions.assertTrue(requestFailed);

    }

    @Test
    void regNewUser() throws SQLException {
        Result<?> result = authService.register("auth_newTestUser", "qwertyuiop");
        boolean userAdded = result.httpCode() == 201 && userRepo.getId("auth_newTestUser") != null;

        Assertions.assertTrue(userAdded);
    }

    @AfterEach
    void cleanup() throws SQLException {
        String id = userRepo.getId("auth_newTestUser");

        if (id != null) {
            userRepo.delete(id);
        }
    }

    @AfterAll
    static void closeDataSource() {
        if (dataSource instanceof HikariDataSource hds) hds.close();
    }
}
