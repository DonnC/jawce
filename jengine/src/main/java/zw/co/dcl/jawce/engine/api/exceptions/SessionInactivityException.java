package zw.co.dcl.jawce.engine.api.exceptions;

/**
 * exception thrown when user inactivity is detected.
 * Session is not cleared here, assumption is user should return
 * to their previous stage if authentication is successful
 */
public class SessionInactivityException extends BaseEngineException {

    public SessionInactivityException(String message) {
        super(message);
    }
}
