package app.websocket.dto.request;

public class SendMessageRequest {
    private int room;
    private String message;


    public int getRoom() {
        return room;
    }

    public String getMessage() {
        return message;
    }
}
