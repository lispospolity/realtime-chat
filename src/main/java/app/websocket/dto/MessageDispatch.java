package app.websocket.dto;

import app.dto.response.GenericResponse;

import org.java_websocket.WebSocket;

public record MessageDispatch (
    WebSocket[] recipients,
    GenericResponse response
) {}
