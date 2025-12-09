package zw.co.dcl.jawce.engine.api.exceptions;

public class UserSessionValidationException extends BaseEngineException {
    public UserSessionValidationException(String message) {
        super(message);
    }

    public UserSessionValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
