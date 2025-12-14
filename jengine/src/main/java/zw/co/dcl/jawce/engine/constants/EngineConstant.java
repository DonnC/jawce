package zw.co.dcl.jawce.engine.constants;

public class EngineConstant {
    public static final String SESSION_ID_HEADER_KEY = "X-WA-ID";
    public static final String MDC_WA_ID_KEY = "waId";
    public static final String MDC_WA_NAME_KEY = "waName";

    public final static int MESSAGE_QUEUE_COUNT = 10;
    public final static String TPL_REGEX_PLACEHOLDER_KEY = "re:";

    //    --- dynamic
    public final static String DYNAMIC_LAST_TEMPLATE_PARAM = "DTPL_LAST_STAGE";
    public final static String DYNAMIC_BODY_STAGE_KEY = "ENGINE_DYNAMIC_BODY_STAGE";

    //    https://graph.facebook.com/{{Version}}/{{Phone-Number-ID}}/messages
    public final static String CHANNEL_BASE_URL = "https://graph.facebook.com/";
    public final static String CHANNEL_FLOW_VERSION = "3";
    public final static String CHANNEL_SUPPORTED_FLOW_ACTION = "navigate";
    public final static String CHANNEL_MESSAGE_SUFFIX = "/messages";
    public final static String CHANNEL_MEDIA_SUFFIX = "/media";

    // --- default
    public final static String BTN_RETRY = "Retry";
    public final static String BTN_MENU = "Menu";
    public final static String BTN_REPORT = "Report";
}
