package app.websocket.dto;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * recipients requires session Objects
 */

public record MessageDispatch<E, F> (
        @Nullable List<String> recipients,
        @Nullable E messageToRecipients,
        @NotNull F responseToSender
) {}
