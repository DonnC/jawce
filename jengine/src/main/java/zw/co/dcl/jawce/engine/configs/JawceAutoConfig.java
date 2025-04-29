package zw.co.dcl.jawce.engine.configs;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import zw.co.dcl.jawce.engine.api.Worker;
import zw.co.dcl.jawce.engine.defaults.YamlTemplateStorageManager;
import zw.co.dcl.jawce.engine.service.iface.ISessionManager;
import zw.co.dcl.jawce.engine.service.iface.ITemplateStorageManager;

@EnableConfigurationProperties({JawceConfig.class, WhatsAppConfig.class, TemplateStorageProperties.class})
@Configuration
public class JawceAutoConfig {
    @Bean
    @ConditionalOnMissingBean(ITemplateStorageManager.class)
    public ITemplateStorageManager defaultTemplateStorageManager(@Qualifier("templateStorageProperties") TemplateStorageProperties properties) {
        return new YamlTemplateStorageManager(properties);
    }

    @Bean
    public Worker worker(WhatsAppConfig whatsappConfig, JawceConfig jawceConfig, ISessionManager sessionManager) {
        return new Worker(whatsappConfig, jawceConfig, sessionManager);
    }
}
