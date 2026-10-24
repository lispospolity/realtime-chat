package app.user;

import jakarta.annotation.Nullable;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Repository
public class UserRepository {
    private final DataSource dataSource;

    public UserRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void create(String id, String name, String hash, Long createdAt) throws SQLException {
        Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement("""
                 INSERT INTO users(id, username, password, createdat)
                 VALUES(?, ?, ?, ?);
                 """);
        statement.setString(1, id);
        statement.setString(2, name);
        statement.setString(3, hash);
        statement.setLong(4, createdAt);

        statement.executeUpdate();
    }

    public void delete(String id) throws SQLException {
        Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement("""
                DELETE FROM users WHERE id = ?;
                """);
        statement.setString(1, id);

        statement.executeUpdate();
    }

    public @Nullable String getId(String name) throws SQLException {
        try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement("""
                SELECT id FROM users WHERE username = ?;
                """)
        ) {
            statement.setString(1, name);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) return resultSet.getString("id");
            return null;
        }
    }

    public @Nullable String getPassword(String id) throws SQLException {
        try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement("""
                SELECT password FROM users WHERE id = ?;
                """)
        ) {
            statement.setString(1, id);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) return resultSet.getString("password");
            return null;
        }
    }
}

