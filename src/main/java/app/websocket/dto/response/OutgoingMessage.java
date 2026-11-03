package app.websocket.dto.response;

import app.dto.response.Response;

public record OutgoingMessage(
        double roomId,
        String userId,
        String message
) implements Response {}