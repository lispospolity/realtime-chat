package app.websocket.dto.request;

import app.dto.request.Request;

public class SendMessageRequest implements Request {
    private int room;
    private String reqid;
    private String message;


    public int getRoom() {
        return room;
    }

    public String getMessageId() {
        return reqid;
    }

    public String getMessage() {
        return message;
    }
}
