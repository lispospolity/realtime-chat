package app.room;

import app.auth.AuthService;
import app.dto.Result;
import app.auth.dto.response.AuthenticationResponse;
import app.room.dto.request.RoomMemberConfigRequest;
import app.user.UserService;
import app.user.dto.UsernameByIdResponse;
import jakarta.validation.Valid;
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
                                            @PathVariable(value = "id") int roomId
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

    @DeleteMapping("/api/rooms/{id}")
    public ResponseEntity<?> removeRoom(@CookieValue(name = "session_token", required = false) String sessionToken,
                                        @PathVariable(value = "id") int roomId
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

    @PostMapping("/api/rooms/{roomId}/users/{userId}")
    public ResponseEntity<?> addMember(@CookieValue(name = "session_token", required = false) String sessionToken,
                                       @PathVariable(value = "roomId") int roomId,
                                       @PathVariable(value = "userId") String newMemberId,
                                       @Valid @RequestBody RoomMemberConfigRequest request
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

        Result roomResult = roomService.addToRoom(userId, roomId, newMemberId, request.permissions());
        return ResponseEntity
                .status(roomResult.httpCode())
                .body(roomResult.response());
    }

    @PatchMapping("/api/rooms/{roomId}/users/{userId}")
    public ResponseEntity<?> updateMember(@CookieValue(name = "session_token", required = false) String sessionToken,
                                          @PathVariable(value = "roomId") int roomId,
                                          @PathVariable(value = "userId") String existingMemberId,
                                          @Valid @RequestBody RoomMemberConfigRequest request
    ) {
        Result authResult = authService.authenticate(sessionToken);
        if (!(authResult.response() instanceof AuthenticationResponse authResponse) || !authResponse.success()) {
            return ResponseEntity
                    .status(authResult.httpCode())
                    .body(authResult.response());
        }
        String userId = authResponse.userId();

        Result userResult = userService.getUsernameById(existingMemberId);
        if (!(userResult.response() instanceof UsernameByIdResponse)) {
            return ResponseEntity
                    .status(userResult.httpCode())
                    .body(userResult.response());
        }

        Result roomResult = roomService.updateMember(userId, roomId, existingMemberId, request.permissions());
        return ResponseEntity
                .status(roomResult.httpCode())
                .body(roomResult.response());
    }

    @DeleteMapping("/api/rooms/{roomId}/users/{userId}")
    public ResponseEntity<?> removeMember(@CookieValue(name = "session_token", required = false) String sessionToken,
                                          @PathVariable(value = "roomId") int roomId,
                                          @PathVariable(value = "userId") String existingMemberId
    ) {
        Result authResult = authService.authenticate(sessionToken);
        if (!(authResult.response() instanceof AuthenticationResponse authResponse) || !authResponse.success()) {
            return ResponseEntity
                    .status(authResult.httpCode())
                    .body(authResult.response());
        }
        String userId = authResponse.userId();

        Result userResult = userService.getUsernameById(existingMemberId);
        if (!(userResult.response() instanceof UsernameByIdResponse)) {
            return ResponseEntity
                    .status(userResult.httpCode())
                    .body(userResult.response());
        }

        Result roomResult = roomService.removeMember(userId, roomId, existingMemberId);
        return ResponseEntity
                .status(roomResult.httpCode())
                .body(roomResult.response());
    }

}