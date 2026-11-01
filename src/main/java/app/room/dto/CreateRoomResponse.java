package app.room.dto;

public record CreateRoomResponse(
        boolean success,
        int roomId,
        String message
) {}
