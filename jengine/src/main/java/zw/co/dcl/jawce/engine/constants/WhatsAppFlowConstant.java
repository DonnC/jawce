package zw.co.dcl.jawce.engine.constants;

import java.util.Map;

public class WhatsAppFlowConstant {
    // --- common status codes
    public static final int SUCCESS_HTTP_CODE = 200;
    public static final int INVALID_SIGNATURE_HTTP_CODE = 432;
    public static final int INVALID_FLOW_TOKEN_HTTP_CODE = 427;
    public static final int CHANGED_PUBLIC_KEY_HTTP_CODE = 421;

    // --- common actions
    public static final String INIT_FLOW_ACTION = "INIT";
    public static final String BACK_FLOW_ACTION = "BACK";
    public static final String PING_FLOW_ACTION = "ping";
    public static final String DATA_EXCHANGE_FLOW_ACTION = "data_exchange";

    // --- common payloads
    public static final Map<String, Object> ACK_ERROR_PAYLOAD = Map.of("data", Map.of("acknowledged", true));
    public static final Map<String, Object> PING_PAYLOAD = Map.of("data", Map.of("status", "active"));

    public static final int GCM_TAG_LENGTH_BITS = 128;
    public static final String AES_ALGO = "AES";
    public static final String AES_CIPHER = "AES/GCM/NoPadding";
    public static final String RSA_OAEP = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding";
}
