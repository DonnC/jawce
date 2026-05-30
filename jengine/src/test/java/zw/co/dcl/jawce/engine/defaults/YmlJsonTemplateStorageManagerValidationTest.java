package zw.co.dcl.jawce.engine.defaults;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import zw.co.dcl.jawce.engine.api.exceptions.InternalException;
import zw.co.dcl.jawce.engine.configs.TemplateStorageProperties;
import zw.co.dcl.jawce.engine.model.template.ListTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YmlJsonTemplateStorageManagerValidationTest {
    @TempDir
    Path tempDir;

    @Test
    void loadsValidTemplatesAndTriggers() throws Exception {
        TemplateStorageProperties properties = propertiesFor(
                """
                "START-MENU":
                  type: text
                  on-receive: "zw.co.dcl.jawce.engine.support.TestHooks.startOnReceive"
                  routes:
                    "open list": "LIST-MENU"
                  message: "Hello"
                "LIST-MENU":
                  type: list
                  message:
                    body: "Choose an option"
                    button: "Open"
                    sections:
                      "Main":
                        "item-1":
                          title: "Option 1"
                          description: "First option"
                  routes:
                    "item-1": "START-MENU"
                """,
                """
                "START-MENU": "re:(?i)hi"
                """
        );

        YmlJsonTemplateStorageManager manager = new YmlJsonTemplateStorageManager(properties);

        assertTrue(manager.exists("START-MENU"));
        assertInstanceOf(ListTemplate.class, manager.getTemplate("LIST-MENU").orElseThrow());
        assertEquals(1, manager.triggers().size());
        assertEquals("START-MENU", manager.triggers().get(0).getNextStage());
    }

    @Test
    void rejectsMissingRequiredTemplateMessageField() throws Exception {
        TemplateStorageProperties properties = propertiesFor(
                """
                "START-MENU":
                  type: text
                  routes:
                    "re:.*": "START-MENU"
                """,
                "{}"
        );

        InternalException ex = assertThrows(InternalException.class, () -> new YmlJsonTemplateStorageManager(properties));

        assertTrue(ex.getMessage().contains("text message is required"));
    }

    @Test
    void rejectsUnknownRouteTarget() throws Exception {
        TemplateStorageProperties properties = propertiesFor(
                """
                "START-MENU":
                  type: text
                  message: "Hello"
                  routes:
                    "re:.*": "MISSING-STAGE"
                """,
                "{}"
        );

        InternalException ex = assertThrows(InternalException.class, () -> new YmlJsonTemplateStorageManager(properties));

        assertTrue(ex.getMessage().contains("Unknown next stage MISSING-STAGE referenced by START-MENU"));
    }

    @Test
    void rejectsUnknownTriggerTarget() throws Exception {
        TemplateStorageProperties properties = propertiesFor(
                """
                "START-MENU":
                  type: text
                  message: "Hello"
                  routes:
                    "re:.*": "START-MENU"
                """,
                """
                "MISSING-STAGE": "re:^start$"
                """
        );

        InternalException ex = assertThrows(InternalException.class, () -> new YmlJsonTemplateStorageManager(properties));

        assertTrue(ex.getMessage().contains("Unknown trigger stage MISSING-STAGE"));
    }

    @Test
    void rejectsInvalidReflectiveHookPath() throws Exception {
        TemplateStorageProperties properties = propertiesFor(
                """
                "START-MENU":
                  type: text
                  on-receive: "zw.co.dcl.jawce.engine.support.TestHooks.missingMethod"
                  message: "Hello"
                  routes:
                    "re:.*": "START-MENU"
                """,
                "{}"
        );

        InternalException ex = assertThrows(InternalException.class, () -> new YmlJsonTemplateStorageManager(properties));

        assertTrue(ex.getMessage().contains("Invalid hook for stage START-MENU"));
    }

    @Test
    void acceptsHttpAndRelativeHookPaths() throws Exception {
        TemplateStorageProperties properties = propertiesFor(
                """
                "START-MENU":
                  type: dynamic
                  on-receive: "https://example.com/on-receive"
                  template: "/internal/render"
                  routes:
                    "re:.*": "START-MENU"
                """,
                "{}"
        );

        YmlJsonTemplateStorageManager manager = new YmlJsonTemplateStorageManager(properties);

        assertTrue(manager.exists("START-MENU"));
    }

    private TemplateStorageProperties propertiesFor(String templatesYaml, String triggersYaml) throws IOException {
        Path templatesDir = Files.createDirectories(tempDir.resolve("templates-" + System.nanoTime()));
        Path triggersDir = Files.createDirectories(tempDir.resolve("triggers-" + System.nanoTime()));

        Files.writeString(templatesDir.resolve("templates.yaml"), templatesYaml);
        Files.writeString(triggersDir.resolve("triggers.yaml"), triggersYaml);

        TemplateStorageProperties properties = new TemplateStorageProperties();
        properties.setTemplatesPath(templatesDir.toString());
        properties.setTriggersPath(triggersDir.toString());
        return properties;
    }
}
