package app.websocket.dto.request;

public class SendMessageRequest {
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
