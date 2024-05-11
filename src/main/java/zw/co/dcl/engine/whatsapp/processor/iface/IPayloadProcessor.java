package zw.co.dcl.engine.whatsapp.processor.iface;

import java.util.Map;

public interface IPayloadProcessor {
    Map<String, Object> generatePayload();
}
