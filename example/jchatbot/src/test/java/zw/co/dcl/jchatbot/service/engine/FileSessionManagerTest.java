package zw.co.dcl.jchatbot.service.engine;

import org.junit.jupiter.api.Test;
import zw.co.dcl.jchatbot.service.engine.FileSessionManager;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class FileSessionManagerTest {

    /**
     * Tests the singleton behavior of the getInstance method.
     * Ensures getInstance always returns the same instance of FileSessionManager.
     */
    @Test
    public void testSingletonInstance() {
        // Act
        FileSessionManager instance1 = FileSessionManager.getInstance();
        FileSessionManager instance2 = FileSessionManager.getInstance();

        // Assert
        assertNotNull(instance1, "First instance should not be null");
        assertNotNull(instance2, "Second instance should not be null");
        assertSame(instance1, instance2, "Both instances should be the same (singleton)");
    }

    /**
     * Tests that the session directory is properly created when the class is initialized.
     */
    @Test
    public void testSessionDirectoryIsCreated() {
        // Act
        FileSessionManager instance = FileSessionManager.getInstance();
        Path sessionDir = instance.getSESSION_DIR();

        // Assert
        assertNotNull(sessionDir, "Session directory path should not be null");
        assertTrue(Files.exists(sessionDir), "Session directory should exist");
        assertTrue(Files.isDirectory(sessionDir), "Session directory should be a directory");
    }

    /**
     * Tests that the global session file is properly created when the class is initialized.
     */
    @Test
    public void testGlobalSessionFileIsCreated() {
        // Act
        FileSessionManager instance = FileSessionManager.getInstance();
        Path globalSessionFile = instance.getGLOBAL_SESSION_FILE();

        // Assert
        assertNotNull(globalSessionFile, "Global session file path should not be null");
        assertTrue(Files.exists(globalSessionFile), "Global session file should exist");
        assertTrue(Files.isRegularFile(globalSessionFile), "Global session file should be a regular file");
    }

    /**
     * Tests that getInstance does not throw any exceptions during initialization.
     */
    @Test
    public void testGetInstanceDoesNotThrow() {
        assertDoesNotThrow(FileSessionManager::getInstance, "getInstance should not throw any exceptions");
    }

    /**
     * Tests that getInstance initializes the singleton only once, even when called from multiple threads.
     * This ensures thread-safety.
     */
    @Test
    public void testThreadSafeSingleton() {
        // Arrange
        FileSessionManager[] instances = new FileSessionManager[2];

        Thread thread1 = new Thread(() -> instances[0] = FileSessionManager.getInstance());
        Thread thread2 = new Thread(() -> instances[1] = FileSessionManager.getInstance());

        // Act
        thread1.start();
        thread2.start();

        try {
            thread1.join();
            thread2.join();
        } catch (InterruptedException e) {
            fail("Test threads were interrupted");
        }

        // Assert
        assertNotNull(instances[0], "First thread instance should not be null");
        assertNotNull(instances[1], "Second thread instance should not be null");
        assertSame(instances[0], instances[1], "Both instances should be the same (singleton)");
    }

    /**
     * Tests that cleanUp removes the session directory and all its contents.
     */
    @Test
    public void testCleanUpDeletesSessionDirectory() {
        // Arrange
        FileSessionManager instance = FileSessionManager.getInstance();
        Path sessionDir = instance.getSESSION_DIR();

        // Act
        instance.cleanUp();

        // Assert
        assertFalse(Files.exists(sessionDir), "Session directory should be deleted after cleanUp()");
    }
}
