package zw.co.dcl.jchatbot.configs;


import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import zw.co.dcl.jawce.session.ISessionManager;
import zw.co.dcl.jawce.session.impl.CaffeineSessionManager;
import zw.co.dcl.jawce.session.impl.FileBasedSessionManager;

import java.util.concurrent.TimeUnit;


@Component
public class SessionConfig {
    @Bean
    public ISessionManager sessionManager() {
        // TODO: ISessionManager implement or use your own choice
        // return FileSessionManager.getInstance();
        return CaffeineSessionManager.getInstance(30L, TimeUnit.MINUTES);
    }
}
