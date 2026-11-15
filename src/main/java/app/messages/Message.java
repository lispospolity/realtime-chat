package app.messages;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public final class Message {
    private final @NotNull String senderId;
    public String getSenderId() {
        return senderId;
    }

    private final int roomId;
    public int getRoomId() {
        return roomId;
    }

    private final @NotNull String content;
    public String getContent() {
        return content;
    }

    private final @NotNull String messageId;
    public String getMessageId() {
        return messageId;
    }

    private final long serverReceivedAt;
    public long getServerReceivedAt() {
        return serverReceivedAt;
    }

    public Message(String sentBy, String message, int roomId) {
        if (sentBy == null || sentBy.isBlank()) throw new IllegalArgumentException("SentBy field cannot be null, nor blank.");
        if (message == null || message.isBlank()) throw new IllegalArgumentException("message field cannot be null, nor blank.");
        this.senderId = sentBy;
        this.roomId = roomId;
        this.content = message;
        this.messageId = UUID.randomUUID().toString();
        this.serverReceivedAt = System.currentTimeMillis();
    }
}
