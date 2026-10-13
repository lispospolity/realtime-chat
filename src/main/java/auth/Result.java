package auth;

public record Result<T>(
        int httpCode,
        T response
) {
}