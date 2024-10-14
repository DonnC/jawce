package zw.co.dcl.jawce.engine.constants;

public class EngineConstants {
    public final static int MESSAGE_QUEUE_COUNT = 10;
    public final static String ENGINE_EXC_MSG_SPLITTER = "#qx#";  // 5000 | 263778000999 | failed to understand option
    public final static String REFL_CLS_METHOD_SPLITTER = ":";  // ..clsName:methodName
    public final static String TPL_TRIGGER_ROUTE_PARAM_KEY = "trigger-route";
    public final static String TPL_REST_HOOK_PLACEHOLDER_KEY = "rest:";
    public final static String TPL_CHECKPOINT_KEY = "checkpoint";

//    if present, acknowledge the message (blue-tick)
    public final static String TPL_READ_RECEIPT_KEY = "ack";

    public final static String TPL_PROP_KEY = "prop";
    public final static String REST_HOOK_DYNAMIC_ROUTE_KEY = "route";
    public final static String TPL_REGEX_PLACEHOLDER_KEY = "re:";
    public final static String TPL_AUTHENTICATED_KEY = "authenticated";
    public final static String TPL_ON_RECEIVE_KEY = "on-receive";
    public final static String TPL_REPLY_MESSAGE_ID_KEY = "message-id";
    public final static String TPL_ON_GENERATE_KEY = "on-generate";
    public final static String TPL_VALIDATOR_KEY = "validator";
    public final static String TPL_ROUTE_TRANSIENT_KEY = "transient";
    public final static String TPL_ROUTES_KEY = "routes";
    public final static String TPL_DYNAMIC_ROUTER_KEY = "router";
    public final static String TPL_MIDDLEWARE_KEY = "middleware";
    public final static String TPL_TEMPLATE_KEY = "template";
    public final static String TPL_METHOD_PARAMS_KEY = "params";
    public final static String X_HUB_SIG_HEADER_KEY = "x-hub-signature-256";

    public final static String DYNAMIC_BODY_STAGE_KEY = "ENGINE_DYNAMIC_BODY_STAGE";

    public final static int TIMEOUT_REQUEST_RETRY_COUNT = 2;

    public final static String RETRY_NAME = "Retry";

    //    --- dynamic
    public final static String DYNAMIC_LAST_TEMPLATE_PARAM = "DTPL_LAST_STAGE";

    //    https://graph.facebook.com/{{Version}}/{{Phone-Number-ID}}/messages
    public final static String CHANNEL_LOADING_REACTION = "🔄️";
    public final static String CHANNEL_BASE_URL = "https://graph.facebook.com/";
    public final static String CHANNEL_FLOW_VERSION = "3";
    public final static String CHANNEL_SUPPORTED_FLOW_ACTION = "navigate";
    public final static String CHANNEL_MESSAGE_SUFFIX = "/messages";
    public final static String CHANNEL_MEDIA_SUFFIX = "/media";
}
