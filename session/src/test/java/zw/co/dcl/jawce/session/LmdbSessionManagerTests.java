package zw.co.dcl.jawce.session;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import zw.co.dcl.jawce.session.impl.LmdbSessionManager;

import java.util.List;

import static org.springframework.test.util.AssertionErrors.*;

class LmdbSessionManagerTests {
    private static LmdbSessionManager session;

    @AfterAll
    static void tearDown() {
        session.deleteSessionFolder();
    }

    @BeforeEach
    void setUp() {
        session = LmdbSessionManager.getInstance(null, null);
    }

    @Test
    public void testSingletonInstance() {
        // Retrieve two instances of the CaffeineSessionManager using the singleton pattern
        LmdbSessionManager instance1 = LmdbSessionManager.getInstance(null, null);
        LmdbSessionManager instance2 = LmdbSessionManager.getInstance(null, null);

        // Check that both instances are the same
        assertEquals("The instances should be the same (singleton behavior)", instance1, instance2);
    }

    @Test
    public void testDistinctUserSessions() {
        // Retrieve the singleton instance
        LmdbSessionManager sessionManager = LmdbSessionManager.getInstance(null, null);

        // Set some session data for different users
        sessionManager.save("user1", "key1", "User 1 Data");
        sessionManager.save("user2", "key1", "User 2 Data");

        // Retrieve the session data for each user
        String user1Data = (String) sessionManager.get("user1", "key1");
        String user2Data = (String) sessionManager.get("user2", "key1");

        // Assert that each user has distinct session data
        assertNotNull("User 1 data should not be null", user1Data);
        assertNotNull("User 2 data should not be null", user2Data);
        assertNotEquals("Each user should have their own distinct session data", user1Data, user2Data);
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
    void testGlobalSession() {
        // Save data to global session
        session.saveGlobal("globalKey", "globalValue");

        // Retrieve and verify global session data
        String result = session.getGlobal("globalKey", String.class);

        assertEquals("Global data should match the expected value.", "globalValue", result);
    }

    @Test
    void testSessionProps() {
        // Create a session for user "user2"
        session.session("user2");

        // Save a property to the session
        session.saveProp("user2", "propKey", "propValue");
        session.save("user2", "index", 30);

        // Retrieve and verify the property
        String result = session.getFromProps("user2", "propKey", String.class);

        assertEquals("Property should match the expected value.", "propValue", result);
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
