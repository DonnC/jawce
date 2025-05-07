package zw.co.dcl.jawce.engine.configs;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import zw.co.dcl.jawce.engine.api.Worker;
import zw.co.dcl.jawce.engine.api.iface.IClientManager;
import zw.co.dcl.jawce.engine.api.iface.ISessionManager;
import zw.co.dcl.jawce.engine.api.iface.ITemplateStorageManager;
import zw.co.dcl.jawce.engine.internal.service.ClientHelperService;
import zw.co.dcl.jawce.engine.internal.service.HookService;
import zw.co.dcl.jawce.engine.internal.service.WebhookProcessor;

@Configuration
@AutoConfiguration
@EnableConfigurationProperties({WhatsAppConfig.class, JawceConfig.class, TemplateStorageProperties.class})
public class JawceAutoConfig {
    @Bean
    public ClientHelperService clientHelperService(
            IClientManager clientManager,
            ISessionManager sessionManager,
            JawceConfig jawceConfig,
            WhatsAppConfig whatsAppConfig
    ) {
        return new ClientHelperService(clientManager, sessionManager, jawceConfig, whatsAppConfig);
    }

    @Bean
    @ConditionalOnMissingBean
    public HookService hookService(
            IClientManager clientManager,
            JawceConfig jawceConfig,
            ApplicationContext applicationContext
    ) {
        return new HookService(clientManager, jawceConfig, applicationContext);
    }

    @Bean
    public WebhookProcessor webhookProcessor(
            HookService hookService,
            ISessionManager sessionManager,
            ITemplateStorageManager templateStorageManager,
            JawceConfig jawceConfig
    ) {
        return new WebhookProcessor(hookService, sessionManager, templateStorageManager, jawceConfig);
    }

    @Bean
    public Worker worker(
            ApplicationEventPublisher publisher,
            WhatsAppConfig whatsAppConfig,
            JawceConfig jawceConfig,
            ClientHelperService clientHelperService,
            WebhookProcessor webhookProcessor,
            ISessionManager sessionManager // this will be injected if user provides it
    ) {
        return new Worker(publisher, whatsAppConfig, jawceConfig, clientHelperService, webhookProcessor, sessionManager);
    }
}
