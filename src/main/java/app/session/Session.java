package app.session;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;

/**
 * expiresAt as null is a never expiring session
 */
public class Session {
    private Token token;
    public Token getSessionToken() { return token; }

    private String userId;
    public String getUserId() { return userId; }

    private String sessionId;
    public String getSessionId() { return sessionId; }

    private Long expiresAtMillis;
    public Long getExpiresAtMillis() { return expiresAtMillis; }

    public Session(@NotNull Token token,
                   @NotNull String userId,
                   @NotNull String sessionId,
                   @Nullable Long expiresAtMillis) {
        this.token = token;
        this.userId = userId;
        this.sessionId = sessionId;
        this.expiresAtMillis = expiresAtMillis;
    }

    public boolean expired() {
        return this.expiresAtMillis != null && expiresAtMillis <= System.currentTimeMillis();
    }
}
