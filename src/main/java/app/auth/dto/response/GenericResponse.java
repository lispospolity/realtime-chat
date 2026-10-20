package app.auth.dto.response;

public record GenericResponse(
        boolean success,
        String message
) {}
