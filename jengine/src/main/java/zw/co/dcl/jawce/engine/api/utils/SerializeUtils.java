package zw.co.dcl.jawce.engine.api.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import zw.co.dcl.jawce.engine.model.abs.BaseEngineTemplate;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;

public class SerializeUtils {
    private static final Logger logger = LoggerFactory.getLogger(SerializeUtils.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    static public <T> T castValue(Object fromObj, Class<T> type) {
        return objectMapper.convertValue(fromObj, type);
    }

    static public Map<String, Object> toMap(Object object) {
        try {
            objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

            if(object instanceof String content) {
                return objectMapper.readValue(content, new TypeReference<Map<String, Object>>() {
                });
            }

            String json = objectMapper.writeValueAsString(object);
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception e) {
            logger.warn("[ENGINE] failed to convert obj to map: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    static public Map<String, Object> fromTemplate(BaseEngineTemplate template) {
        return objectMapper.convertValue(template, new TypeReference<>() {
        });
    }

    static public BaseEngineTemplate toTemplate(Map<String, Object> mapTemplate) {
        return SerializeUtils.castValue(mapTemplate, BaseEngineTemplate.class);
    }

    static public String toJsonString(Object object) throws JsonProcessingException {
        return objectMapper.writeValueAsString(object);
    }

    static public JsonNode readStringAsTree(String data) throws JsonProcessingException {
        return objectMapper.readTree(data);
    }

    static public Map<String, Object> readMapFromFile(File file) {
        try {
            return objectMapper.readValue(file, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception e) {
            logger.warn("Failed to read map from file: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    static public void writeToFile(File file, Object object) {
        try {
            file.createNewFile();
            objectMapper.writeValue(file, object);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create or write file: " + e.getMessage());
        }
    }

    public static void deleteDirectoryRecursively(Path path) throws IOException {
        if(Files.exists(path)) {
            Files.walkFileTree(path, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }
}
