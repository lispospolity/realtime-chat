package app.websocket.dto.response;

public record OutgoingMessage(
        double roomId,
        String userId,
        String message
) {
}
