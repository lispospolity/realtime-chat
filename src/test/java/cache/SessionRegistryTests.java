package cache;

import app.config.AppConfig;
import app.config.Env;
import app.database.DataSourceFactory;
import app.session.SessionRegistry;
import app.session.SessionRepository;
import app.session.Token;
import app.user.UserRepository;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.UUID;

public class SessionRegistryTests {
    private final static Path testEnvDir = Paths.get("src/test/resources/.env.db.test");

    private static DataSource ds;

    private static UserRepository userRepo;
    private static SessionRepository sessionRepo;

    private static SessionRegistry sessionRegistry;

    private String testUserId;
    private Token neverExpiringToken;


    @BeforeAll
    static void setupCache() throws IOException {
        Env env = new Env(testEnvDir, null);
        AppConfig testConfig = new AppConfig(env);
        ds = DataSourceFactory.create(testConfig);
        userRepo = new UserRepository(ds);
        sessionRepo = new SessionRepository(ds);
        sessionRegistry = new SessionRegistry(testConfig, sessionRepo);
    }

    @BeforeEach
    void setupSessions() throws NoSuchAlgorithmException, SQLException {
        createUsers();
        createSessions();
    }
    void createUsers() throws SQLException {
        testUserId = UUID.randomUUID().toString();
        userRepo.create(testUserId, "cache_test", "qwertyuiop", System.currentTimeMillis());
    }
    void createSessions() throws NoSuchAlgorithmException, SQLException {
        neverExpiringToken = Token.createToken();
        sessionRepo.newSession("abc", testUserId, neverExpiringToken.getHashedValue(), System.currentTimeMillis(), null);
    }






    @AfterEach
    void cleanupDatabase() throws SQLException {
        removeSessions();
        removeUsers();
    }
    void removeSessions() throws SQLException {
        sessionRepo.deleteSessionByToken(neverExpiringToken.getHashedValue());
    }
    void removeUsers() throws SQLException {
        userRepo.delete(testUserId);
    }

    @AfterAll
    static void closeDataSource() {
        if (ds instanceof HikariDataSource hds) hds.close();
    }
}
