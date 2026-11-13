package app.auth;

import app.auth.dto.response.AuthenticationResponse;
import app.dto.response.ErrorResponse;
import app.dto.response.GenericResponse;
import app.auth.dto.response.LoginResponse;
import app.dto.Result;
import app.config.AppConfig;
import app.session.SessionRegistry;
import app.session.SessionRepository;
import app.session.Token;
import app.user.UserRepository;
import app.logging.Debug;

import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.UUID;

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

    private final SessionRegistry sessionRegistry;

    public AuthService(AppConfig appConfig, UserRepository userRepository, SessionRegistry sessionRegistry) {
        minNameLength = appConfig.minNameLength;
        maxNameLength = appConfig.maxNameLength;
        minPasswordLength = appConfig.minPasswordLength;
        maxPasswordLength = appConfig.maxPasswordLength;
        tokenExpiryInMillis = appConfig.tokenExpiryInMillis;

        this.userRepo = userRepository;
        this.sessionRegistry = sessionRegistry;
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

        for (int i = 0; i < 3; i++) {
            String sessionId = UUID.randomUUID().toString();

            try {
                Token token = new Token();

                sessionRegistry.addSession(token, sessionId, userId);

                var response = new LoginResponse(
                        true,
                        token.getValue(),
                        sessionId,
                        tokenExpiryInMillis/1000,
                        "Login successful."
                );
                return new Result(200, response);
            } catch (SQLIntegrityConstraintViolationException ex) {
                if (ex.getErrorCode() != 1062) return serverError(500, ex);

                Debug.log("Well that's unexpected: " + ex);
            } catch (NoSuchAlgorithmException | SQLException ex) {
                return serverError(500, ex);
            }
        }

        // If this gets through, go buy a lottery ticket. Actually, don't.
        // This is considerably less likely than you think.
        return serverError(500, new SQLIntegrityConstraintViolationException(
                "Failed to generate unique session identifiers after 3 attempts. "
                + "At this point, the universe is probably messing with us."));
    }

    public Result logout(String sessionKey, @Nullable String sessionId) {
        Result result = authenticate(sessionKey);
        if (!(result.response() instanceof AuthenticationResponse authResponse)) { return result; }
        Session session = authResponse.session();

        try {
            if (sessionId == null) sessionId = session.getSessionId();

            if (!sessionRegistry.sessionOwnerCheck(sessionId, session.getUserId())) {
                var response = new ErrorResponse(
                        false,
                        "Incorrect session id, or you're not the owner of this session."
                );
                return new Result(401, response);
            }

            sessionRegistry.removeSessionBySessionId(sessionId);

        } catch (SQLException ex) { return serverError(500, ex); }

        var response = new GenericResponse(
                true,
                "Logout successful."
        );
        return new Result(200, response);
    }

    public Result logoutAll(String sessionKey) {
        Result result = authenticate(sessionKey);
        if (!(result.response() instanceof AuthenticationResponse authResponse)) { return result; }
        Session session = authResponse.session();

        try {
            sessionRegistry.removeAllUserSessions(session);
        } catch (NoSuchAlgorithmException | SQLException ex) { return serverError(500, ex); }

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
        if (sessionKey == null || sessionKey.isBlank()) {
            var response = new ErrorResponse(
                    false,
                    "Session token cannot be null."
            );
            return new Result(400, response);
        }

        Session session;
        try {
            session = sessionRegistry.getSessionByKey(sessionKey);
        } catch (NoSuchAlgorithmException | SQLException ex) { return serverError(500, ex); }

        if (session == null || session.expired()) {
            var response = new ErrorResponse(
                    false,
                    "Incorrect, or expired session key."
            );
            return new Result(400, response);
        }

        var response = new AuthenticationResponse(
                true,
                session,
                "Successful authentication."
        );
        return new Result(200, response);
    }

    private Result serverError(int httpCode, Exception ex) {
        Debug.log("AuthService:generic serverError(): "+ex.toString());

        var response = new ErrorResponse(
                false,
                "Internal Server Error");
        return new Result(httpCode, response);
    }
}