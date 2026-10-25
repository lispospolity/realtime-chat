package app.auth;

import app.auth.dto.Result;
import app.auth.dto.request.LoginRequest;
import app.auth.dto.request.RegisterRequest;

import app.dto.response.ErrorResponse;
import app.dto.response.GenericResponse;
import app.dto.response.LoginResponse;
import jakarta.validation.Valid;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
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

        if (!(result.response() instanceof LoginResponse response)) {
            return ResponseEntity
                    .status(result.httpCode())
                    .body(result.response());
        }

        ResponseCookie tokenCookie = createCookie(
                "session_token",
                response.token(),
                response.expiresInSeconds(),
                true
        );
        ResponseCookie sessionCookie = createCookie(
                "session_id",
                response.sessionId(),
                response.expiresInSeconds(),
                false
        );

        return ResponseEntity
                .status(result.httpCode())
                .header(HttpHeaders.SET_COOKIE, tokenCookie.toString())
                .header(HttpHeaders.SET_COOKIE, sessionCookie.toString())
                .body(new GenericResponse(response.success(), response.message()));
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

    @DeleteMapping ("/api/sessions/me")
    public ResponseEntity<?> logoutMe(
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
        Result<?> result = authService.logout(sessionToken, null);

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


    private ResponseCookie createCookie(
            String name,
            String value,
            long maxAge,
            boolean httpOnly
    ) {
        return ResponseCookie.from(name, value)
                .httpOnly(httpOnly)
                .path("/")
                .sameSite("Lax")
                .maxAge(maxAge)
                .build();
    }
}
