package zw.co.dcl.jchatbot.configs;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import zw.co.dcl.jawce.engine.api.iface.IClientManager;
import zw.co.dcl.jawce.engine.api.iface.ISessionManager;
import zw.co.dcl.jawce.engine.api.iface.ITemplateStorageManager;
import zw.co.dcl.jchatbot.service.engine.FileSessionManager;
import zw.co.dcl.jchatbot.service.engine.RestTemplateClientManager;
import zw.co.dcl.jchatbot.service.engine.YamlTemplateStorageManager;

@Configuration
public class EngineConfigs {
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public ISessionManager sessionManager() {
        return FileSessionManager.getInstance();
    }

    @Bean
    public IClientManager clientManager(RestTemplateClientManager restTemplateClientManager) {
        return restTemplateClientManager;
    }

    @Bean
    public ITemplateStorageManager templateStorageManager(YamlTemplateStorageManager yamlTemplateStorageManager) {
        return yamlTemplateStorageManager;
    }
}
