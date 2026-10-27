package app.websocket.dto;

public record OutgoingMessage(
        double roomId,
        String userId,
        String message
) {
}
