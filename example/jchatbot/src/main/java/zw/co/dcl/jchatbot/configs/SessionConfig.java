package zw.co.dcl.jchatbot.configs;


import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import zw.co.dcl.jawce.session.ISessionManager;
import zw.co.dcl.jawce.session.impl.FileBasedSessionManager;


@Component
public class SessionConfig {
    @Bean
    public ISessionManager sessionManager() {
        // TODO: ISessionManager implement or use your own choice
        return FileBasedSessionManager.getInstance();
    }
}
