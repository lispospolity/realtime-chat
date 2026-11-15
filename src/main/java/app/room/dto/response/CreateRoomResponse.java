package app.room.dto.response;

import app.dto.response.Response;

public record CreateRoomResponse(
        boolean success,
        int roomId,
        String message
) implements Response {
    @Override
    public boolean getSuccess() {
        return success();
    }

    @Override
    public String getMessage() {
        return message();
    }
}
