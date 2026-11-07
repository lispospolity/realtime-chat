package app.user;

import app.auth.dto.Result;
import app.dto.response.ErrorResponse;
import app.logging.Debug;
import app.user.dto.UsernameByIdResponse;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Service;

import java.sql.SQLException;

@Service
public class UserService {
    private UserRepository userRepo;

    public UserService(UserRepository userRepo) { this.userRepo = userRepo; }

    public Result getUsernameById(@NotNull String userId) {
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
        } catch (SQLException ex) { return serverError(500, ex); }
    }

    private Result serverError(int httpCode, Exception ex) {
        Debug.log("RoomService:generic serverError(): "+ex.toString());

        var response = new ErrorResponse(
                false,
                "Internal Server Error");
        return new Result(httpCode, response);
    }
}
