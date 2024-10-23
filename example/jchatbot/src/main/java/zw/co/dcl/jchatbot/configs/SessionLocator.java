package zw.co.dcl.jchatbot.configs;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import zw.co.dcl.jawce.session.ISessionManager;

/**
 * Session service locator
 * <p>
 * Used on non-spring service classes (usually Hook classes)
 * <p>
 * to get access to ISessionManager bean
 */
@Component
public class SessionLocator {
    private static ISessionManager sessionManager;

    public static ISessionManager getSessionManager() {
        if(sessionManager == null) {
            throw new IllegalStateException("SessionManager not initialized.");
        }
        return sessionManager;
    }

    @Autowired
    public void setSessionManager(SessionConfig sessionConfig) {
        SessionLocator.sessionManager = sessionConfig.sessionManager();
    }
}
