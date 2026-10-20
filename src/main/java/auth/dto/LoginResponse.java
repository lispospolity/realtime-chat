package auth.dto;

public record LoginResponse(
        boolean success,
        String token,
        String sessionId,
        String message
) {}