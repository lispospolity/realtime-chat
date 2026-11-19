package app.messages;

import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Repository
public class MessageRepository {
    private final DataSource dataSource;

    public MessageRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void create(String messageId, int roomId, String senderId, String content, long serverReceivedAt) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO messages(message_id, room_id, sent_by, content, sent_at)
                     VALUES (?, ?, ?, ?, ?);
                     """)
        ) {
            statement.setString(1, messageId);
            statement.setInt(2, roomId);
            statement.setString(3, senderId);
            statement.setString(4, content);
            statement.setLong(5, serverReceivedAt);

            statement.executeUpdate();
        }
    }

    public boolean messageExists(String messageId) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT * FROM messages
                     WHERE message_id = ?;
                     """)
        ) {
            statement.setString(1, messageId);

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    public void delete(String messageId) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     DELETE FROM messages
                     WHERE message_id = ?;
                     """)
        ) {
            statement.setString(1, messageId);

            statement.executeUpdate();
        }
    }
}
