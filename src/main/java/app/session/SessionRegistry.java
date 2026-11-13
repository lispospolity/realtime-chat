package app.session;

import app.config.AppConfig;
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


    private final ConcurrentHashMap<Token, Session> sessionsByToken = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Session> sessionsById = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Set<Session>> sessionsByUserId = new ConcurrentHashMap<>();

    public SessionRegistry(AppConfig appConfig, SessionRepository sessionRepo) {
        this.tokenExpiryInMillis = appConfig.tokenExpiryInMillis;
        this.sessionRepo = sessionRepo;
    }

    public void addSession(Token token, String sessionId, String userId) throws SQLException, NoSuchAlgorithmException {
        if (sessionsByToken.containsKey(token) || sessionsById.containsKey(sessionId)) throw new SQLException("Unique sessionKey constraint broken.");

        long timeNow = System.currentTimeMillis();
        sessionRepo.newSession(sessionId, userId, token.getHashedValue(), timeNow, timeNow + tokenExpiryInMillis);

        Session session = new Session(token, userId, sessionId, timeNow + tokenExpiryInMillis);

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
        Token token = new Token(sessionKey);

        Session session = sessionsByToken.get(token);
        if (session != null) {
            if (!session.expired()) return session;

            removeSessionFromCache(session);
            return session;
        }

        if (sessionRepo.tokenExpired(token.getHashedValue(), System.currentTimeMillis())) return null;

        SessionRecord record = sessionRepo.getSessionBySessionKey(token.getHashedValue());

        if (record == null) return null;
        session = new Session(token, record.userId(), record.sessionId(), record.expiresAt());

        addSessionToCache(session);

        return session;
    }


    private void addSessionToCache(@Nullable Session session) {
        if (session == null) return;

        sessionsByToken.put(session.getSessionToken(), session);
        sessionsById.put(session.getSessionId(), session);

        sessionsByUserId
                .computeIfAbsent(session.getUserId(), key -> ConcurrentHashMap.newKeySet())
                .add(session);
    }

    private void removeSessionFromCache(@Nullable Session session) {
        if (session == null) return;
        sessionsByToken.remove(session.getSessionToken(), session);
        sessionsById.remove(session.getSessionId(), session);

        Set<Session> sessions = sessionsByUserId.get(session.getUserId());
        if (sessions == null) return;

        sessions.removeIf(sesh -> session.getSessionId().equals(sesh.getSessionId()));
        if (sessions.isEmpty()) {
            sessionsByUserId.remove(session.getUserId(), sessions);
        }
    }
}
