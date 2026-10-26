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

    public Permissions getUserPerms(int roomId, String userId) {
        try {
            if (userId.equals(roomRepo.getOwner(roomId)))
                return new Permissions(Permissions.getOwnerPerms());
            else
                return new Permissions(roomRepo.getUserPermissions(roomId, userId));

        } catch (SQLException ex) {
            Debug.log("RoomService: getUserPerms: "+ex);
            return new Permissions(0);
        }
    }
}
