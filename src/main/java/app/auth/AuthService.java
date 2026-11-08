package app.auth;

import app.auth.dto.response.AuthenticationResponse;
import app.dto.response.ErrorResponse;
import app.dto.response.GenericResponse;
import app.auth.dto.response.LoginResponse;
import app.auth.dto.Result;
import app.config.AppConfig;
import app.session.SessionRepository;
import app.user.UserRepository;
import app.logging.Debug;

import static app.utils.TokenUtils.generateToken;
import static app.utils.TokenUtils.hashToken;

import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import app.utils.TokenUtils;
import app.session.Session;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Service;

import org.mindrot.jbcrypt.BCrypt;

@Service
public class AuthService {
    private static final String DUMMY_HASH = "$2a$10$nD7B2O4X8vIMPT/I0rIAwuB9DTuB8kHe4dAHk80KKGcs5jucRbR3a";

    private final int minNameLength;
    private final int maxNameLength;
    private final int minPasswordLength;
    private final int maxPasswordLength;

    private final long tokenExpiryInMillis;

    private final UserRepository userRepo;
    private final SessionRepository sessionRepo;

    private final ConcurrentHashMap<String, Session> sessions = new ConcurrentHashMap<>();

    public AuthService(AppConfig appConfig, UserRepository userRepository, SessionRepository sessionRepository) {
        minNameLength = appConfig.minNameLength;
        maxNameLength = appConfig.maxNameLength;
        minPasswordLength = appConfig.minPasswordLength;
        maxPasswordLength = appConfig.maxPasswordLength;
        tokenExpiryInMillis = appConfig.tokenExpiryInMillis;

        this.userRepo = userRepository;
        this.sessionRepo = sessionRepository;
    }

    public Result login(String username, String password) {
        String userId;
        String hash;
        try {
            userId = userRepo.getId(username);
            if (userId == null) {
                BCrypt.checkpw(password, DUMMY_HASH);

                var response = new ErrorResponse(
                        false,
                        "Username or password is incorrect.");
                return new Result(400, response);
            }
            hash = userRepo.getPassword(userId);
        } catch (SQLException ex) { return serverError(500, ex); }

        if (hash == null) return serverError(500, new NullPointerException("For some reason hash is null - impossible? Yeah i thought so too."));

        if (!BCrypt.checkpw(password, hash)) {
            var response = new ErrorResponse(
                    false,
                    "Username or password is incorrect.");
            return new Result(400, response);
        }

        //Retry loop for the absolutely unlikely (1/2^122) 500 error, which probably will never happen, but I wanted to make a check for it :)
        String token = "";
        String sessionId = "";
        long timeNow = 0;
        boolean created = false;
        for (int i = 0; i < 3; i++) {
            token = generateToken();
            byte[] hashedToken;
            try {
                hashedToken = hashToken(token);
            } catch (NoSuchAlgorithmException ex) { return serverError(500, ex); }
            sessionId = UUID.randomUUID().toString();
            timeNow = System.currentTimeMillis();

            try {
                sessionRepo.newSession(sessionId, userId, hashedToken, timeNow, timeNow + tokenExpiryInMillis);
                created = true;
            } catch (SQLIntegrityConstraintViolationException ex) {
                continue;
            } catch (SQLException ex) {
                return serverError(500, ex);
            }
        }

        // If this happens, go buy a lottery ticket. Actually, don't.
        // This is considerably less likely than you think.
        if (!created) return serverError(500, new SQLIntegrityConstraintViolationException(
                "Failed to generate unique session identifiers after 3 attempts. "
                + "At this point, the universe is probably messing with us."));

        Session session = new Session(userId, sessionId, timeNow + tokenExpiryInMillis);
        sessions.put(token, session);

        var response = new LoginResponse(
                true,
                token,
                sessionId,
                tokenExpiryInMillis/1000,
                "Login successful."
        );
        return new Result(200, response);
    }

    public Result logout(String sessionKey, @Nullable String sessionId) {
        Result result = authenticate(sessionKey);
        if (!(result.response() instanceof AuthenticationResponse authResponse)) { return result; }
        String owner = authResponse.userId();

        byte[] sessionHash;
        try {
            sessionHash = hashToken(sessionKey);

            if (sessionId == null) sessionId = sessionRepo.getSessionIdBySessionKey(sessionHash);

            if (!sessionRepo.sessionOwnerCheck(sessionId, owner)) {
                var response = new ErrorResponse(
                        false,
                        "Incorrect session id, or you're not the owner of this session."
                );
                return new Result(401, response);
            }

            sessionRepo.deleteSessionBySessionId(sessionId);

        } catch (NoSuchAlgorithmException | SQLException ex) { return serverError(500, ex); }

        sessions.remove(sessionKey);

        var response = new GenericResponse(
                true,
                "Logout successful."
        );
        return new Result(200, response);
    }

    public Result logoutAll(String sessionKey) {
        Result result = authenticate(sessionKey);
        if (!(result.response() instanceof AuthenticationResponse authResponse)) { return result; }
        String userId = authResponse.userId();

        try {
            sessionRepo.deleteSessionsByUserId(userId);
        } catch (SQLException ex) { return serverError(500, ex); }

        deleteUserSessions(userId);

        var response = new GenericResponse(
                true,
                "Logout successful."
        );
        return new Result(200, response);
    }

    public Result register(@Nullable String username, @Nullable String password) {
        if (username == null || password == null) {
            var response = new ErrorResponse(
                    false,
                    "Username, or password cannot be null.");
            return new Result(409, response);
        }

        boolean nameLengthCorrect = !username.isBlank() && username.length() >= minNameLength && username.length() <= maxNameLength;
        boolean passwordLengthCorrect = !password.isBlank() && password.length() >= minPasswordLength && password.length() <= maxPasswordLength;

        if (!passwordLengthCorrect) {
            var response = new ErrorResponse(
                    false,
                    "Password length is incorrect. Range: " + minPasswordLength + " - " + maxPasswordLength);
            return new Result(400, response);
        }
        if (!nameLengthCorrect) {
            var response = new ErrorResponse(
                    false,
                    "Name length is incorrect. Range: " + minNameLength + " - " + maxNameLength);
            return new Result(400, response);
        }

        boolean userExists;
        try {
            userExists = userRepo.getId(username) != null;
        } catch (SQLException ex) { return serverError(500, ex); }

        if (userExists) {
            var response = new ErrorResponse(
                    false,
                    "Username already exists.");
            return new Result(409, response);
        }

        String hash = BCrypt.hashpw(password, BCrypt.gensalt());

        //Similar "issue" like with session creation
        boolean created = false;
        for (int i = 0; i < 3; i++) {
            String userId = UUID.randomUUID().toString();
            try {
                userRepo.create(userId, username, hash, System.currentTimeMillis());
                created = true;
                break;
            } catch (SQLIntegrityConstraintViolationException ex) {
                if (ex.getErrorCode() != 1062) return serverError(500, ex);

                Debug.log("Well this has 1/2^122 chances of happening: " + ex);
                continue;
            } catch (SQLException ex) {
                return serverError(500, ex);
            }
        }
        if (!created) return serverError(500, new SQLIntegrityConstraintViolationException(
                "Similarly to session creation, this just doesn't happen, what did you do to piss off the man upstairs?"
        ));

        var response = new GenericResponse(
                true,
                "Account successfully made."
        );
        return new Result(201, response);
    }

    public Result authenticate(@Nullable String sessionKey) {
        if (sessionKey == null) {
            var response = new ErrorResponse(
                    false,
                    "Session token cannot be null."
            );
            return new Result(400, response);
        }

        if (!sessions.containsKey(sessionKey)) return dbAuthenticate(sessionKey);

        Session session = sessions.get(sessionKey);
        if (session == null || session.userId() == null) return dbAuthenticate(sessionKey);

        if (session.expiresAtMillis() != null && session.expiresAtMillis() <= System.currentTimeMillis()) {
            var response = new ErrorResponse(
                    false,
                    "Incorrect, or expired session key."
            );
            return new Result(400, response);
        }

        var response = new AuthenticationResponse(
                true,
                session.userId(),
                "Successful authentication."
        );
        return new Result(200, response);
    }

    private Result dbAuthenticate(String sessionKey) {
        if (sessionKey == null || sessionKey.isBlank()) {
            var response = new ErrorResponse(
                    false,
                    "Session key cookie cannot be empty.");
            return new Result(401, response);
        }

        try {
            byte[] tokenHash = TokenUtils.hashToken(sessionKey);

            if (sessionRepo.tokenExpired(tokenHash, System.currentTimeMillis())) {
                var response = new ErrorResponse(
                        false,
                        "Incorrect, or expired session key."
                );
                return new Result(400, response);
            }
            String userId = sessionRepo.getUserIdBySessionKey(tokenHash);
            if (userId == null) {
                return serverError(500, new NullPointerException("AuthService:authenticate(): For whatever fuckass reason userId is null."));
            }

            String sessionId = sessionRepo.getSessionIdBySessionKey(tokenHash);
            Long expiresAt = sessionRepo.getTokenExpiry(tokenHash);

            Session session = new Session(userId, sessionId, expiresAt);
            sessions.put(sessionKey, session);

            var response = new AuthenticationResponse(
                    true,
                    session.userId(),
                    "Successful authentication."
            );
            return new Result(200, response);
        } catch (NoSuchAlgorithmException | SQLException ex) { return serverError(500, ex); }
    }

    private void deleteUserSessions(String userId) {
        for (String seshKey : sessions.keySet()) {
            if (sessions.get(seshKey).userId().equals(userId))
                sessions.remove(seshKey);
        }
    }

    private Result serverError(int httpCode, Exception ex) {
        Debug.log("AuthService:generic serverError(): "+ex.toString());

        var response = new ErrorResponse(
                false,
                "Internal Server Error");
        return new Result(httpCode, response);
    }
}
