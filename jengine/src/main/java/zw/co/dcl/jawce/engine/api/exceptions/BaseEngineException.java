package zw.co.dcl.jawce.engine.api.exceptions;

public abstract class BaseEngineException extends RuntimeException {
    public BaseEngineException(String message) {
        super(message);
    }

    public BaseEngineException(String message, Throwable cause) {
        super(message, cause);
    }
}
