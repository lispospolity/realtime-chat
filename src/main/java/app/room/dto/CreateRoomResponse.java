package app.room.dto;

import app.dto.response.Response;

public record CreateRoomResponse(
        boolean success,
        int roomId,
        String message
) implements Response {}
