package org.dcl.jawce.server.configs;

import org.dcl.jawce.server.service.engine.FileSessionManager;
import org.dcl.jawce.server.service.engine.RestTemplateClientManager;
import org.dcl.jawce.server.service.engine.YmlJsonTemplateStorageManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import zw.co.dcl.jawce.engine.api.iface.IClientManager;
import zw.co.dcl.jawce.engine.api.iface.ISessionManager;
import zw.co.dcl.jawce.engine.api.iface.ITemplateStorageManager;

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
    public IClientManager clientManager(RestTemplate restTemplate) {
        return new RestTemplateClientManager(restTemplate);
    }

    @Bean
    public ITemplateStorageManager templateStorageManager(YmlJsonTemplateStorageManager ymlJsonTemplateStorageManager) {
        return ymlJsonTemplateStorageManager;
    }
}
