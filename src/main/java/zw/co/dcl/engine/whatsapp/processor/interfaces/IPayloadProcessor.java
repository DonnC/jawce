package zw.co.dcl.engine.whatsapp.processor.interfaces;

import java.util.Map;

public interface IPayloadProcessor {
    Map<String, Object> generatePayload();
}
