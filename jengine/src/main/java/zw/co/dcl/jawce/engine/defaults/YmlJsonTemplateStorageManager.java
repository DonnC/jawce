package zw.co.dcl.jawce.engine.defaults;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
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
 * Basic YAML/JSON template storage manager intended as a default/reference implementation.
 *
 * Applications can replace this by providing their own ITemplateStorageManager bean.
 */
@Slf4j
public class YmlJsonTemplateStorageManager implements ITemplateStorageManager {
    private final Map<String, BaseEngineTemplate> templates = new ConcurrentHashMap<>();
    private final List<EngineRoute> triggers = new CopyOnWriteArrayList<>();
    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
    private final ObjectMapper jsonMapper = new ObjectMapper();
    private final TemplateStorageProperties properties;

    public YmlJsonTemplateStorageManager(TemplateStorageProperties properties) {
        this.properties = properties;
        this.loadTemplates();
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
        templates.clear();

        Map<String, BaseEngineTemplate> loaded = new ConcurrentHashMap<>();

        if (pathDir.startsWith("classpath:")) {
            String pattern = "classpath*:" + pathDir.substring("classpath:".length()) + "/**/*.{yml,yaml,json}";
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

            try {
                Resource[] resources = resolver.getResources(pattern);

                for (Resource res : resources) {
                    try (InputStream in = res.getInputStream()) {
                        loaded.putAll(parseInput(in, res.getFilename()));
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to load classpath resources from " + pathDir, e);
            }
        } else {
            Path folderDir = Paths.get(pathDir);
            if (!Files.exists(folderDir) || !Files.isDirectory(folderDir)) {
                throw new IllegalStateException("Directory does not exist: " + folderDir);
            }

            try (Stream<Path> paths = Files.walk(folderDir)) {
                paths.filter(p -> p.toString().endsWith(".yml") || p.toString().endsWith(".yaml") || p.toString().endsWith(".json"))
                        .forEach(p -> {
                            try (InputStream in = Files.newInputStream(p)) {
                                loaded.putAll(parseInput(in, p.toString()));
                            } catch (IOException e) {
                                throw new RuntimeException("Failed to load file " + p, e);
                            }
                        });
            } catch (IOException e) {
                throw new RuntimeException("Error walking directory " + folderDir, e);
            }
        }

        templates.putAll(loaded);
        log.info("Loaded {} templates from {}", loaded.size(), pathDir);
    }

    Map<String, BaseEngineTemplate> parseInput(InputStream in, String filename) throws IOException {
        if (filename.endsWith(".json")) {
            return jsonMapper.readValue(in, new TypeReference<Map<String, BaseEngineTemplate>>() {});
        }

        if (filename.endsWith(".yaml") || filename.endsWith(".yml")) {
            return yamlMapper.readValue(in, new TypeReference<Map<String, BaseEngineTemplate>>() {});
        }

        throw new IllegalArgumentException("Unsupported file type: " + filename);
    }

    @Override
    public void loadTriggers() {
        var pathDir = this.properties.getTriggersPath();
        Assert.notNull(pathDir, "Directory is null");
        triggers.clear();

        Map<String, Object> map = new ConcurrentHashMap<>();

        if (pathDir.startsWith("classpath:")) {
            String pattern = "classpath*:" + pathDir.substring("classpath:".length()) + "/**/*.{yml,yaml,json}";
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

            try {
                Resource[] resources = resolver.getResources(pattern);

                for (Resource res : resources) {
                    try (InputStream in = res.getInputStream()) {
                        if (res.getFilename().endsWith(".json")) {
                            map.putAll(jsonMapper.readValue(in, Map.class));
                        } else {
                            map.putAll(yamlMapper.readValue(in, Map.class));
                        }
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to load classpath resources from " + pathDir, e);
            }
        } else {
            Path folderDir = Paths.get(pathDir);
            if (!Files.exists(folderDir) || !Files.isDirectory(folderDir)) {
                throw new IllegalStateException("Directory does not exist: " + folderDir);
            }

            try (Stream<Path> paths = Files.walk(folderDir)) {
                paths.filter(p -> p.toString().endsWith(".yml") || p.toString().endsWith(".yaml") || p.toString().endsWith(".json"))
                        .forEach(p -> {
                            try (InputStream in = Files.newInputStream(p)) {
                                if (p.toString().endsWith(".json")) {
                                    map.putAll(jsonMapper.readValue(in, Map.class));
                                } else {
                                    map.putAll(yamlMapper.readValue(in, Map.class));
                                }
                            } catch (IOException e) {
                                throw new RuntimeException("Failed to load file " + p, e);
                            }
                        });
            } catch (IOException e) {
                throw new RuntimeException("Error walking directory " + folderDir, e);
            }
        }

        this.parseTriggerMapToRoutes(map);
        log.info("Loaded {} triggers from {}", map.size(), pathDir);
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
