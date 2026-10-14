package database.repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class SessionRepository {
    private final DataSource dataSource;

    public SessionRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void newSession(String userId, byte[] key, long createdAt, long expiresAt) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO user_sessions(id, sesh_key, created_at, expires_at)
                     VALUES(?, ?, ?, ?);
                     ON DUPLICATE KEY UPDATE
                     sesh_key   = ?,
                     created_at = ?,
                     expiresAt  = ?;
                     """)
        ) {
            statement.setString(1, userId);
            statement.setBytes(2, key);
            statement.setLong(3, createdAt);
            statement.setLong(4, expiresAt);

            statement.setBytes(5, key);
            statement.setLong(6, createdAt);
            statement.setLong(7, expiresAt);

            statement.executeUpdate();
        }
    }

    public void deleteSessionsByUserId(String userId) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                    DELETE FROM user_sessions WHERE id = ?;
                    """)
        ) {
            statement.setString(1, userId);

            statement.executeUpdate();
        }
    }

    public void deleteSessionByToken(byte[] token) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                    DELETE FROM user_sessions WHERE sesh_key = ?;
                    """)
        ) {
            statement.setBytes(1, token);

            statement.executeUpdate();
        }
    }
}
