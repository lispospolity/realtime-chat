package app.websocket;

import jakarta.validation.constraints.NotNull;

public record Session(
        @NotNull String userId,
        long expiresIn
) {}
