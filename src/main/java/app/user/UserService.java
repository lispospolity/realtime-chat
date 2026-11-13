package app.user;

import app.dto.Result;
import app.dto.response.ErrorResponse;
import app.logging.Debug;
import app.user.dto.UsernameByIdResponse;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.sql.SQLTimeoutException;
import java.sql.SQLTransientConnectionException;

@Service
public class UserService {
    private final UserRepository userRepo;

    public UserService(UserRepository userRepo) { this.userRepo = userRepo; }

    public Result getUsernameById(String userId) {
        if (userId == null) {
            var response = new ErrorResponse(
                    false,
                    "UserId cannot be null.");
            return new Result(400, response);
        }

        try {
            String username = userRepo.getUserById(userId);

            if (username == null) {
                var response = new ErrorResponse(
                        false,
                        "Could not find a given userId.");
                return new Result(400, response);
            }

            var response = new UsernameByIdResponse(
                    true,
                    username,
                    "Successfully fetched the username.");
            return new Result(200, response);
        } catch (SQLException ex) { return serverError(ex); }
    }

    private Result serverError(Exception ex) {
        Debug.log("RoomService:generic serverError(): "+ex.toString());

        int httpCode = switch (ex) {
            case SQLTransientConnectionException e -> 503;
            case SQLTimeoutException e -> 504;
            case SQLException e -> 500;
            default -> 500;
        };

        var response = new ErrorResponse(
                false,
                "Internal Server Error");
        return new Result(httpCode, response);
    }
}
