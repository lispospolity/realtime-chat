package app.websocket;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;

public record Session(
        @NotNull String userId,
        @Nullable Long expiresIn
) {}
