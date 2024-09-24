package zw.co.dcl.jawce.session;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import zw.co.dcl.jawce.session.impl.FileBasedSessionManager;

import java.util.List;

import static org.springframework.test.util.AssertionErrors.*;

class FileBasedSessionManagerTests {
    private static FileBasedSessionManager session;

    @AfterAll
    static void tearDown() {
        session.deleteSessionFolder();
    }

    @BeforeEach
    void setUp() {
        session = FileBasedSessionManager.getInstance();
    }

    @Test
    public void testSingletonInstance() {
        FileBasedSessionManager instance1 = FileBasedSessionManager.getInstance();
        FileBasedSessionManager instance2 = FileBasedSessionManager.getInstance();
        assertEquals("The instances should be the same (singleton behavior)", instance1, instance2);
    }

    @Test
    void testCreateSessionAndAddData() {
        session.session("user1");

        // Add data to the session
        session.save("user1", "key1", "value1");

        // Retrieve and verify the data
        String result = session.get("user1", "key1", String.class);
        assertEquals("Data should match the expected value.", "value1", result);
    }

    @Test
    void testGlobalSession() {
        session.saveGlobal("globalKey", "globalValue");
        String result = session.getGlobal("globalKey", String.class);
        assertEquals("Global data should match the expected value.", "globalValue", result);
    }

    @Test
    void testSessionProps() {
        session.session("user2");
        session.saveProp("user2", "propKey", "propValue");

        String result = session.getFromProps("user2", "propKey", String.class);
        assertEquals("Property should match the expected value.", "propValue", result);
    }

    @Test
    void testSessionClearWithRetain() {
        session.session("user7");
        session.saveProp("user7", "k1", "v1");
        session.saveProp("user7", "k2", "v2");
        session.save("user7", "k3", "v3");
        session.save("user7", "k4", "v4");
        session.save("user7", "k5", "v5");

        session.clear("user7", List.of("k4", "k5"));

        assertNotNull("user7 k4 should be valid", session.get("user7", "k4", String.class));
        assertNotNull("user7 k5 should be valid", session.get("user7", "k5", String.class));

        assertNull("user7 k1 prop should be null", session.getFromProps("user7", "k1", String.class));
    }

    @Test
    void testSessionClear() {
        session.session("user8");
        session.save("user8", "k1", "v1");
        session.saveProp("user8", "k2", "v2");

        assertEquals("k1 should be v1", "v1", session.get("user8", "k1", String.class));

        session.clear("user8");

        assertNull("k1 should be null after clear", session.getFromProps("user8", "k1", String.class));
    }


    @Test
    void testClearUsersData() {
        session.clear("user1");
        session.clear("user2");

        var userAllData1 = session.fetchAll("user1");
        var userAllData2 = session.fetchAll("user2");

        assertTrue("Data 1 should return empty map", userAllData1.isEmpty());
        assertTrue("Data 2 should return empty map", userAllData2.isEmpty());
    }
}
