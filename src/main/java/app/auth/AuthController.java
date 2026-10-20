package app.auth;

import app.auth.dto.Result;
import app.auth.dto.request.LoginRequest;
import app.auth.dto.request.RegisterRequest;

import app.auth.dto.response.ErrorResponse;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping ("/api/servercheck")
    public ResponseEntity<?> serverCheck() {
        return ResponseEntity.ok().build();
    }

    @PostMapping ("/api/users")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest request) {
        Result<?> result = authService.register(request.username(), request.password());
        return ResponseEntity
                .status(result.httpCode())
                .body(result.response());
    }


    @PostMapping ("/api/sessions")
    public ResponseEntity<?>  loginUser(@Valid @RequestBody LoginRequest request) {
        Result<?> result = authService.login(request.username(), request.password());
        return ResponseEntity
                .status(result.httpCode())
                .body(result.response());
    }


    @DeleteMapping ("/api/sessions/{id}")
    public ResponseEntity<?> logoutSession(
            @PathVariable("id") String sessionId,
            @CookieValue(name = "session_token", required = false)
            String sessionToken
    ) {
        if (sessionToken == null) {
            return ResponseEntity
                    .status(401)
                    .body(new ErrorResponse(
                            false,
                            "Unauthorized access: session_token is null"));
        }
        Result<?> result = authService.logout(sessionToken, sessionId);
        return ResponseEntity
                .status(result.httpCode())
                .body(result.response());
    }


    @DeleteMapping ("/api/sessions")
    public ResponseEntity<?> logoutSessions(
            @CookieValue(name = "session_token", required = false)
            String sessionToken
    ) {
        if (sessionToken == null) {
            return ResponseEntity
                    .status(401)
                    .body(new ErrorResponse(
                            false,
                            "Unauthorized access: session_token is null"));
        }
        Result<?> result = authService.logoutAll(sessionToken);
        return ResponseEntity
                .status(result.httpCode())
                .body(result.response());
    }
}
