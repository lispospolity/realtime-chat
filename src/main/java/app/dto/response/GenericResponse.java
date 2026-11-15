package app.dto.response;

public record GenericResponse(
        boolean success,
        String message
) implements Response {
    @Override
    public boolean getSuccess() {
        return success();
    }

    @Override
    public String getMessage() {
        return message();
    }
}
