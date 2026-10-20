package database;

import app.database.DataSourceFactory;
import com.zaxxer.hikari.HikariDataSource;
import app.config.AppConfig;
import app.config.Env;
import app.auth.SessionRepository;
import org.junit.jupiter.api.*;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;

public class SeshRepoTest {
    private final static Path testEnvDir = Paths.get("src/test/resources/.env.db.test");
    private static DataSource dataSource;
    private SessionRepository testRepo;

    private boolean cleanupNeeded = false;

    byte[] existingTokenHash = {
            (byte) 0x6D, (byte) 0x97, (byte) 0x08, (byte) 0x74,
            (byte) 0xD0, (byte) 0xDB, (byte) 0x76, (byte) 0x7A,
            (byte) 0x70, (byte) 0x58, (byte) 0x79, (byte) 0x89,
            (byte) 0x73, (byte) 0xF2, (byte) 0x2C, (byte) 0xF6,
            (byte) 0x58, (byte) 0x96, (byte) 0x01, (byte) 0xED,
            (byte) 0xAB, (byte) 0x57, (byte) 0x99, (byte) 0x63,
            (byte) 0x12, (byte) 0xF2, (byte) 0xEF, (byte) 0x7B,
            (byte) 0x56, (byte) 0xE5, (byte) 0x58, (byte) 0x4D
    };

    byte[] neverExpiringHash = {
            (byte) 0x6D, (byte) 0x97, (byte) 0x08, (byte) 0x74,
            (byte) 0xD0, (byte) 0xDB, (byte) 0x76, (byte) 0x7A,
            (byte) 0x70, (byte) 0x58, (byte) 0x79, (byte) 0x89,
            (byte) 0x73, (byte) 0xF2, (byte) 0x2C, (byte) 0xF6,
            (byte) 0x58, (byte) 0x96, (byte) 0x01, (byte) 0xED,
            (byte) 0xAB, (byte) 0x57, (byte) 0x99, (byte) 0x63,
            (byte) 0x12, (byte) 0xF2, (byte) 0xEF, (byte) 0x7B,
            (byte) 0x56, (byte) 0xE1, (byte) 0x23, (byte) 0x45
    };

    byte[] newTokenHash = {
            (byte) 0xBA, (byte) 0x78, (byte) 0x16, (byte) 0xBF,
            (byte) 0x8F, (byte) 0x01, (byte) 0xCF, (byte) 0xEA,
            (byte) 0x41, (byte) 0x41, (byte) 0x40, (byte) 0xDE,
            (byte) 0x5D, (byte) 0xAE, (byte) 0x22, (byte) 0x23,
            (byte) 0xB0, (byte) 0x03, (byte) 0x61, (byte) 0xA3,
            (byte) 0x96, (byte) 0x17, (byte) 0x7A, (byte) 0x9C,
            (byte) 0xB4, (byte) 0x10, (byte) 0xFF, (byte) 0x61,
            (byte) 0xF2, (byte) 0x00, (byte) 0x15, (byte) 0xAD
    };

    @BeforeAll
    static void setupDatabase() throws IOException {
        Env env = new Env(testEnvDir, null);
        AppConfig testConfig = new AppConfig(env);
        dataSource = DataSourceFactory.create(testConfig);
    }

    @BeforeEach
    void setupRepo() {
        testRepo = new SessionRepository(dataSource);
    }

    @Test
    void getSessionIdBySessionKey() throws SQLException {
        String sessionId = testRepo.getSessionIdBySessionKey(existingTokenHash);

        Assertions.assertEquals("repo_existingSessionId", sessionId);
    }

    @Test
    void newSession() throws SQLException {
        testRepo.newSession(
                "repo_newSessionId",
                "repo_testId",
                newTokenHash,
                System.currentTimeMillis(),
                System.currentTimeMillis()+50000
        );
        String resultId = testRepo.getSessionIdBySessionKey(newTokenHash);

        Assertions.assertEquals("repo_newSessionId", resultId);

        cleanupNeeded = true;
    }

    @Test
    void handleSessionIdConflict() {
        cleanupNeeded = true;

        Assertions.assertThrows(
                SQLException.class,
                () -> testRepo.newSession(
                        "repo_existingSessionId",
                        "repo_testId",
                        newTokenHash,
                        System.currentTimeMillis(),
                        System.currentTimeMillis()+50000
                )
        );

        cleanupNeeded = false;
    }

    @Test
    void handleSessionHashConflict() {
        cleanupNeeded = true;

        Assertions.assertThrows(
                SQLException.class,
                () -> testRepo.newSession(
                        "repo_newSessionId",
                        "repo_testId",
                        existingTokenHash,
                        System.currentTimeMillis(),
                        System.currentTimeMillis()+50000
                )
        );

        cleanupNeeded = false;
    }

    @Test
    void deleteSessionBySessionKey() throws SQLException {
        testRepo.deleteSessionByToken(existingTokenHash);

        String userId = testRepo.getUserIdBySessionKey(existingTokenHash);
        Assertions.assertNull(userId);

        cleanupNeeded = true;
    }

    @Test
    void deleteSessionsByUserId() throws SQLException {
        testRepo.deleteSessionsByUserId("repo_testId");

        String sessionId = testRepo.getSessionIdBySessionKey(existingTokenHash);
        Assertions.assertNull(sessionId);

        cleanupNeeded = true;
    }

    @Test
    void tokenExpired() throws SQLException {
        Assertions.assertTrue(testRepo.tokenExpired(existingTokenHash, System.currentTimeMillis()));
    }

    @Test
    void tokenNonexistent() throws SQLException {
        Assertions.assertTrue(testRepo.tokenExpired(newTokenHash, System.currentTimeMillis()));
    }

    @Test
    void tokenNeverExpires() throws SQLException {
        Assertions.assertFalse(testRepo.tokenExpired(neverExpiringHash, System.currentTimeMillis()));
    }

    @Test
    void wrongSessionOwner() throws SQLException {
        Assertions.assertFalse(testRepo.sessionOwnerCheck("repo_existingSessionId", "repo_wrongUser"));
    }

    @Test
    void correctSessionOwner() throws SQLException {
        Assertions.assertTrue(testRepo.sessionOwnerCheck("repo_existingSessionId", "repo_testId"));
    }

    @AfterEach
    void cleanup() throws SQLException {
        if (!cleanupNeeded) return;
        testRepo.deleteSessionByToken(newTokenHash);
        testRepo.deleteSessionByToken(existingTokenHash);
        testRepo.deleteSessionByToken(neverExpiringHash);

        testRepo.newSession(
                "repo_existingSessionId",
                "repo_testId",
                existingTokenHash,
                System.currentTimeMillis(),
                0L
        );
        testRepo.newSession(
                "repo_neverExpires",
                "repo_testId",
                neverExpiringHash,
                System.currentTimeMillis(),
                null
        );

        cleanupNeeded = false;
    }

    @AfterAll
    static void closeDataSource() {
        if (dataSource instanceof HikariDataSource hds) hds.close();
    }
}
