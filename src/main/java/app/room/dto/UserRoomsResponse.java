package app.room.dto;

import app.dto.response.Response;

import java.util.List;

public record UserRoomsResponse(
        boolean success,
        List<Integer> rooms,
        String message
) implements Response {}
