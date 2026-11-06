package app.room;

import app.auth.AuthService;
import app.auth.dto.Result;
import app.auth.dto.response.AuthenticationResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class RoomController {
    private final AuthService authService;
    private final RoomService roomService;

    public RoomController(AuthService authService, RoomService roomService) {
        this.authService = authService;
        this.roomService = roomService;
    }

    @GetMapping("/api/rooms")
    public ResponseEntity<?> getRooms(@CookieValue(name = "session_token", required = false) String sessionToken,
                                      @RequestParam(name = "owner", defaultValue = "false") boolean owner
    ) {
        Result authResult = authService.authenticate(sessionToken);
        if (!(authResult.response() instanceof AuthenticationResponse authResponse) || !authResponse.success()) {
            return ResponseEntity
                    .status(authResult.httpCode())
                    .body(authResult.response());
        }
        String userId = authResponse.userId();

        if (owner) {
            Result roomResult = roomService.getUserOwnedRooms(userId);
            return ResponseEntity
                    .status(roomResult.httpCode())
                    .body(roomResult.response());
        }
        Result roomResult = roomService.getUserRooms(userId);
        return ResponseEntity
                .status(roomResult.httpCode())
                .body(roomResult.response());
    }

    @PostMapping("/api/rooms")
    public ResponseEntity<?> createRoom(@CookieValue(name = "session_token", required = false) String sessionToken) {
        Result authResult = authService.authenticate(sessionToken);
        if (!(authResult.response() instanceof AuthenticationResponse authResponse) || !authResponse.success()) {
            return ResponseEntity
                    .status(authResult.httpCode())
                    .body(authResult.response());
        }
        String userId = authResponse.userId();

        Result roomResult = roomService.createRoom(userId);
        return ResponseEntity
                .status(roomResult.httpCode())
                .body(roomResult.response());
    }

    @DeleteMapping("/api/rooms/{id}")
    public ResponseEntity<?> removeRoom(@CookieValue(name = "session_token", required = false) String sessionToken,
                                        @PathVariable(value = "id", required = true) int roomId
    ) {
        Result authResult = authService.authenticate(sessionToken);
        if (!(authResult.response() instanceof AuthenticationResponse authResponse) || !authResponse.success()) {
            return ResponseEntity
                    .status(authResult.httpCode())
                    .body(authResult.response());
        }
        String userId = authResponse.userId();

        Result roomResult = roomService.deleteRoom(userId, roomId);
        return ResponseEntity
                .status(roomResult.httpCode())
                .body(roomResult.response());
    }
}