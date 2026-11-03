package app.dto.response;

public record ErrorResponse(
        boolean success,
        String error
) implements Response {}