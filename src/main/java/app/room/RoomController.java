package app.room;

import app.auth.AuthService;
import app.dto.Result;
import app.auth.dto.response.AuthenticationResponse;
import app.user.UserService;
import app.user.dto.UsernameByIdResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class RoomController {
    private final AuthService authService;
    private final UserService userService;
    private final RoomService roomService;

    public RoomController(AuthService authService, UserService userService, RoomService roomService) {
        this.authService = authService;
        this.userService = userService;
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

    @GetMapping("/api/rooms/{id}/users")
    public ResponseEntity<?> getRoomMembers(@CookieValue(name = "session_token", required = false) String sessionToken,
                                            @PathVariable(value = "id", required = true) int roomId
    ) {
        Result authResult = authService.authenticate(sessionToken);
        if (!(authResult.response() instanceof AuthenticationResponse authResponse) || !authResponse.success()) {
            return ResponseEntity
                    .status(authResult.httpCode())
                    .body(authResult.response());
        }
        String userId = authResponse.userId();

        Result roomResult = roomService.getRoomMembers(userId, roomId);
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

    @PostMapping("/api/rooms/{roomId}/users/{userId}")
    public ResponseEntity<?> addToRoom(@CookieValue(name = "session_token", required = false) String sessionToken,
                                       @PathVariable(value = "roomId", required = true) int roomId,
                                       @PathVariable(value = "userId", required = true) String newMemberId,
                                       @RequestParam(name = "perms", defaultValue = "0") int perms
    ) {
        Result authResult = authService.authenticate(sessionToken);
        if (!(authResult.response() instanceof AuthenticationResponse authResponse) || !authResponse.success()) {
            return ResponseEntity
                    .status(authResult.httpCode())
                    .body(authResult.response());
        }
        String userId = authResponse.userId();

        Result userResult = userService.getUsernameById(newMemberId);
        if (!(userResult.response() instanceof UsernameByIdResponse)) {
            return ResponseEntity
                    .status(userResult.httpCode())
                    .body(userResult.response());
        }

        Result roomResult = roomService.addToRoom(userId, roomId, newMemberId, perms);
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