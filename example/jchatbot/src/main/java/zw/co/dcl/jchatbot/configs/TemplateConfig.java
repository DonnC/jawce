package zw.co.dcl.jchatbot.configs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Configuration
@Slf4j
public class TemplateConfig {
    private final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    private final ResourcePatternResolver resolver = ResourcePatternUtils.getResourcePatternResolver(null);

    @Value("${resources.templates}")
    private String botTemplatesLocation;

    @Value("${resources.triggers}")
    private String botTriggersLocation;

    @Getter
    @Value("${resources.hooks.base-url}")
    private String botEngineHookBaseUrl;

    @Getter
    @Value("${resources.hooks.security-token}")
    private String botEngineHookUrlToken;

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean("botTemplates")
    public Map<String, Object> readBotTemplates() {
        return getResourceAsMap(botTemplatesLocation);
    }

    @Bean("botTriggers")
    public Map<String, Object> readBotTriggers() {
        return getResourceAsMap(botTriggersLocation);
    }


    private Map<String, Object> getResourceAsMap(String path) {
        Map<String, Object> map = new HashMap<>();
        try {
            Resource[] resources = resolver.getResources(path);
            for (Resource resource : resources) {
                log.warn("[-] Processing template file: {}", resource.getFilename());
                map.putAll(mapper.readValue(resource.getInputStream(), Map.class));
            }
            return map;
        } catch (Exception err) {
            throw new RuntimeException("error loading template: " + err.getMessage());
        }
    }
}
