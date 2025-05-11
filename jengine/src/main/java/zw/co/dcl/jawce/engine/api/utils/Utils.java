package zw.co.dcl.jawce.engine.api.utils;

import lombok.extern.slf4j.Slf4j;
import zw.co.dcl.jawce.engine.constants.EngineConstant;
import zw.co.dcl.jawce.engine.model.dto.ShortcutVar;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class Utils {
    private static final DateTimeFormatter dtFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z");
    private static final String MUSTACHE_PATTERN = "\\{\\{\\s*([^}]*)\\s*\\}\\}";

    public static boolean containsMustacheVariables(String text) {
        return isRegexPatternMatch(MUSTACHE_PATTERN, text);
    }

    public static List<String> extractMustacheVariables(String text) {
        List<String> variables = new ArrayList<>();
        Matcher matcher = Pattern.compile(MUSTACHE_PATTERN).matcher(text);

        while (matcher.find()) {
            variables.add(matcher.group(1).trim());
        }
        return variables;
    }

    /**
     * shortcuts starts with
     * <p>
     * p.varName for prop data
     * <p>
     * s.varName for general session data
     */
    public static List<String> extractShortcutMustacheVariables(String text) {
        List<String> shortcutVar = new ArrayList<>();

        if(containsMustacheVariables(text)) {
            List<String> variables = extractMustacheVariables(text);

            variables.forEach(shortVar -> {
                if(shortVar.startsWith("p.") || shortVar.startsWith("s.")) {
                    shortcutVar.add(shortVar);
                }
            });
        }

        return shortcutVar;
    }

    public static ShortcutVar parseShortcutVar(String input) {
        // Split the input on the colon (:) to determine if a class type is provided
        String[] parts = input.split(":");

        var name = extractName(parts[0]);

        Class<?> classz = parts.length > 1 ? mapToClass(parts[1]) : null;

        return new ShortcutVar(name, classz);
    }

    private static String extractName(String fullName) {
        int lastDotIndex = fullName.lastIndexOf('.');
        return lastDotIndex != -1 ? fullName.substring(lastDotIndex + 1) : fullName;
    }

    private static Class<?> mapToClass(String className) {
        return switch (className.trim()) {
            case "String" -> String.class;
            case "Integer" -> Integer.class;
            case "Long" -> Long.class;
            case "Double" -> Double.class;
            case "Boolean" -> Boolean.class;
            case "List" -> List.class;
            case "Map" -> Map.class;
            default -> null;
        };
    }

    public static String formatZonedDateTime(ZonedDateTime dateTime) {
        return dateTime.format(dtFormatter);
    }

    public static ZonedDateTime parseZonedDateTime(String dateTimeString) {
        return ZonedDateTime.parse(dateTimeString, dtFormatter);
    }

    public static ZonedDateTime currentSystemDate() {
        return ZonedDateTime.now(ZoneId.systemDefault());
    }

    public static boolean hasInteractionExpired(String lastInteractionTime, int maxInteractionInMins) {
        if(lastInteractionTime == null) return false;
        return Duration.between(parseZonedDateTime(lastInteractionTime), currentSystemDate()).abs().toMinutes() > maxInteractionInMins;
    }

    public static boolean isRegexPatternMatch(String regexPattern, String text) {
        var p = regexPattern.replaceAll(EngineConstant.TPL_REGEX_PLACEHOLDER_KEY, "").trim();
        return Pattern.compile(p).matcher(text).find();
    }
}
