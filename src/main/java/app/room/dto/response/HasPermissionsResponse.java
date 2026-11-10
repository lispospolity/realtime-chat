package app.room.dto.response;

import app.dto.response.Response;
import app.room.Permissions;

public record HasPermissionsResponse(
        boolean success,
        boolean isOwner,
        Permissions userPerms,
        String message
) implements Response {}
