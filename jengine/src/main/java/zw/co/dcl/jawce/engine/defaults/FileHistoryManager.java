package zw.co.dcl.jawce.engine.defaults;

import lombok.extern.slf4j.Slf4j;
import zw.co.dcl.jawce.engine.api.iface.IHistoryManager;
import zw.co.dcl.jawce.engine.api.utils.SerializeUtils;
import zw.co.dcl.jawce.engine.configs.HistoryProperties;
import zw.co.dcl.jawce.engine.model.history.ChatHistoryEvent;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

@Slf4j
public class FileHistoryManager implements IHistoryManager {
    private static final DateTimeFormatter FILE_TS_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final HistoryProperties properties;
    private final Path baseDir;
    private final AtomicInteger fileCounter = new AtomicInteger();
    private Path currentFile;

    public FileHistoryManager(HistoryProperties properties) {
        this.properties = properties;
        this.baseDir = Path.of(properties.getDir()).toAbsolutePath().normalize();
        ensureBaseDir();
    }

    @Override
    public synchronized void record(ChatHistoryEvent event) {
        try {
            String line = SerializeUtils.toJsonString(event) + System.lineSeparator();
            byte[] content = line.getBytes(StandardCharsets.UTF_8);

            Path target = resolveWritableFile(content.length);
            Files.write(target, content, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND);
            currentFile = target;
            pruneOldFiles();
        } catch (Exception e) {
            log.warn("Failed to persist history event: {}", e.getMessage());
        }
    }

    private void ensureBaseDir() {
        try {
            Files.createDirectories(baseDir);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create history directory: " + baseDir, e);
        }
    }

    private Path resolveWritableFile(int nextRecordSizeBytes) throws IOException {
        if(currentFile != null && Files.exists(currentFile) && !shouldRotate(currentFile, nextRecordSizeBytes)) {
            return currentFile;
        }

        Path existing = latestWritableExistingFile(nextRecordSizeBytes);
        if(existing != null) {
            return existing;
        }

        return newFilePath();
    }

    private Path latestWritableExistingFile(int nextRecordSizeBytes) throws IOException {
        try (Stream<Path> files = listHistoryFiles()) {
            return files
                    .filter(Files::isRegularFile)
                    .sorted(Comparator.comparing(this::lastModifiedSafe).reversed())
                    .filter(path -> !shouldRotate(path, nextRecordSizeBytes))
                    .findFirst()
                    .orElse(null);
        }
    }

    private boolean shouldRotate(Path path, int nextRecordSizeBytes) {
        try {
            return Files.exists(path) && (Files.size(path) + nextRecordSizeBytes > properties.getMaxFileSizeBytes());
        } catch (IOException e) {
            return true;
        }
    }

    private Path newFilePath() {
        String timestamp = LocalDateTime.now().format(FILE_TS_FORMAT);
        String filename = "%s-%s-%05d.ndjson".formatted(
                properties.getFilePrefix(),
                timestamp,
                fileCounter.incrementAndGet()
        );
        return baseDir.resolve(filename);
    }

    private void pruneOldFiles() throws IOException {
        if(properties.getMaxFiles() <= 0) return;

        List<Path> files;
        try (Stream<Path> stream = listHistoryFiles()) {
            files = stream
                    .filter(Files::isRegularFile)
                    .sorted(Comparator.comparing(this::lastModifiedSafe).reversed())
                    .toList();
        }

        for(int i = properties.getMaxFiles(); i < files.size(); i++) {
            Files.deleteIfExists(files.get(i));
        }
    }

    private Stream<Path> listHistoryFiles() throws IOException {
        return Files.list(baseDir)
                .filter(path -> path.getFileName().toString().startsWith(properties.getFilePrefix()))
                .filter(path -> path.getFileName().toString().endsWith(".ndjson"));
    }

    private java.nio.file.attribute.FileTime lastModifiedSafe(Path path) {
        try {
            return Files.getLastModifiedTime(path);
        } catch (IOException e) {
            return java.nio.file.attribute.FileTime.fromMillis(0);
        }
    }
}
