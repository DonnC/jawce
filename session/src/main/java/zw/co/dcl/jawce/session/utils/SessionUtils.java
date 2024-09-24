package zw.co.dcl.jawce.session.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class SessionUtils {
    private static final Logger logger = LoggerFactory.getLogger(SessionUtils.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static <T> T castValue(Object fromObj, Class<T> type) {
        return objectMapper.convertValue(fromObj, type);
    }

    public static Map<String, Object> convertMap(Map<Object, Object> originalMap) {
        return originalMap
                .entrySet()
                .stream()
                .collect(Collectors.toMap(
                                entry -> entry.getKey().toString(),
                                Map.Entry::getValue
                        )
                );
    }


    static public Map<String, Object> byteToMap(byte[] bytes) {
        try {
            return objectMapper.readValue(bytes, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception e) {
            logger.warn("Failed to convert bytes to map: {}", e.getMessage());
            return new HashMap<>();
        }
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

    static public byte[] toBytes(Object object) {
        try {
            return objectMapper.writeValueAsBytes(object);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write bytes: " + e.getMessage());
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
