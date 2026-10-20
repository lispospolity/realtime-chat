package app.auth.dto.response;

public record ErrorResponse(
        boolean success,
        String error
) {}