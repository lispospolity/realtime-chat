package app.dto;

import app.dto.response.Response;

public record Result(
        int httpCode,
        Response response
) {}