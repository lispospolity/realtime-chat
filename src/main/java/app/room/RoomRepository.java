package app.room;

import jakarta.annotation.Nullable;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Repository
public class RoomRepository {
    private final DataSource dataSource;

    public RoomRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public int create(String ownerId, long timeNow) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO rooms(created_by, created_at)
                     VALUES(?, ?);
                     """, PreparedStatement.RETURN_GENERATED_KEYS)
        ) {
            statement.setString(1, ownerId);
            statement.setLong(2, timeNow);

            statement.executeUpdate();
            try (ResultSet resultSet = statement.getGeneratedKeys()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1);
                }
                throw new SQLException("Room created by " + ownerId + ", but no generated ID was returned");
            }
        }
    }

    public void deleteRoom(int roomId) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     DELETE FROM rooms WHERE room_id = ?;
                     """)
        ) {
            statement.setInt(1, roomId);

            statement.executeUpdate();
        }
    }

    public @Nullable String getOwner(int roomId) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT created_by FROM rooms WHERE
                     room_id = ?;
                     """)
        ) {
            statement.setInt(1, roomId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getString(1);
                }
                return null;
            }
        }
    }


    public List<Integer> getUserOwnedRooms(String userId) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT room_id FROM rooms WHERE
                     created_by = ?;
                     """)
        ) {
            statement.setString(1, userId);

            try (ResultSet resultSet = statement.executeQuery()) {
                List<Integer> rooms = new ArrayList<>();
                while (resultSet.next()) {
                    rooms.add(resultSet.getInt(1));
                }
                return rooms;
            }
        }
    }

    /**
     * Will not call a query when roomId is negative, because roomId in the database is an unsigned bit.
     */
    public boolean roomExists(int roomId) throws SQLException {
        if (roomId < 0) return false;

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT room_id FROM rooms WHERE
                     room_id = ?;
                     """)
        ) {
            statement.setInt(1, roomId);

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }
}
