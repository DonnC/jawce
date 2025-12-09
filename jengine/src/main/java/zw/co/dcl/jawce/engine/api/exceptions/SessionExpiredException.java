package zw.co.dcl.jawce.engine.api.exceptions;

/**
 * exception thrown when engine global
 * user session expired, assume all user session data
 * is cleared and sent user back to login screen
 */
public class SessionExpiredException extends BaseEngineException {
    public SessionExpiredException(String message) {
        super(message);
    }
}
