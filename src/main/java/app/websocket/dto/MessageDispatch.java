package app.websocket.dto;

import app.websocket.Session;

import java.util.List;

/**
 * recipients requires session Objects
 */

public record MessageDispatch<E> (
    List<Session> recipients,
    E response
) {}
