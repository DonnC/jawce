package zw.co.dcl.jawce.engine.defaults;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import zw.co.dcl.jawce.engine.configs.TemplateStorageProperties;
import zw.co.dcl.jawce.engine.model.abs.AbsEngineTemplate;
import zw.co.dcl.jawce.engine.model.core.EngineRoute;
import zw.co.dcl.jawce.engine.service.iface.ITemplateStorageManager;
import zw.co.dcl.jawce.engine.utils.SerializeUtils;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A default storage manager that is based on YAML templates
 * <p>
 * The manager reads all template & trigger yaml files in a directory path provided
 * <p>
 * TODO: implement yaml template manager
 */
public class YamlTemplateStorageManager implements ITemplateStorageManager {
    private static Map<String, Object> templates = new ConcurrentHashMap<>();
    private static Map<String, Object> triggers = new ConcurrentHashMap<>();
    private final Logger logger = LoggerFactory.getLogger(YamlTemplateStorageManager.class);

    public YamlTemplateStorageManager(TemplateStorageProperties properties) {
        templates = loadResourcesAsMap(properties.getTemplatesPath());
        triggers = loadResourcesAsMap(properties.getTriggersPath());
        logger.debug("Template storage manager initialized with templates: {} and triggers: {}", templates.size(), triggers.size());
    }

    private Map<String, Object> loadResourcesAsMap(String folderPath) {
        Map<String, Object> map = new ConcurrentHashMap<>();

        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        try {
            Resource[] resources = resolver.getResources("classpath*:" + folderPath + "/**/*.yml");

            for (Resource resource : resources) {
                try {
                    map.putAll(SerializeUtils.readInputStreamAsMap(resource.getInputStream()));
                } catch (IOException e) {
                    throw new RuntimeException("Failed to load resource: " + resource.getFilename(), e);
                }
            }

            // Also try loading *.yaml files
            resources = resolver.getResources("classpath*:" + folderPath + "/**/*.yaml");
            for (Resource resource : resources) {
                try {
                    map.putAll(SerializeUtils.readInputStreamAsMap(resource.getInputStream()));
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
        return List.of();
    }

    @Override
    public Optional<AbsEngineTemplate> getTemplate(String templateName) {
        if(!this.exists(templateName)) return Optional.empty();

        Map<String, Object> stageMap = (Map) templates.get(templateName);

        // TODO: implement logic

        return Optional.empty();
    }
}
