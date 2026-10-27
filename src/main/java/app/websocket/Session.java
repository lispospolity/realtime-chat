package app.websocket;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;

/**
 * expiresAt as null is a never expiring session
 */
public record Session(
        @NotNull String userId,
        @NotNull String sessionId,
        @Nullable Long expiresAt
) {}
