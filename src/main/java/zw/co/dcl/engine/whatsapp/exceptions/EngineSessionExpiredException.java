package zw.co.dcl.engine.whatsapp.exceptions;

/**
 * exception thrown when engine global
 * user session expired, assume all user session data
 * is cleared and sent user back to login screen
 */
public class EngineSessionExpiredException extends WaEngineException {
    public EngineSessionExpiredException(String message) {
        super(message);
    }
}