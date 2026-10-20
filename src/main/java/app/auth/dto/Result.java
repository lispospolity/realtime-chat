package app.auth.dto;

public record Result<T>(
        int httpCode,
        T response
) {
}