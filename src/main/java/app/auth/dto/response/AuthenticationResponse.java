package app.auth.dto.response;

import jakarta.validation.constraints.NotNull;

public record AuthenticationResponse(
        boolean success,
        @NotNull String userId,
        String message
) {}
