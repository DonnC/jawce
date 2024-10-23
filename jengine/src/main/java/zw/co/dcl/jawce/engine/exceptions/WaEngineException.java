package zw.co.dcl.jawce.engine.exceptions;

public abstract class WaEngineException extends RuntimeException {
    public WaEngineException(String message) {
        super(message);
    }

    public WaEngineException(String message, Throwable cause) {
        super(message, cause);
    }
}
