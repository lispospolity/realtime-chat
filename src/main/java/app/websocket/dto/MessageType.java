package app.websocket.dto;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public enum MessageType {
    JOIN_ROOM(0d), SEND_MESSAGE(1d),
    SERVER_RESPONSE(200d), INCOMING_MESSAGE(201d);

    public final Double code;
    MessageType(Double code) {this.code = code;}

    private static final Map<Double, MessageType> BY_CODE =
            Arrays.stream(values()).collect(Collectors.toMap(m -> m.code, m -> m));

    public static MessageType fromCode(double code) {
        return BY_CODE.get(code);
    }
}
