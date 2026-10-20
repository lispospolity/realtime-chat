package app.auth.dto.request;

import jakarta.validation.constraints.NotNull;

public record RegisterRequest(
        @NotNull String username,
        @NotNull String password
) {}
