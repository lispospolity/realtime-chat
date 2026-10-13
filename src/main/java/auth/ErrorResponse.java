package auth;

public record ErrorResponse(
        boolean success,
        String error
) {}