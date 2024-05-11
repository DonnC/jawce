package zw.co.dcl.engine.whatsapp.exceptions;

/**
 * Channel high alert exception
 *
 * <p>
 * Global WhatsApp exception received
 * when something wrong is received
 * like billable errors or channel exception
 * with detailed error code message
 */
public class EngineWhatsappException extends WaEngineException {
    public EngineWhatsappException(String message) {
        super(message);
    }
}

