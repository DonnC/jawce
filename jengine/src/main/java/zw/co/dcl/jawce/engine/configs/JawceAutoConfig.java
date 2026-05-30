package zw.co.dcl.jawce.engine.configs;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.client.RestTemplate;
import zw.co.dcl.jawce.engine.api.Worker;
import zw.co.dcl.jawce.engine.api.iface.IClientManager;
import zw.co.dcl.jawce.engine.api.iface.IHistoryManager;
import zw.co.dcl.jawce.engine.api.iface.ISessionManager;
import zw.co.dcl.jawce.engine.api.iface.ITemplateStorageManager;
import zw.co.dcl.jawce.engine.defaults.FileHistoryManager;
import zw.co.dcl.jawce.engine.defaults.FileSessionManager;
import zw.co.dcl.jawce.engine.defaults.NoOpHistoryManager;
import zw.co.dcl.jawce.engine.defaults.RestTemplateClientManager;
import zw.co.dcl.jawce.engine.defaults.YmlJsonTemplateStorageManager;
import zw.co.dcl.jawce.engine.internal.service.HistoryEventListener;
import zw.co.dcl.jawce.engine.internal.service.HistoryEventPublisher;
import zw.co.dcl.jawce.engine.internal.service.FlowHookRegistry;
import zw.co.dcl.jawce.engine.internal.service.HookService;
import zw.co.dcl.jawce.engine.internal.service.WebhookProcessor;
import zw.co.dcl.jawce.engine.internal.service.WhatsAppHelperService;
import zw.co.dcl.jawce.engine.internal.service.WhatsAppFlowService;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
@AutoConfiguration
@EnableConfigurationProperties({WhatsAppConfig.class, JawceConfig.class, TemplateStorageProperties.class, FileSessionProperties.class, HistoryProperties.class})
public class JawceAutoConfig {
    @Bean
    @ConditionalOnMissingBean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    @ConditionalOnMissingBean(ISessionManager.class)
    public ISessionManager sessionManager(FileSessionProperties fileSessionProperties) {
        return new FileSessionManager(fileSessionProperties);
    }

    @Bean
    @ConditionalOnMissingBean(IClientManager.class)
    public IClientManager clientManager(RestTemplate restTemplate) {
        return new RestTemplateClientManager(restTemplate);
    }

    @Bean
    @ConditionalOnMissingBean(ITemplateStorageManager.class)
    public ITemplateStorageManager templateStorageManager(
            TemplateStorageProperties templateStorageProperties,
            FlowHookRegistry flowHookRegistry
    ) {
        return new YmlJsonTemplateStorageManager(templateStorageProperties, flowHookRegistry);
    }

    @Bean
    @ConditionalOnMissingBean(IHistoryManager.class)
    public IHistoryManager historyManager(HistoryProperties historyProperties) {
        if(historyProperties.isFileEnabled()) {
            return new FileHistoryManager(historyProperties);
        }
        return new NoOpHistoryManager();
    }

    @Bean(name = "jawceHistoryExecutor", destroyMethod = "shutdown")
    @ConditionalOnMissingBean(name = "jawceHistoryExecutor")
    public ExecutorService jawceHistoryExecutor(HistoryProperties historyProperties) {
        return Executors.newFixedThreadPool(Math.max(1, historyProperties.getExecutorThreads()), runnable -> {
            Thread thread = new Thread(runnable);
            thread.setName("jawce-history");
            thread.setDaemon(true);
            return thread;
        });
    }

    @Bean
    @ConditionalOnMissingBean
    public HistoryEventPublisher historyEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        return new HistoryEventPublisher(applicationEventPublisher);
    }

    @Bean
    @ConditionalOnMissingBean
    public HistoryEventListener historyEventListener(
            IHistoryManager historyManager,
            @Qualifier("jawceHistoryExecutor") ExecutorService jawceHistoryExecutor
    ) {
        return new HistoryEventListener(historyManager, jawceHistoryExecutor);
    }

    @Bean
    public WhatsAppHelperService whatsAppHelperService(
            IClientManager clientManager,
            ISessionManager sessionManager,
            JawceConfig jawceConfig,
            WhatsAppConfig whatsAppConfig,
            HistoryEventPublisher historyEventPublisher
    ) {
        return new WhatsAppHelperService(clientManager, sessionManager, jawceConfig, whatsAppConfig, historyEventPublisher);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "whatsapp", name = "private-key-pem-path")
    public WhatsAppFlowService whatsAppFlowService(WhatsAppConfig whatsAppConfig) {
        return new WhatsAppFlowService(whatsAppConfig);
    }

    @Bean
    @ConditionalOnMissingBean
    public FlowHookRegistry flowHookRegistry(ApplicationContext applicationContext) {
        return new FlowHookRegistry(applicationContext);
    }

    @Bean
    @ConditionalOnMissingBean
    public HookService hookService(
            IClientManager clientManager,
            JawceConfig jawceConfig,
            ApplicationContext applicationContext,
            FlowHookRegistry flowHookRegistry
    ) {
        return new HookService(clientManager, jawceConfig, applicationContext, flowHookRegistry);
    }

    @Bean
    public WebhookProcessor webhookProcessor(
            HookService hookService,
            ISessionManager sessionManager,
            ITemplateStorageManager templateStorageManager,
            JawceConfig jawceConfig,
            WhatsAppHelperService whatsAppHelperService,
            HistoryEventPublisher historyEventPublisher
    ) {
        return new WebhookProcessor(hookService, sessionManager, templateStorageManager, jawceConfig, whatsAppHelperService, historyEventPublisher);
    }

    @Bean
    public Worker worker(
            ApplicationEventPublisher publisher,
            WhatsAppConfig whatsAppConfig,
            JawceConfig jawceConfig,
            WhatsAppHelperService whatsAppHelperService,
            WebhookProcessor webhookProcessor,
            ISessionManager sessionManager,
            HistoryEventPublisher historyEventPublisher
    ) {
        return new Worker(publisher, whatsAppConfig, jawceConfig, whatsAppHelperService, webhookProcessor, sessionManager, historyEventPublisher);
    }
}
