package zw.co.dcl.ehailing.service.engine;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import zw.co.dcl.jawce.engine.configs.FileSessionProperties;
import zw.co.dcl.jawce.engine.defaults.FileSessionManager;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class FileSessionManagerTest {
    @TempDir
    Path tempDir;

    private FileSessionManager currentManager;

    private FileSessionManager createManager() {
        FileSessionProperties properties = new FileSessionProperties();
        properties.setDir(tempDir.resolve("sessions").toString());
        currentManager = new FileSessionManager(properties);
        return currentManager;
    }

    @AfterEach
    void tearDown() {
        if (currentManager != null && Files.exists(currentManager.getSessionDir())) {
            currentManager.cleanUp();
        }
    }

    /**
     * Tests that a file session manager can be created from configuration properties.
     */
    @Test
    public void testManagerCreationFromProperties() {
        FileSessionManager instance = createManager();
        assertNotNull(instance, "Manager instance should not be null");
    }

    /**
     * Tests that the session directory is properly created when the class is initialized.
     */
    @Test
    public void testSessionDirectoryIsCreated() {
        FileSessionManager instance = createManager();
        Path sessionDir = instance.getSessionDir();

        assertNotNull(sessionDir, "Session directory path should not be null");
        assertTrue(Files.exists(sessionDir), "Session directory should exist");
        assertTrue(Files.isDirectory(sessionDir), "Session directory should be a directory");
    }

    /**
     * Tests that the global session file is properly created when the class is initialized.
     */
    @Test
    public void testGlobalSessionFileIsCreated() {
        FileSessionManager instance = createManager();
        Path globalSessionFile = instance.getGlobalSessionFile();

        assertNotNull(globalSessionFile, "Global session file path should not be null");
        assertTrue(Files.exists(globalSessionFile), "Global session file should exist");
        assertTrue(Files.isRegularFile(globalSessionFile), "Global session file should be a regular file");
    }

    /**
     * Tests that creation does not throw any exceptions during initialization.
     */
    @Test
    public void testCreationDoesNotThrow() {
        assertDoesNotThrow(this::createManager, "Manager creation should not throw any exceptions");
    }

    /**
     * Tests that cleanUp removes the session directory and all its contents.
     */
    @Test
    public void testCleanUpDeletesSessionDirectory() {
        FileSessionManager instance = createManager();
        Path sessionDir = instance.getSessionDir();
        instance.cleanUp();

        assertFalse(Files.exists(sessionDir), "Session directory should be deleted after cleanUp()");
    }
}
