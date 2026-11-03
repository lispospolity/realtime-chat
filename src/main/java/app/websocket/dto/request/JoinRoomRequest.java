package app.websocket.dto.request;

import app.dto.request.Request;

public class JoinRoomRequest implements Request {
    private int room;

    public int getRoom() {
        return room;
    }
}
