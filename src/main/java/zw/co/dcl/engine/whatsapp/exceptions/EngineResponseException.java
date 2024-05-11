package zw.co.dcl.engine.whatsapp.exceptions;

/**
 * if caught, send the response back to User
 * to get the error message for actioning
 */
public class EngineResponseException extends WaEngineException {
    public EngineResponseException(String message) {
        super(message);
    }

    public EngineResponseException(String message, Throwable cause) {
        super(message, cause);
    }
}

