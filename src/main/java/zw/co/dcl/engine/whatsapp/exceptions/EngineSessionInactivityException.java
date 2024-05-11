package zw.co.dcl.engine.whatsapp.exceptions;

/**
 * exception thrown when user inactivity is detected.
 * Session is not cleared here, assumption is user should return
 * to their previous stage if authentication is successful
 */
public class EngineSessionInactivityException extends WaEngineException {

    public EngineSessionInactivityException(String message) {
        super(message);
    }
}

