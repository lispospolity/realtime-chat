package app.room.dto.response;

import app.dto.response.Response;

import java.util.List;

public record RoomMembersResponse(
        boolean success,
        List<String> members,
        String message
) implements Response {}
