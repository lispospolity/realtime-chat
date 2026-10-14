import config.AppConfig;
import config.Env;
import database.DataSourceFactory;
import database.repository.UserRepository;
import org.junit.jupiter.api.*;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;

public class UserRepoTest {
    private final static Path testEnvDir = Paths.get("src/test/resources/.env.db.test");
    private static DataSource dataSource;
    private UserRepository testRepo;

    @BeforeAll
    static void setupDatabase() throws IOException {
        Env env = new Env(testEnvDir, null);
        AppConfig testConfig = new AppConfig(env);
        dataSource = DataSourceFactory.create(testConfig);
    }

    @BeforeEach
    void setUpRepo() {
        testRepo = new UserRepository(dataSource);
    }

    @Test
    void exists() throws SQLException {
        Assertions.assertNotNull(testRepo.getId("testUser"));
    }

    @Test
    void doesntExist() throws SQLException {
        Assertions.assertNull(testRepo.getId("nonexistentUser"));
    }

    @Test
    void getId() throws SQLException {
        Assertions.assertEquals("testId", testRepo.getId("testUser"));
    }

    @Test
    void newUser() throws SQLException {
        String username = "newTestUser";
        testRepo.create(
                "newTestId",
                username,
                "qwertyuiop",
                System.currentTimeMillis()
        );
        Assertions.assertNotNull(testRepo.getId(username));
    }

    @Test
    void deleteUser() throws SQLException {
        testRepo.delete("deleteId");
        Assertions.assertNull(testRepo.getId("deleteMe"));
    }

    @AfterAll
    static void restoreDatabase() throws SQLException{
        UserRepository testRepo = new UserRepository(dataSource);
        testRepo.create(
                "deleteId",
                "deleteMe",
                "testhash_!@#$",
                0L
                );
        testRepo.delete("newTestId");
    }
}
