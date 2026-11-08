package app.room.dto.response;

import app.dto.response.Response;
import app.room.Permissions;

public record UserPermsResponse(
        boolean success,
        Permissions perms,
        String message
) implements Response {}
