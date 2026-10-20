package auth.dto;

public record Result<T>(
        int httpCode,
        T response
) {
}