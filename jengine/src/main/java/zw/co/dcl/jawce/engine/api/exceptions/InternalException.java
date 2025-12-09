package zw.co.dcl.jawce.engine.api.exceptions;

/**
 * exception thrown when engine encounters
 * an error internally during processing
 *
 * <p>
 * Should not be sent to user, meant for internal alert
 */
public class InternalException extends BaseEngineException {
    public InternalException(String message) {
        super(message);
    }

    public InternalException(String message, Throwable cause) {
        super(message, cause);
    }
}
