package app.room.dto.response;

import app.dto.response.Response;
import app.room.Permissions;
import jakarta.validation.constraints.NotNull;

public record HasPermissionsResponse(
        boolean success,
        @NotNull String roomOwner,
        Permissions userPerms,
        String message
) implements Response {}
