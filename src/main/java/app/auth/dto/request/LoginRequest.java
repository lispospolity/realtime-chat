package app.auth.dto.request;

import app.dto.request.Request;
import jakarta.validation.constraints.NotNull;

public record LoginRequest(
        @NotNull String username,
        @NotNull String password
) implements Request {}
