package app.room;

import app.auth.AuthService;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RoomController {
    private final AuthService authService;
    private final RoomService roomService;

    public RoomController(AuthService authService, RoomService roomService) {
        this.authService = authService;
        this.roomService = roomService;
    }


}
