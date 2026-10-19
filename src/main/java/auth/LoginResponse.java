package auth;

public record LoginResponse(
        boolean success,
        String token,
        String sessionId,
        String message
) {}