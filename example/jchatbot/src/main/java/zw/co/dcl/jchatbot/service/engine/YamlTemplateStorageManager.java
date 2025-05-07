package zw.co.dcl.jchatbot.service.engine;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;
import zw.co.dcl.jawce.engine.api.iface.ITemplateStorageManager;
import zw.co.dcl.jawce.engine.api.utils.SerializeUtils;
import zw.co.dcl.jawce.engine.configs.TemplateStorageProperties;
import zw.co.dcl.jawce.engine.model.abs.BaseEngineTemplate;
import zw.co.dcl.jawce.engine.model.core.EngineRoute;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;


/**
 * A default storage manager that is based on YAML templates
 * <p>
 * The manager reads all template & trigger yaml files in a directory path provided
 * <p>
 * TODO: implement yaml template manager
 */
@Slf4j
@Service
public class YamlTemplateStorageManager implements ITemplateStorageManager {
    private static Map<String, Object> templates = new ConcurrentHashMap<>();
    private static Map<String, Object> triggers = new ConcurrentHashMap<>();

    public YamlTemplateStorageManager(TemplateStorageProperties properties) {
        templates = loadResourcesAsMap(properties.getTemplatesPath());
        triggers = loadResourcesAsMap(properties.getTriggersPath());
        log.info("Template storage manager initialized with templates: {} and triggers: {}", templates.size(), triggers.size());
    }

    private Map<String, Object> loadResourcesAsMap(String folderPath) {
        log.info("Loading resources from {}", folderPath);

        Map<String, Object> map = new ConcurrentHashMap<>();

        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

        try {
            Resource[] ymlResources = resolver.getResources(folderPath + "/**/*.yml");
            Resource[] yamlResources = resolver.getResources(folderPath + "/**/*.yaml");

            Resource[] allResources = Stream.concat(Arrays.stream(ymlResources), Arrays.stream(yamlResources)).toArray(Resource[]::new);

            log.info("Found {} resources", allResources.length);

            for (Resource resource : allResources) {
                try {
                    // Skip directories and unreadable files
                    if(resource.exists() && resource.isReadable() && !resource.getFilename().endsWith("/")) {
                        map.putAll(SerializeUtils.readInputStreamAsMap(resource.getInputStream()));
                    } else {
                        log.warn("Skipping non-readable or directory resource: {}", resource.getFilename());
                    }
                } catch (IOException e) {
                    throw new RuntimeException("Failed to load resource: " + resource.getFilename(), e);
                }
            }

        } catch (IOException e) {
            throw new RuntimeException("Error loading templates/triggers from folder: " + folderPath, e);
        }

        return map;
    }


    @Override
    public void loadTemplates() {
    }

    @Override
    public void loadTriggers() {
    }

    @Override
    public boolean exists(String templateName) {
        return templates.containsKey(templateName);
    }

    @Override
    public List<EngineRoute> triggers() {
        // TODO: implement get triggers logic
        return new ArrayList<>();
    }

    @Override
    public Optional<BaseEngineTemplate> getTemplate(String templateName) {
        if(!this.exists(templateName)) return Optional.empty();

        Map<String, Object> stageMap = (Map) templates.get(templateName);

        // TODO: implement logic

        return Optional.empty();
    }
}
