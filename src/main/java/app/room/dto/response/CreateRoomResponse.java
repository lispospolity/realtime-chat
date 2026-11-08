package app.room.dto.response;

import app.dto.response.Response;

public record CreateRoomResponse(
        boolean success,
        int roomId,
        String message
) implements Response {}
