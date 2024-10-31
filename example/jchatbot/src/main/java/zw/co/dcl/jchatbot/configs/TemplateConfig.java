package zw.co.dcl.jchatbot.configs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

@Configuration
@Slf4j
public class TemplateConfig {
    private final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Value("${resources.templates}")
    private String botTemplatesDir;
    @Value("${resources.triggers}")
    private String botTriggersDir;
    @Value("${resources.watcher}")
    private String watchDir;

    @Getter
    private Map<String, Object> botTemplates = new ConcurrentHashMap<>();

    @Getter
    private Map<String, Object> botTriggers = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        this.botTemplates = getResourceAsMap(botTemplatesDir);
        this.botTriggers = getResourceAsMap(botTriggersDir);

        log.warn("Templates loaded");
        startFileWatcher();
    }

    private Map<String, Object> getResourceAsMap(String pathDir) {
        Map<String, Object> map = new ConcurrentHashMap<>();
        try {
            Path folderDir = Paths.get(pathDir);

            if(Files.exists(folderDir) && Files.isDirectory(folderDir)) {
                try (Stream<Path> paths = Files.walk(folderDir)) {
                    paths.filter(filePath -> filePath.toString().endsWith(".yml") || filePath.toString().endsWith(".yaml"))
                            .forEach(filePath -> {
                                try {
                                    log.warn(">> Loading file: {}", filePath.getFileName());
                                    map.putAll(mapper.readValue(Files.newInputStream(filePath), Map.class));
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            });
                }
            } else {
                throw new RuntimeException("Templates directory does not exist: " + folderDir);
            }

        } catch (Exception err) {
            log.error("Error loading template: {}", err.getMessage());
            throw new RuntimeException("Error loading template", err);
        }
        return map;
    }

    private void reloadTemplatesAndTriggers() {
        this.botTemplates.clear();
        this.botTriggers.clear();
        this.botTemplates.putAll(getResourceAsMap(botTemplatesDir));
        this.botTriggers.putAll(getResourceAsMap(botTriggersDir));
    }

    private void startFileWatcher() {
        executorService.submit(() -> {
            try (WatchService watchService = FileSystems.getDefault().newWatchService()) {
                var watchFile = Paths.get(watchDir);
                watchFile.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);

                log.warn("Started watching dir: {}", watchDir);

                while (true) {
                    WatchKey key = watchService.take();
                    for (WatchEvent<?> event : key.pollEvents()) {
                        WatchEvent.Kind<?> kind = event.kind();
                        if(kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                            WatchEvent<Path> ev = (WatchEvent<Path>) event;
                            Path fileName = ev.context();
                            log.warn("File change detected in {} >> reloading all templates...", fileName.getFileName());
                            reloadTemplatesAndTriggers();
                        }
                    }
                    key.reset();
                }
            } catch (Exception e) {
                log.error("Error in file watcher: {} ** ", e.getMessage(), e);
            }
        });
    }
}
