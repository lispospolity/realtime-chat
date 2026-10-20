package app.auth.dto.response;

public record LoginResponse(
        boolean success,
        String token,
        String sessionId,
        String message
) {}