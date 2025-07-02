package zw.co.dcl.jawce.engine.api.exceptions;

/**
 * Channel high alert exception
 *
 * <p>
 * Global WhatsAppConfig exception received
 * when something wrong is received
 * like billable errors or channel exception
 * with detailed error code message
 */
public class WhatsappException extends BaseEngineException {
    public WhatsappException(String message) {
        super(message);
    }
}
