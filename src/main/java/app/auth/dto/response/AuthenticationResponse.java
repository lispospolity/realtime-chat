package app.auth.dto.response;

import app.websocket.Session;
import jakarta.validation.constraints.NotNull;

public record AuthenticationResponse(
        boolean success,
        @NotNull Session session,
        String message
) {}
