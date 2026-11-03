package app.auth.dto.response;

import app.dto.response.Response;
import jakarta.validation.constraints.NotNull;

public record AuthenticationResponse(
        boolean success,
        @NotNull String userId,
        String message
) implements Response {}
