package zw.co.dcl.jawce.engine.defaults;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.util.Assert;
import zw.co.dcl.jawce.engine.api.exceptions.InternalException;
import zw.co.dcl.jawce.engine.api.iface.ITemplateStorageManager;
import zw.co.dcl.jawce.engine.configs.TemplateStorageProperties;
import zw.co.dcl.jawce.engine.model.abs.BaseEngineTemplate;
import zw.co.dcl.jawce.engine.model.core.EngineRoute;
import zw.co.dcl.jawce.engine.model.messages.*;
import zw.co.dcl.jawce.engine.model.template.*;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
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
        validateTemplates(templates);
        log.info("Loaded {} templates from {}", loaded.size(), pathDir);
    }

    Map<String, BaseEngineTemplate> parseInput(InputStream in, String filename) throws IOException {
        try {
            if (filename.endsWith(".json")) {
                return jsonMapper.readValue(in, new TypeReference<Map<String, BaseEngineTemplate>>() {});
            }

            if (filename.endsWith(".yaml") || filename.endsWith(".yml")) {
                return yamlMapper.readValue(in, new TypeReference<Map<String, BaseEngineTemplate>>() {});
            }

            throw new IllegalArgumentException("Unsupported file type: " + filename);
        } catch (IllegalArgumentException e) {
            throw new InternalException("Invalid template in " + filename + ": " + e.getMessage(), e);
        }
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
        validateTriggers();
        log.info("Loaded {} triggers from {}", map.size(), pathDir);
    }

    void validateTemplates(Map<String, BaseEngineTemplate> loadedTemplates) {
        loadedTemplates.forEach((stage, template) -> {
            validateTemplateShape(stage, template);
            validateHooks(stage, template);
            validateRoutes(stage, template, loadedTemplates);
        });
    }

    void validateTemplateShape(String stage, BaseEngineTemplate template) {
        if (template == null || template.getType() == null) {
            throw new InternalException("Invalid template for stage " + stage + ": missing type");
        }

        if (template.getRoutes() == null || template.getRoutes().isEmpty()) {
            throw new InternalException("Invalid template for stage " + stage + ": missing routes");
        }

        if (template instanceof TextTemplate textTemplate) {
            require(stage, textTemplate.getMessage() != null && !textTemplate.getMessage().isBlank(), "text message is required");
        } else if (template instanceof ButtonTemplate buttonTemplate) {
            ButtonMessage message = buttonTemplate.getMessage();
            require(stage, message != null, "button message is required");
            require(stage, message.getBody() != null && !message.getBody().isBlank(), "button body is required");
            require(stage, message.getButtons() != null && !message.getButtons().isEmpty(), "button options are required");
        } else if (template instanceof ListTemplate listTemplate) {
            ListMessage message = listTemplate.getMessage();
            require(stage, message != null, "list message is required");
            require(stage, message.getBody() != null && !message.getBody().isBlank(), "list body is required");
            require(stage, message.getButton() != null && !message.getButton().isBlank(), "list button label is required");
            require(stage, message.getSections() != null && !message.getSections().isEmpty(), "list sections are required");
        } else if (template instanceof CtaTemplate ctaTemplate) {
            CtaMessage message = ctaTemplate.getMessage();
            require(stage, message != null, "cta message is required");
            require(stage, message.getBody() != null && !message.getBody().isBlank(), "cta body is required");
            require(stage, message.getButton() != null && !message.getButton().isBlank(), "cta button is required");
            require(stage, message.getUrl() != null && !message.getUrl().isBlank(), "cta url is required");
        } else if (template instanceof FlowTemplate flowTemplate) {
            FlowMessage message = flowTemplate.getMessage();
            require(stage, message != null, "flow message is required");
            require(stage, message.getBody() != null && !message.getBody().isBlank(), "flow body is required");
            require(stage, message.getButton() != null && !message.getButton().isBlank(), "flow button is required");
            require(stage, message.getFlowId() != null && !message.getFlowId().isBlank(), "flow id is required");
            require(stage, message.getName() != null && !message.getName().isBlank(), "flow screen name is required");
        } else if (template instanceof LocationTemplate locationTemplate) {
            LocationMessage message = locationTemplate.getMessage();
            require(stage, message != null, "location message is required");
            require(stage, message.getLat() != null && !message.getLat().isBlank(), "location latitude is required");
            require(stage, message.getLon() != null && !message.getLon().isBlank(), "location longitude is required");
        } else if (template instanceof MediaTemplate mediaTemplate) {
            MediaMessage message = mediaTemplate.getMessage();
            require(stage, message != null, "media message is required");
            require(stage, message.getType() != null && !message.getType().isBlank(), "media type is required");
            require(stage, (message.getMediaId() != null && !message.getMediaId().isBlank()) || (message.getUrl() != null && !message.getUrl().isBlank()), "media id or url is required");
        } else if (template instanceof RequestLocationTemplate requestLocationTemplate) {
            require(stage, requestLocationTemplate.getMessage() != null && !requestLocationTemplate.getMessage().isBlank(), "request-location message is required");
        } else if (template instanceof TemplateTemplate templateTemplate) {
            TemplateMessage message = templateTemplate.getMessage();
            require(stage, message != null, "template message is required");
            require(stage, message.getName() != null && !message.getName().isBlank(), "template name is required");
        } else if (template instanceof DynamicTemplate) {
            // current engine allows hook-driven dynamic templates, so only route presence is enforced here
        } else {
            throw new InternalException("Invalid template for stage " + stage + ": unsupported type " + template.getType());
        }
    }

    void validateHooks(String stage, BaseEngineTemplate template) {
        for (String hook : Arrays.asList(template.getOnReceive(), template.getOnGenerate(), template.getRouter(), template.getMiddleware(), template.getTemplate())) {
            if (hook != null) {
                validateHookPath(stage, hook);
            }
        }
    }

    void validateHookPath(String stage, String hookPath) {
        String lower = hookPath.toLowerCase();
        if (lower.startsWith("/") || lower.startsWith("http://") || lower.startsWith("https://")) {
            return;
        }

        int lastDot = hookPath.lastIndexOf('.');
        if (lastDot <= 0 || lastDot == hookPath.length() - 1) {
            throw new InternalException("Invalid hook for stage " + stage + ": " + hookPath);
        }

        String className = hookPath.substring(0, lastDot);
        String methodName = hookPath.substring(lastDot + 1);

        try {
            Class<?> hookClass = Class.forName(className);
            boolean found = Arrays.stream(hookClass.getDeclaredMethods())
                    .map(Method::getName)
                    .anyMatch(methodName::equals);

            if (!found) {
                throw new InternalException("Invalid hook for stage " + stage + ": " + hookPath);
            }
        } catch (ClassNotFoundException e) {
            throw new InternalException("Invalid hook for stage " + stage + ": " + hookPath, e);
        }
    }

    void validateRoutes(String stage, BaseEngineTemplate template, Map<String, BaseEngineTemplate> loadedTemplates) {
        template.getRoutes().forEach(route -> {
            if (!loadedTemplates.containsKey(route.getNextStage())) {
                throw new InternalException("Unknown next stage " + route.getNextStage() + " referenced by " + stage);
            }
        });
    }

    void validateTriggers() {
        triggers.forEach(trigger -> {
            if (!templates.containsKey(trigger.getNextStage())) {
                throw new InternalException("Unknown trigger stage " + trigger.getNextStage());
            }
        });
    }

    void require(String stage, boolean condition, String message) {
        if (!condition) {
            throw new InternalException("Invalid template for stage " + stage + ": " + message);
        }
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
