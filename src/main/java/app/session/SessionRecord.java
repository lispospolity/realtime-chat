package app.session;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;

public record SessionRecord(
        @NotNull String sessionId,
        @NotNull String userId,
        @NotNull byte[] sessionKey,
        @Nullable Long expiresAt
) {}
