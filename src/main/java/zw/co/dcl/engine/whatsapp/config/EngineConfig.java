package zw.co.dcl.engine.whatsapp.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternUtils;

import java.util.HashMap;
import java.util.Map;

@Configuration
@Slf4j
public class EngineConfig {
    private final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    private final ResourcePatternResolver resolver = ResourcePatternUtils.getResourcePatternResolver(null);

    @Value("${resources.templates}")
    private String yamlTemplateFilesLocation;

    @Value("${resources.triggers}")
    private String yamlTriggerFilesLocation;

    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager() {
            @SuppressWarnings("NullableProblems")
            @Override
            protected Cache createConcurrentMapCache(String name) {
                return new ConcurrentMapCache(name, true);
            }
        };
    }

    @Bean("templatesMapConfig")
    public Map<String, Object> templatesContextMap() {
        return this.getContextMap(yamlTemplateFilesLocation);
    }

    @Bean("triggersMapConfig")
    public Map<String, Object> triggersContextMap() {
        return this.getContextMap(yamlTriggerFilesLocation);
    }

    private Map<String, Object> getContextMap(String path) {
        Map<String, Object> map = new HashMap<>();
        try {
            Resource[] resources = resolver.getResources(path);
            for (Resource resource : resources) {
                log.info("READING FROM TEMPLATE: {}", resource.getFilename());
                map.putAll(mapper.readValue(resource.getInputStream(), Map.class));
            }
            return map;
        } catch (Exception err) {
            throw new RuntimeException("error loading template: " + err.getMessage());
        }
    }
}
