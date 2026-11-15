package app.user.dto;

import app.dto.response.Response;
import jakarta.validation.constraints.NotNull;

public record UsernameByIdResponse(
        boolean success,
        @NotNull String username,
        String message
) implements Response {
    @Override
    public boolean getSuccess() {
        return success();
    }

    @Override
    public String getMessage() {
        return message();
    }
}
