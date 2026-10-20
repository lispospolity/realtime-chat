package auth.dto;

public record ErrorResponse(
        boolean success,
        String error
) {}