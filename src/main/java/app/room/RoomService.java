package app.room;

import app.logging.Debug;
import org.springframework.stereotype.Service;

import java.sql.SQLException;

@Service
public class RoomService {
    private final RoomRepository roomRepo;

    public RoomService(RoomRepository roomRepo) {
        this.roomRepo = roomRepo;
    }

    public int getUserPerms(int roomId, String userId) {
        try {
            return roomRepo.getUserPermissions(roomId, userId);
        } catch (SQLException ex) {
            Debug.log("RoomService: getUserPerms: "+ex);
            return 0;
        }
    }
}
