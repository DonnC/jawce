package zw.co.dcl.jchatbot.service.engine;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import zw.co.dcl.jawce.engine.api.iface.ITemplateStorageManager;
import zw.co.dcl.jawce.engine.configs.TemplateStorageProperties;
import zw.co.dcl.jawce.engine.model.abs.BaseEngineTemplate;
import zw.co.dcl.jawce.engine.model.core.EngineRoute;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
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
    private static final Map<String, BaseEngineTemplate> templates = new ConcurrentHashMap<>();
    private static final List<EngineRoute> triggers = new CopyOnWriteArrayList<>();
    private final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    private final TemplateStorageProperties properties;

    public YamlTemplateStorageManager(TemplateStorageProperties properties) {
        this.properties = properties;
        this.loadTemplates();

        templates.forEach((key, template) -> log.debug("{} -> {}", key, template.getClass().getSimpleName()));

        this.loadTriggers();
        log.info("Template storage manager initialized with templates: {} and triggers: {}", templates.size(), triggers.size());
    }

    void parseTriggerMapToRoutes(Map<String, Object> triggerMap) {
        List<EngineRoute> routes = new ArrayList<>();

        for (Map.Entry<String, Object> entry : triggerMap.entrySet()) {
            String nextStage = entry.getKey();
            String userInput = entry.getValue().toString();

            EngineRoute route = new EngineRoute();
            route.setUserInput(userInput);
            route.setNextStage(nextStage);
            route.setRegex(userInput.startsWith("re:"));

            routes.add(route);
        }

        triggers.addAll(routes);
    }


    @Override
    public void loadTemplates() {
        var pathDir = this.properties.getTemplatesPath();
        Assert.notNull(pathDir, "Directory is null");

        Map<String, BaseEngineTemplate> map = new ConcurrentHashMap<>();

        if(pathDir.startsWith("classpath:")) {
            log.warn("Loading classpath resources from {}", pathDir);
            String pattern = "classpath*:" + pathDir.substring("classpath:".length()) + "/**/*.{yml,yaml}";
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

            try {
                Resource[] resources = resolver.getResources(pattern);

                for (Resource res : resources) {
                    try (InputStream in = res.getInputStream()) {
                        map.putAll(mapper.readValue(in, new TypeReference<Map<String, BaseEngineTemplate>>() {
                        }));

                    }
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to load classpath resources from " + pathDir, e);
            }

        } else {
            log.warn("Loading external disk resources from {}", pathDir);
            Path folderDir = Paths.get(pathDir);
            if(!Files.exists(folderDir) || !Files.isDirectory(folderDir)) {
                throw new IllegalStateException("Directory does not exist: " + folderDir);
            }
            try (Stream<Path> paths = Files.walk(folderDir)) {
                paths.filter(p -> p.toString().endsWith(".yml") || p.toString().endsWith(".yaml"))
                        .forEach(p -> {
                            try (InputStream in = Files.newInputStream(p)) {
                                map.putAll(mapper.readValue(in, new TypeReference<Map<String, BaseEngineTemplate>>() {
                                }));
                            } catch (IOException e) {
                                throw new RuntimeException("Failed to load file " + p, e);
                            }
                        });
            } catch (IOException e) {
                throw new RuntimeException("Error walking directory " + folderDir, e);
            }
        }

        log.warn("Loaded {} templates from {}", map.size(), pathDir);

        templates.putAll(map);
    }

    @Override
    public void loadTriggers() {
        var pathDir = this.properties.getTriggersPath();
        Assert.notNull(pathDir, "Directory is null");

        Map<String, Object> map = new ConcurrentHashMap<>();

        if(pathDir.startsWith("classpath:")) {
            log.warn("Loading trigers classpath resources from {}", pathDir);
            String pattern = "classpath*:" + pathDir.substring("classpath:".length()) + "/**/*.{yml,yaml}";
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

            try {
                Resource[] resources = resolver.getResources(pattern);

                for (Resource res : resources) {
                    try (InputStream in = res.getInputStream()) {
                        map.putAll(mapper.readValue(in, Map.class));
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to load classpath resources from " + pathDir, e);
            }

        } else {
            log.warn("Loading external disk triggers resources from {}", pathDir);
            Path folderDir = Paths.get(pathDir);
            if(!Files.exists(folderDir) || !Files.isDirectory(folderDir)) {
                throw new IllegalStateException("Directory does not exist: " + folderDir);
            }
            try (Stream<Path> paths = Files.walk(folderDir)) {
                paths.filter(p -> p.toString().endsWith(".yml") || p.toString().endsWith(".yaml"))
                        .forEach(p -> {
                            try (InputStream in = Files.newInputStream(p)) {
                                map.putAll(mapper.readValue(in, Map.class));
                            } catch (IOException e) {
                                throw new RuntimeException("Failed to load file " + p, e);
                            }
                        });
            } catch (IOException e) {
                throw new RuntimeException("Error walking directory " + folderDir, e);
            }
        }

        log.warn("Loaded {} triggers from {}", map.size(), pathDir);

        this.parseTriggerMapToRoutes(map);
    }

    @Override
    public boolean exists(String templateName) {
        return templates.containsKey(templateName);
    }

    @Override
    public List<EngineRoute> triggers() {
        return triggers;
    }

    @Override
    public Optional<BaseEngineTemplate> getTemplate(String templateName) {
        return Optional.ofNullable(templates.get(templateName));
    }
}
