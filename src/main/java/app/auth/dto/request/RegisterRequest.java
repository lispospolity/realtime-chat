package app.auth.dto.request;

import app.dto.request.Request;
import jakarta.validation.constraints.NotNull;

public record RegisterRequest(
        @NotNull String username,
        @NotNull String password
) implements Request {}
