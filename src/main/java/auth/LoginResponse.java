package auth;

public record LoginResponse(
        boolean success,
        String token,
        String message
) {}