package database;

import app.config.AppConfig;
import app.config.Env;
import app.database.DataSourceFactory;
import app.messages.Message;
import app.messages.MessageRepository;
import app.room.RoomRepository;
import app.user.UserRepository;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.*;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.UUID;

public class MessageRepoTests {
    private final static Path testEnvDir = Paths.get("src/test/resources/.env.db.test");
    private static DataSource dataSource;

    private UserRepository userRepo;
    private RoomRepository roomRepo;

    private MessageRepository messageRepo;

    private String userId;
    private int roomId;

    private Message existingMessage;
    private Message newMessage;

    @BeforeAll
    static void setupDatabase() throws IOException {
        Env env = new Env(testEnvDir, null);
        AppConfig testConfig = new AppConfig(env);
        dataSource = DataSourceFactory.create(testConfig);
    }

    @BeforeEach
    void setupRepos() throws SQLException {
        setupUsers();
        setupRooms();

        setupMessages();
    }
    void setupUsers() throws SQLException {
        userRepo = new UserRepository(dataSource);

        userId = UUID.randomUUID().toString();
        userRepo.create(userId, "message_repo_test_user", "qwertyuiop", System.currentTimeMillis());
    }
    void setupRooms() throws SQLException {
        roomRepo = new RoomRepository(dataSource);

        roomId = roomRepo.create(userId, System.currentTimeMillis());
    }
    void setupMessages() throws SQLException {
        messageRepo = new MessageRepository(dataSource);

        existingMessage = new Message(roomId, userId, "test message");
        messageRepo.create(
                existingMessage.getMessageId(),
                existingMessage.getRoomId(),
                existingMessage.getSenderId(),
                existingMessage.getContent(),
                existingMessage.getServerReceivedAt()
                );
    }

    @Test
    void deleteMessage_existing() throws SQLException {
        Assertions.assertTrue(messageRepo.messageExists(existingMessage.getMessageId()));

        messageRepo.delete(existingMessage.getMessageId());

        Assertions.assertFalse(messageRepo.messageExists(existingMessage.getMessageId()));
    }

    @Test
    void createMessage_existing() throws SQLException {
        Assertions.assertTrue(messageRepo.messageExists(existingMessage.getMessageId()));

        Assertions.assertThrows(
                SQLIntegrityConstraintViolationException.class,
                () -> messageRepo.create(
                        existingMessage.getMessageId(),
                        existingMessage.getRoomId(),
                        existingMessage.getSenderId(),
                        existingMessage.getContent(),
                        existingMessage.getServerReceivedAt()
                ));
    }

    @AfterEach
    void cleanupTable() throws SQLException {
        deleteMessages();
        deleteRooms();
        deleteUsers();
    }
    void deleteMessages() throws SQLException {
        messageRepo.delete(existingMessage.getMessageId());

        if (newMessage == null) return;
        messageRepo.delete(newMessage.getMessageId());
    }
    void deleteRooms() throws SQLException {
        roomRepo.deleteRoom(roomId);
    }
    void deleteUsers() throws SQLException {
        userRepo.delete(userId);
    }

    @AfterAll
    static void closeDataSource() {
        if (dataSource instanceof HikariDataSource hds) hds.close();
    }
}
