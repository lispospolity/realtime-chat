package app.dto.response;

public record ErrorResponse(
        boolean success,
        String error
) implements Response {
    @Override
    public boolean getSuccess() {
        return success();
    }

    @Override
    public String getMessage() {
        return error();
    }
}