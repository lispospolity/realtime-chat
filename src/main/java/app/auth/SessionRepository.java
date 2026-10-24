package app.auth;

import jakarta.annotation.Nullable;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;

@Repository
public class SessionRepository {
    private final DataSource dataSource;

    public SessionRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void newSession(String sessionId, String userId, byte[] key, long createdAt, Long expiresAt) throws SQLException {
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
            if (expiresAt == null) statement.setNull(5, Types.BIGINT);
            else statement.setLong(5, expiresAt);

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

    public void deleteSessionBySessionId(String sessionId) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                    DELETE FROM user_sessions WHERE session_id = ?;
                    """)
        ) {
            statement.setString(1, sessionId);

            statement.executeUpdate();
        }
    }

    public @Nullable String getUserIdBySessionKey(byte[] sessionKey) throws SQLException {
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

    public @Nullable String getSessionIdBySessionKey(byte[] sessionKey) throws SQLException {
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

    public @Nullable Long getTokenExpiry(byte[] sessionKey) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT expires_at FROM user_sessions
                     WHERE sesh_key = ?;
                     """)
        ) {
            statement.setBytes(1, sessionKey);

            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) return resultSet.getLong(1);
            return 0L;
        }
    }

    public boolean sessionOwnerCheck(String sessionId, String userId) throws SQLException {
        try (Connection connection = dataSource.getConnection();
            PreparedStatement statement = connection.prepareStatement("""
                    SELECT * FROM user_sessions WHERE
                    session_id = ?
                    AND user_id = ?
                    """)
        ) {
            statement.setString(1, sessionId);
            statement.setString(2, userId);

            ResultSet resultSet = statement.executeQuery();
            return resultSet.next();
        }
    }
}
