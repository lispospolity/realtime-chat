package app.auth;

import app.dto.response.ErrorResponse;
import app.dto.response.GenericResponse;
import app.dto.response.LoginResponse;
import app.auth.dto.Result;
import app.config.AppConfig;
import app.user.UserRepository;
import app.logging.Debug;

import static app.utils.TokenUtils.generateToken;
import static app.utils.TokenUtils.hashToken;

import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.UUID;

import jakarta.annotation.Nullable;
import org.springframework.stereotype.Service;

import org.mindrot.jbcrypt.BCrypt;

@Service
public class AuthService {
    private final int minNameLength;
    private final int maxNameLength;
    private final int minPasswordLength;
    private final int maxPasswordLength;

    private final long tokenExpiryInMillis;

    private final UserRepository userRepo;
    private final SessionRepository sessionRepo;

    public AuthService(AppConfig appConfig, UserRepository userRepository, SessionRepository sessionRepository) {
        minNameLength = appConfig.minNameLength;
        maxNameLength = appConfig.maxNameLength;
        minPasswordLength = appConfig.minPasswordLength;
        maxPasswordLength = appConfig.maxPasswordLength;
        tokenExpiryInMillis = appConfig.tokenExpiryInMillis;

        this.userRepo = userRepository;
        this.sessionRepo = sessionRepository;
    }

    public Result<?> login(String username, String password) {
        String userId;
        String hash;
        try {
            userId = userRepo.getId(username);
            if (userId == null) {
                var response = new ErrorResponse(
                        false,
                        "Username or password is incorrect.");
                return new Result<ErrorResponse>(400, response);
            }
            hash = userRepo.getPassword(userId);
        } catch (SQLException ex) { return serverError(500, ex); }

        if (hash == null) return serverError(500, new NullPointerException("For some reason hash is null - impossible? Yeah i thought so too."));

        if (!BCrypt.checkpw(password, hash)) {
            var response = new ErrorResponse(
                    false,
                    "Username or password is incorrect.");
            return new Result<ErrorResponse>(400, response);
        }

        String token = generateToken();
        byte[] hashedToken;
        try {
            hashedToken = hashToken(token);
        } catch (NoSuchAlgorithmException ex) { return serverError(500, ex); }
        String sessionId = UUID.randomUUID().toString();

        long timeNow = System.currentTimeMillis();
        try {
            sessionRepo.newSession(sessionId, userId, hashedToken, timeNow, timeNow + tokenExpiryInMillis);
        } catch (SQLException ex) { return serverError(500, ex); }

        var response = new LoginResponse(
                true,
                token,
                sessionId,
                tokenExpiryInMillis/1000,
                "Login successful."
        );
        return new Result<LoginResponse>(200, response);
    }

    public Result<?> logout(String sessionKey, @Nullable String sessionId) {
        byte[] sessionHash;
        String owner;

        try {
            sessionHash = hashToken(sessionKey);

            if (sessionRepo.tokenExpired(sessionHash, System.currentTimeMillis())) {
                var response = new ErrorResponse(
                        false,
                        "Incorrect, or expired session key."
                );
                return new Result<ErrorResponse>(401, response);
            }
            owner = sessionRepo.getUserIdBySessionKey(sessionHash);
            if (sessionId == null) sessionId = sessionRepo.getSessionIdBySessionKey(sessionHash);

            if (!sessionRepo.sessionOwnerCheck(sessionId, owner)) {
                var response = new ErrorResponse(
                        false,
                        "Incorrect session id, or you're not the owner of this session."
                );
                return new Result<ErrorResponse>(401, response);
            }

            sessionRepo.deleteSessionBySessionId(sessionId);

        } catch (NoSuchAlgorithmException | SQLException ex) { return serverError(500, ex); }


        var response = new GenericResponse(
                true,
                "Logout successful."
        );
        return new Result<GenericResponse>(200, response);
    }

    public Result<?> logoutAll(String sessionKey) {
        byte[] sessionHash;
        String owner;
        try {
            sessionHash = hashToken(sessionKey);

            if (sessionRepo.tokenExpired(sessionHash, System.currentTimeMillis())) {
                var response = new ErrorResponse(
                        false,
                        "Incorrect, or expired session key."
                );
                return new Result<ErrorResponse>(400, response);
            }
            owner = sessionRepo.getUserIdBySessionKey(sessionHash);

            sessionRepo.deleteSessionsByUserId(owner);
        } catch (NoSuchAlgorithmException | SQLException ex) { return serverError(500, ex); }

        var response = new GenericResponse(
                true,
                "Logout successful."
        );
        return new Result<GenericResponse>(200, response);
    }

    public Result<?> register(String username, String password) {
        boolean userExists;
        try {
            userExists = userRepo.getId(username) != null;
        } catch (SQLException ex) { return serverError(500, ex); }

        if (userExists) {
            var response = new ErrorResponse(
                    false,
                    "Username already exists.");
            return new Result<ErrorResponse>(409, response);
        }

        boolean nameLengthCorrect = !username.isBlank() && username.length() >= minNameLength && username.length() <= maxNameLength;
        boolean passwordLengthCorrect = !password.isBlank() && password.length() >= minPasswordLength && password.length() <= maxPasswordLength;

        if (!passwordLengthCorrect) {
            var response = new ErrorResponse(
                    false,
                    "Password length is incorrect. Range: " + minPasswordLength + " - " + maxPasswordLength);
            return new Result<ErrorResponse>(400, response);
        }
        if (!nameLengthCorrect) {
            var response = new ErrorResponse(
                    false,
                    "Name length is incorrect. Range: " + minNameLength + " - " + maxNameLength);
            return new Result<ErrorResponse>(400, response);
        }


        String hash = BCrypt.hashpw(password, BCrypt.gensalt());
        String userId = UUID.randomUUID().toString();

        try {
            userRepo.create(userId, username, hash, System.currentTimeMillis());
        } catch (SQLException ex) { return serverError(500, ex); }

        var response = new GenericResponse(
                true,
                "Account successfully made."
        );
        return new Result<GenericResponse>(201, response);
    }



    private Result<?> serverError(int httpCode, Exception ex) {
        Debug.log("AuthService:generic serverError(): "+ex.toString());

        var response = new ErrorResponse(
                false,
                "Internal Server Error");
        return new Result<ErrorResponse>(httpCode, response);
    }
}
