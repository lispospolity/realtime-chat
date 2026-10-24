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
            ResultSet resultSet = statement.getGeneratedKeys();
            if (resultSet.next()) {
                return resultSet.getInt(1);
            }
            throw new SQLException("Room created by "+ownerId+", but no generated ID was returned");
        }
    }

    public void deleteRoom(int roomId) throws SQLException{
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

            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getString(1);
            }
            return null;
        }
    }

    /**
     * Permissions (column perms) work in the same way as Linux path rwx permissions:
     * 1) Read messages (first bit),
     * 2) Send message (second bit),
     * 4) Delete messages (third bit),
     * 8) Add/remove users (forth bit),
     * 16) Change user permissions (fifth bit).
     * Also found in Enum Permissions for better readability int 3
     */
    public void addToRoom(int roomId, String userId, int permissions) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO room_members(room_id, user_id, perms)
                     VALUES(?, ?, ?);
                     """)
        ) {
            statement.setInt(1, roomId);
            statement.setString(2, userId);
            statement.setInt(3, permissions);

            statement.executeUpdate();
        }
    }

    public void changePerms(int roomId, String userId, int newPerms) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     UPDATE room_members SET perms = ?
                     WHERE room_id = ?
                     AND user_id = ?;
                     """)
        ) {
            statement.setInt(1, newPerms);
            statement.setInt(2, roomId);
            statement.setString(3, userId);

            statement.executeUpdate();
        }
    }

    public void removeFromRoom(int roomId, String userId) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     DELETE FROM room_members
                     WHERE room_id = ?
                     AND user_id = ?;
                     """)
        ) {
            statement.setInt(1, roomId);
            statement.setString(2, userId);

            statement.executeUpdate();
        }
    }

    public List<Integer> getUserRooms(String userId) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT room_id FROM room_members WHERE
                     AND user_id = ?;
                     """)
        ) {
            statement.setString(1, userId);

            ResultSet resultSet = statement.executeQuery();

            List<Integer> rooms = new ArrayList<>();
            while (resultSet.next()) {
                rooms.add(resultSet.getInt(1));
            }
            return rooms;
        }
    }

    public int getUserPermissions(int roomId, String userId) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT perms FROM room_members WHERE
                     room_id = ?
                     AND user_id = ?;
                     """)
        ) {
            statement.setInt(1, roomId);
            statement.setString(2, userId);

            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getInt(1);
            }
            return 0;
        }
    }
}
