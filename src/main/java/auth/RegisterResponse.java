package auth;

public record RegisterResponse(
        boolean success,
        String message
) {}
