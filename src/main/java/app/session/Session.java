package app.session;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;

/**
 * expiresAt as null is a never expiring session
 */
public class Session {
    private String sessionKey;
    public String getSessionKey() { return sessionKey; }

    private String userId;
    public String getUserId() { return userId; }

    private String sessionId;
    public String getSessionId() { return sessionId; }

    private Long expiresAtMillis;
    public Long getExpiresAtMillis() { return expiresAtMillis; }

    public Session(@NotNull String sessionKey,
                   @NotNull String userId,
                   @NotNull String sessionId,
                   @Nullable Long expiresAtMillis) {
        this.sessionKey = sessionKey;
        this.userId = userId;
        this.sessionId = sessionId;
        this.expiresAtMillis = expiresAtMillis;
    }

    public boolean expired() {
        return this.expiresAtMillis != null && expiresAtMillis <= System.currentTimeMillis();
    }
}
