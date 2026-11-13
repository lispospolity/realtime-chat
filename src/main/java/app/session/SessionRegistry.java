package app.session;

import app.config.AppConfig;
import app.utils.TokenUtils;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Component;

import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SessionRegistry {
    private final long tokenExpiryInMillis;

    private final SessionRepository sessionRepo;


    private final ConcurrentHashMap<String, Session> sessionsByKey = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Session> sessionsById = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Set<Session>> sessionsByUserId = new ConcurrentHashMap<>();

    public SessionRegistry(AppConfig appConfig, SessionRepository sessionRepo) {
        this.tokenExpiryInMillis = appConfig.tokenExpiryInMillis;
        this.sessionRepo = sessionRepo;
    }

    public void addSession(String sessionKey, String sessionId, String userId) throws SQLException, NoSuchAlgorithmException {
        if (sessionsByKey.containsKey(sessionKey) || sessionsById.containsKey(sessionId)) throw new SQLException("Unique sessionKey constraint broken.");

        long timeNow = System.currentTimeMillis();
        byte[] hashedToken = TokenUtils.hashToken(sessionKey);
        sessionRepo.newSession(sessionId, userId, hashedToken, timeNow, timeNow + tokenExpiryInMillis);

        Session session = new Session(sessionKey, userId, sessionId, timeNow + tokenExpiryInMillis);

        addSessionToCache(session);
    }

    public void removeSessionBySessionId(@NotNull String sessionId) throws SQLException {
        sessionRepo.deleteSessionBySessionId(sessionId);

        Session session = sessionsById.remove(sessionId);
        removeSessionFromCache(session);
    }

    public void removeAllUserSessions(@NotNull Session session) throws SQLException, NoSuchAlgorithmException {
        sessionRepo.deleteSessionsByUserId(session.getUserId());

        Set<Session> userSessions = sessionsByUserId.get(session.getUserId());
        if (userSessions == null) return;

        for (Session sesh : Set.copyOf(userSessions)) {
            removeSessionFromCache(sesh);
        }

        sessionsByUserId.remove(session.getUserId(), userSessions);
    }

    public @Nullable Session getSessionByKey(@NotNull String sessionKey) throws SQLException, NoSuchAlgorithmException {
        Session session = sessionsByKey.get(sessionKey);
        if (session != null) {
            if (!session.expired()) return session;

            removeSessionFromCache(session);
            return session;
        }

        byte[] hash = TokenUtils.hashToken(sessionKey);
        if (sessionRepo.tokenExpired(hash, System.currentTimeMillis())) return null;

        String userId = sessionRepo.getUserIdBySessionKey(hash);
        String sessionId = sessionRepo.getSessionIdBySessionKey(hash);
        Long expiresAt = sessionRepo.getTokenExpiry(hash);

        if (userId == null || sessionId == null) return null;
        session = new Session(sessionKey, userId, sessionId, expiresAt);

        addSessionToCache(session);

        return session;
    }


    private void addSessionToCache(@Nullable Session session) {
        if (session == null) return;

        sessionsByKey.put(session.getSessionKey(), session);
        sessionsById.put(session.getSessionId(), session);

        sessionsByUserId
                .computeIfAbsent(session.getUserId(), key -> ConcurrentHashMap.newKeySet())
                .add(session);
    }

    private void removeSessionFromCache(@Nullable Session session) {
        if (session == null) return;
        sessionsByKey.remove(session.getSessionKey(), session);
        sessionsById.remove(session.getSessionId(), session);

        Set<Session> sessions = sessionsByUserId.get(session.getUserId());
        if (sessions == null) return;

        sessions.removeIf(sesh -> session.getSessionId().equals(sesh.getSessionId()));
        if (sessions.isEmpty()) {
            sessionsByUserId.remove(session.getUserId(), sessions);
        }
    }
}
