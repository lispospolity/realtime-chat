package database;

import app.database.DataSourceFactory;
import com.zaxxer.hikari.HikariDataSource;
import app.config.AppConfig;
import app.config.Env;
import app.user.UserRepository;
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
        Assertions.assertNotNull(testRepo.getId("repo_testUser"));
    }

    @Test
    void doesntExist() throws SQLException {
        Assertions.assertNull(testRepo.getId("repo_nonexistentUser"));
    }

    @Test
    void getId() throws SQLException {
        Assertions.assertEquals("repo_testId", testRepo.getId("repo_testUser"));
    }

    @Test
    void newUser() throws SQLException {
        String username = "repo_newTestUser";
        testRepo.create(
                "repo_newTestId",
                username,
                "qwertyuiop",
                System.currentTimeMillis()
        );
        Assertions.assertNotNull(testRepo.getId(username));
    }

    @Test
    void deleteUser() throws SQLException {
        testRepo.delete("repo_deleteId");
        Assertions.assertNull(testRepo.getId("repo_deleteMe"));
    }

    @AfterAll
    static void restoreDatabase() throws SQLException{
        UserRepository testRepo = new UserRepository(dataSource);
        testRepo.create(
                "repo_deleteId",
                "repo_deleteMe",
                "testhash_!@#$",
                0L
                );
        testRepo.delete("repo_newTestId");

        if (dataSource instanceof HikariDataSource hds) hds.close();
    }
}
