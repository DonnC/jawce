package zw.co.dcl.jawce.engine.processor.abstracts;

import zw.co.dcl.jawce.engine.constants.SessionConstants;
import zw.co.dcl.jawce.engine.model.abs.AbsHookArg;
import zw.co.dcl.jawce.engine.exceptions.EngineSessionExpiredException;
import zw.co.dcl.jawce.engine.exceptions.UserSessionValidationException;
import zw.co.dcl.jawce.session.ISessionManager;
import zw.co.dcl.jawce.engine.utils.CommonUtils;

import java.util.logging.Logger;

/**
 * Used to validate if the recipient is same as
 * <p>
 * saved msisdn for channel
 * <p>
 * It simply verifies that, the current waId is equal to the session
 * <p>
 * String sessionUid = session.get(currentWaId, SessionConstants.SERVICE_PROFILE_MSISDN_KEY, String.class);
 * <p>
 * sessionUid == currentWaId
 */
public abstract class AbstractRecipientValidator {
    private static final Logger log = Logger.getLogger(AbstractRecipientValidator.class.getName());

    protected void validate(AbsHookArg args, ISessionManager session) {
        if(session == null || args == null) return;

        var currentWaId = args.getWaUser().waId();
        var hasAuth = session.get(currentWaId, SessionConstants.ENGINE_AUTH_VALID_KEY, Boolean.class);
        String loggedInTime = session.get(currentWaId, SessionConstants.SESSION_EXPIRY, String.class);
        String sessionUid = session.get(currentWaId, SessionConstants.SERVICE_PROFILE_MSISDN_KEY, String.class);

        if(hasAuth != null && hasAuth) {
            if(loggedInTime == null || sessionUid == null || CommonUtils.hasSessionExpired(loggedInTime)) {
                log.severe("Session expired during session mismatch check for WA id: " + currentWaId);
                throw new EngineSessionExpiredException("Your session has expired. Kindly login again to access our WhatsAppConfig Services");
            }

            if(!sessionUid.equals(currentWaId)) {
                log.severe("Session mismatch detected for WA id: " + currentWaId + " & session msisdn: " + sessionUid);
                throw new UserSessionValidationException("Recipient validation failed: possible s-mismatch");
            }
        }
    }
}
