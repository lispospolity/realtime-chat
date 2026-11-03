package app.auth.dto.response;

import app.dto.response.Response;

public record LoginResponse(
        boolean success,
        String token,
        String sessionId,
        Long expiresInSeconds,
        String message
) implements Response {}