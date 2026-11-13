package cache;

import app.config.AppConfig;
import app.config.Env;
import app.database.DataSourceFactory;
import app.session.Session;
import app.session.SessionRegistry;
import app.session.SessionRepository;
import app.session.Token;
import app.user.UserRepository;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.*;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.UUID;

public class SessionRegistryTests {
    private final static Path testEnvDir = Paths.get("src/test/resources/.env.db.test");
    private static AppConfig testConfig;

    private static DataSource ds;

    private static UserRepository userRepo;
    private static SessionRepository sessionRepo;

    private SessionRegistry sessionRegistry;

    private String testUserId;

    private Token neverExpiringToken;
    private Token expiredToken;

    private Session expiredSession;

    @BeforeAll
    static void setupDatabase() throws IOException {
        Env env = new Env(testEnvDir, null);
        testConfig = new AppConfig(env);
        ds = DataSourceFactory.create(testConfig);
        userRepo = new UserRepository(ds);
        sessionRepo = new SessionRepository(ds);
    }

    @BeforeEach
    void setupSessions() throws NoSuchAlgorithmException, SQLException {
        setupRegistry();
        createUsers();
        createSessions();
    }
    void setupRegistry() {
        sessionRegistry = new SessionRegistry(testConfig, sessionRepo);
    }
    void createUsers() throws SQLException {
        testUserId = UUID.randomUUID().toString();
        userRepo.create(testUserId, "cache_test", "qwertyuiop", System.currentTimeMillis());
    }
    void createSessions() throws NoSuchAlgorithmException, SQLException {
        neverExpiringToken = new Token();
        sessionRepo.newSession("cache_test1", testUserId, neverExpiringToken.getHashedValue(), System.currentTimeMillis(), null);
        expiredToken = new Token();
        expiredSession = new Session(expiredToken, testUserId, "cache_testExpired", 0L);
        sessionRepo.newSession("cache_testExpired", testUserId, expiredToken.getHashedValue(), System.currentTimeMillis(), 0L);
    }

    @Test
    void addSession_dbConstraintBroken() {
        SQLException dbEx = Assertions.assertThrows(
                SQLIntegrityConstraintViolationException.class,
                () -> sessionRegistry.addSession(neverExpiringToken, "cache_test1", testUserId));
        Assertions.assertEquals(1062, dbEx.getErrorCode());
    }

    @Test
    void addSession_cacheConstraintBroken() throws NoSuchAlgorithmException {
        Token token = new Token();

        Assertions.assertDoesNotThrow(() -> sessionRegistry.addSession(token, "cache_test2", testUserId));

        Session session = Assertions.assertDoesNotThrow(() -> sessionRegistry.getSessionByKey(token.getValue()));
        Assertions.assertNotNull(session);

        SQLException cacheEx = Assertions.assertThrows(
                SQLIntegrityConstraintViolationException.class,
                () -> sessionRegistry.addSession(token, "cache_test2", testUserId));

        Assertions.assertEquals("Unique sessionKey constraint broken.", cacheEx.getMessage());
    }

    @Test
    void removeSessionBySessionId_success() throws SQLException {
        Assertions.assertNotNull(sessionRepo.getSessionBySessionKey(neverExpiringToken.getHashedValue()));

        Assertions.assertDoesNotThrow(() -> sessionRegistry.removeSessionBySessionId("cache_test1"));

        Assertions.assertNull(sessionRepo.getSessionBySessionKey(neverExpiringToken.getHashedValue()));
    }

    @Test
    void removeSessionBySessionId_nonexistentId() throws SQLException {
        Assertions.assertNotNull(sessionRepo.getSessionBySessionKey(neverExpiringToken.getHashedValue()));

        Assertions.assertDoesNotThrow(() -> sessionRegistry.removeSessionBySessionId(""));

        Assertions.assertNotNull(sessionRepo.getSessionBySessionKey(neverExpiringToken.getHashedValue()));
    }

    @Test
    void removeAllUserSessions_success() throws NoSuchAlgorithmException, SQLException {
        Assertions.assertNotNull(sessionRepo.getSessionBySessionKey(neverExpiringToken.getHashedValue()));
        Assertions.assertNotNull(sessionRepo.getSessionBySessionKey(expiredToken.getHashedValue()));

        sessionRegistry.removeAllUserSessions(expiredSession);

        Assertions.assertNull(sessionRepo.getSessionBySessionKey(neverExpiringToken.getHashedValue()));
        Assertions.assertNull(sessionRepo.getSessionBySessionKey(expiredToken.getHashedValue()));
    }

    @Test
    void removeAllUserSessions_null() {
        Assertions.assertThrows(NullPointerException.class, () -> sessionRegistry.removeAllUserSessions(null));
    }

    @Test
    void sessionOwnerCheck_expiredSession() throws SQLException {
        Assertions.assertTrue(sessionRegistry.sessionOwnerCheck(expiredSession.getSessionId(), expiredSession.getUserId()));
    }

    @AfterEach
    void cleanupDatabase() throws SQLException {
        removeUsers();
    }
    void removeUsers() throws SQLException {
        userRepo.delete(testUserId);
    }

    @AfterAll
    static void closeDataSource() {
        if (ds instanceof HikariDataSource hds) hds.close();
    }
}
