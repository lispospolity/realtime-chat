package app.room.dto.request;

import app.dto.request.Request;

public record RoomMemberConfigRequest(
        int permissions
) implements Request {}
