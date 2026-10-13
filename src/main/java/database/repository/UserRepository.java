package database.repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserRepository {
    private final DataSource dataSource;

    public UserRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void create(String id, String name, String hash, Long createdAt) throws SQLException {
        Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement("""
                 INSERT INTO users(id, username, password, createdAt)
                 VALUES(?, ?, ?, ?);
                 """);
        statement.setString(1, id);
        statement.setString(2, name);
        statement.setString(3, hash);
        statement.setLong(4, createdAt);
        int rowsInserted = statement.executeUpdate();
    }


    public boolean userExists(String name) throws SQLException {
        Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement("""
                SELECT FROM users WHERE username = ?
                """);
        statement.setString(1, name);
        ResultSet resultSet = statement.executeQuery();
        return resultSet.next();
    }
}

