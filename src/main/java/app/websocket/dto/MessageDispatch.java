package app.websocket.dto;

import org.java_websocket.WebSocket;

public record MessageDispatch<E> (
    WebSocket[] recipients,
    E response
) {}
