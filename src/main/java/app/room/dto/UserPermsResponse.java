package app.room.dto;

import app.room.Permissions;

public record UserPermsResponse(
        boolean success,
        Permissions perms,
        String message
) {}
