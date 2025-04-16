package zw.co.dcl.jawce.engine.defaults;

import zw.co.dcl.jawce.engine.model.abs.AbsEngineTemplate;
import zw.co.dcl.jawce.engine.model.core.EngineRoute;
import zw.co.dcl.jawce.engine.service.iface.ITemplateStorageManager;
import zw.co.dcl.jawce.engine.utils.SerializeUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

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

    public YamlTemplateStorageManager(Path templatesFolderPath, Path triggersFolderPath) {
        templates = getResourceAsMap(templatesFolderPath);
        triggers = getResourceAsMap(triggersFolderPath);
    }

    private Map<String, Object> getResourceAsMap(Path folderDir) {
        Map<String, Object> map = new ConcurrentHashMap<>();

        try {
            if(Files.exists(folderDir) && Files.isDirectory(folderDir)) {
                try (Stream<Path> paths = Files.walk(folderDir)) {
                    paths.filter(filePath -> filePath.toString().endsWith(".yml") || filePath.toString().endsWith(".yaml"))
                            .forEach(filePath -> {
                                try {
                                    map.putAll(SerializeUtils.readPath(filePath));
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            });
                }
            } else {
                throw new RuntimeException("Templates / triggers directory does not exist: " + folderDir);
            }

        } catch (Exception err) {
            throw new RuntimeException("Error loading template", err);
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
