package zw.co.dcl.ehailing;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import zw.co.dcl.jawce.engine.api.iface.IClientManager;
import zw.co.dcl.jawce.engine.api.iface.ISessionManager;
import zw.co.dcl.jawce.engine.api.iface.ITemplateStorageManager;
import zw.co.dcl.jawce.engine.defaults.FileSessionManager;
import zw.co.dcl.jawce.engine.defaults.RestTemplateClientManager;
import zw.co.dcl.jawce.engine.defaults.YmlJsonTemplateStorageManager;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class EhailingApplicationTests {
    @Autowired
    private ISessionManager sessionManager;

    @Autowired
    private IClientManager clientManager;

    @Autowired
    private ITemplateStorageManager templateStorageManager;

    @Test
    void contextLoads() {
        assertNotNull(sessionManager);
        assertNotNull(clientManager);
        assertNotNull(templateStorageManager);
        assertInstanceOf(FileSessionManager.class, sessionManager);
        assertInstanceOf(RestTemplateClientManager.class, clientManager);
        assertInstanceOf(YmlJsonTemplateStorageManager.class, templateStorageManager);
    }

}
