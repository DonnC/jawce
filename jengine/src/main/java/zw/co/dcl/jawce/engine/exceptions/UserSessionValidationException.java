package zw.co.dcl.jawce.engine.exceptions;

public class UserSessionValidationException extends WaEngineException {
    public UserSessionValidationException(String message) {
        super(message);
    }

    public UserSessionValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
