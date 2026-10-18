package database.repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SessionRepository {
    private final DataSource dataSource;

    public SessionRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void newSession(String sessionId, String userId, byte[] key, long createdAt, long expiresAt) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO user_sessions(session_id, user_id, sesh_key, created_at, expires_at)
                     VALUES(?, ?, ?, ?, ?);
                     """)
        ) {
            statement.setString(1, sessionId);
            statement.setString(2, userId);
            statement.setBytes(3, key);
            statement.setLong(4, createdAt);
            statement.setLong(5, expiresAt);

            statement.executeUpdate();
        }
    }

    public void deleteSessionsByUserId(String userId) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                    DELETE FROM user_sessions WHERE user_id = ?;
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

    public String getUserIdBySessionKey(byte[] sessionKey) throws SQLException {
        try (Connection connection = dataSource.getConnection();
            PreparedStatement statement = connection.prepareStatement("""
                    SELECT user_id FROM user_sessions WHERE sesh_key = ?;
                    """)
        ) {
            statement.setBytes(1, sessionKey);

            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) return resultSet.getString("user_id");
            return null;
        }
    }

    public String getSessionIdBySessionKey(byte[] sessionKey) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                    SELECT session_id FROM user_sessions WHERE sesh_key = ?;
                    """)
        ) {
            statement.setBytes(1, sessionKey);

            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) return resultSet.getString("session_id");
            return null;
        }
    }

    public boolean tokenExpired(byte[] sessionKey, Long timeNow) throws SQLException {
        try (Connection connection = dataSource.getConnection();
            PreparedStatement statement = connection.prepareStatement("""
                    SELECT * FROM user_sessions WHERE
                    sesh_key = ?
                    AND (
                    expires_at IS NULL
                    OR expires_at > ?
                    );
                    """)
        ) {
            statement.setBytes(1, sessionKey);
            statement.setLong(2, timeNow);

            ResultSet resultSet = statement.executeQuery();
            return !resultSet.next();
        }
    }
}
