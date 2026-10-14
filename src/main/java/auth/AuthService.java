package auth;

import config.AppConfig;

import java.security.SecureRandom;
import java.sql.SQLException;
import java.util.Base64;
import java.util.UUID;

import database.repository.SessionRepository;
import database.repository.UserRepository;
import logging.Debug;
import org.mindrot.jbcrypt.BCrypt;

public class AuthService {
    private final int minNameLength;
    private final int maxNameLength;
    private final int minPasswordLength;
    private final int maxPasswordLength;

    private final SecureRandom secureRandom = new SecureRandom();
    private final UserRepository userRepo;
    private final SessionRepository sessionRepo;

    public AuthService(AppConfig appConfig, UserRepository userRepository, SessionRepository sessionRepository) {
        minNameLength = appConfig.minNameLength;
        maxNameLength = appConfig.maxNameLength;
        minPasswordLength = appConfig.minPasswordLength;
        maxPasswordLength = appConfig.maxPasswordLength;

        this.userRepo = userRepository;
        this.sessionRepo = sessionRepository;
    }

    public Result<?> login(String username, String password) {

        String token = generateToken();

        var response = new LoginResponse(
                false,
                token,
                "Login successful."
        );
        return new Result<LoginResponse>(201, response);
    }

    public Result<?> register(String username, String password) {
        boolean userExists;
        try {
            userExists = userRepo.nameExists(username);
        } catch (SQLException ex) {
            return serverError(ex);
        }

        if (userExists) {
            var response = new ErrorResponse(
                    false,
                    "Username already exists.");
            return new Result<ErrorResponse>(409, response);
        }

        boolean nameLengthCorrect = username.length() >= minNameLength && username.length() <= maxNameLength;
        boolean passwordLengthCorrect = password.length() >= minPasswordLength && password.length() <= maxPasswordLength;

        if (!passwordLengthCorrect) {
            var response = new ErrorResponse(
                    false,
                    "Password length is incorrect. Range: "+minPasswordLength+" - "+maxPasswordLength);
            return new Result<ErrorResponse>(400, response);
        }
        if (!nameLengthCorrect) {
            var response = new ErrorResponse(
                    false,
                    "Name length is incorrect. Range: "+minNameLength+" - "+maxNameLength);
            return new Result<ErrorResponse>(400, response);
        }


        String hash = BCrypt.hashpw(password, BCrypt.gensalt());
        String userId = UUID.randomUUID().toString();

        try {
            userRepo.create(userId, username, hash, System.currentTimeMillis());
        } catch (SQLException ex) {
            return serverError(ex);
        }

        var response = new RegisterResponse(
                true,
                "Account successfully made."
        );
        return new Result<RegisterResponse>(201, response);
    }



    private String generateToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }
    private Result<?> serverError(Exception ex) {
        Debug.log(ex.toString());

        var response = new ErrorResponse(
                false,
                "Internal Server Error");
        return new Result<ErrorResponse>(500, response);
    }
}
