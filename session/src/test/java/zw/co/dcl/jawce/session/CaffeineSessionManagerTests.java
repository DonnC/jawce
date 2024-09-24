package zw.co.dcl.jawce.session;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import zw.co.dcl.jawce.session.impl.CaffeineSessionManager;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.springframework.test.util.AssertionErrors.*;

public class CaffeineSessionManagerTests {
    private static CaffeineSessionManager session;

    @AfterAll
    static void tearDown() {
        session.deleteSessions();
    }

    @BeforeEach
    void setUp() {
        session = CaffeineSessionManager.getInstance(5, TimeUnit.MINUTES);
    }

    @Test
    public void testSingletonInstance() {
        CaffeineSessionManager instance1 = CaffeineSessionManager.getInstance(5, TimeUnit.MINUTES);
        CaffeineSessionManager instance2 = CaffeineSessionManager.getInstance(5, TimeUnit.MINUTES);
        assertEquals("The instances should be the same (singleton behavior)", instance1, instance2);
    }

    @Test
    public void testDistinctUserSessions() {
        CaffeineSessionManager sessionManager = CaffeineSessionManager.getInstance(5, TimeUnit.MINUTES);

        sessionManager.save("user1", "key1", "User 1 Data");
        sessionManager.save("user2", "key1", "User 2 Data");

        String user1Data = (String) sessionManager.get("user1", "key1");
        String user2Data = (String) sessionManager.get("user2", "key1");

        assertNotNull("User 1 data should not be null", user1Data);
        assertNotNull("User 2 data should not be null", user2Data);
        assertNotEquals("Each user should have their own distinct session data", user1Data, user2Data);
    }

    @Test
    void testCreateSessionAndAddData() {
        session.session("user1");
        session.save("user1", "key1", "value1");
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
    void testSessionAddAndClearProps() {
        session.session("user3");
        session.saveProp("user3", "propKey3", "propValue3");
        String result = session.getFromProps("user3", "propKey3", String.class);

        assertEquals("Property should match the expected value.", "propValue3", result);

        session.evictProp("user3", "propKey3");

        result = session.getFromProps("user3", "propKey3", String.class);
        assertNull("There should not be a prop after clear", result);
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
    void testUserSessionMismatch() {
        var user4 = session.session("user4");
        var user5 = session.session("user5");

        user4.save("user4", "propKey", "4");
        user5.save("user5", "propKey", "5");

        String user4Result = session.get("user4", "propKey", String.class);
        String user5Result = session.get("user5", "propKey", String.class);

        assertEquals("Property should match the expected value.", "4", user4Result);
        assertEquals("Property should match the expected value.", "5", user5Result);
        assertNotEquals("User4 should not see user5 data", "5", user4Result);
        assertNotEquals("User5 should not see user4 data", "4", user5Result);
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
