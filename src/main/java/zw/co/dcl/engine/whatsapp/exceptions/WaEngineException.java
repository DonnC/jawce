package zw.co.dcl.engine.whatsapp.exceptions;

public abstract class WaEngineException extends RuntimeException {
    public WaEngineException(String message) {
        super(message);
    }

    public WaEngineException(String message, Throwable cause) {
        super(message, cause);
    }
}

