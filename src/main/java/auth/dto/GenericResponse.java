package auth.dto;

public record GenericResponse(
        boolean success,
        String message
) {}
