package app.room.dto.response;

import app.dto.response.Response;
import app.room.RoomMember;

import java.util.List;

public record RoomMembersResponse(
        boolean success,
        List<RoomMember> members,
        String message
) implements Response {}
