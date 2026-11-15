package app.room.dto.response;

import app.dto.response.Response;
import app.room.Permissions;
import app.room.Room;
import jakarta.validation.constraints.NotNull;

public record HasPermissionsResponse(
        boolean success,
        @NotNull Room room,
        @NotNull Permissions userPerms,
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
