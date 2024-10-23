package zw.co.dcl.jawce.engine.exceptions;

/**
 * exception thrown when engine encounters
 * an error internally during processing
 *
 * <p>
 * Should not be sent to user, meant for internal alert
 */
public class EngineInternalException extends WaEngineException {
    public EngineInternalException(String message) {
        super(message);
    }

    public EngineInternalException(String message, Throwable cause) {
        super(message, cause);
    }
}
