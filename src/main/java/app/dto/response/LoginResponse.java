package app.dto.response;

public record LoginResponse(
        boolean success,
        String token,
        String sessionId,
        Long expiresInSeconds,
        String message
) {}