package app.websocket.dto;

import app.dto.response.Response;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * recipients requires session Objects
 */

public record MessageDispatch<E> (
        @Nullable List<String> recipients,
        @Nullable E messageToRecipients,
        @NotNull Response responseToSender
) {}
