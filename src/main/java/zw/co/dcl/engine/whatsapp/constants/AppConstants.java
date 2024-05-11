package zw.co.dcl.engine.whatsapp.constants;

public class AppConstants {
    public final static String COMMAND_MESG_PREFIX = "/"; // all command msgs starts with /<command> <desc>
    public final static String ENGINE_EXC_MSG_SPLITTER = "#qx#";  // 5000 | 263778000999 | failed to understand option
    public final static String REFL_CLS_METHOD_SPLITTER = ":";  // UserService:getAccounts
    public final static String TPL_REST_HOOK_PLACEHOLDER_KEY = "rest:";
    public final static String TPL_PROP_KEY = "prop";
    public final static String REST_HOOK_DYNAMIC_ROUTE_KEY = "route";
    public final static String TPL_REGEX_PLACEHOLDER_KEY = "re:";
    public final static String TPL_AUTHENTICATED_KEY = "authenticated";
    public final static String TPL_ON_RECEIVE_KEY = "on-receive";
    public final static String TPL_ON_GENERATE_KEY = "on-generate";
    public final static String TPL_VALIDATOR_KEY = "validator";
    public final static String TPL_ROUTES_KEY = "routes";
    public final static String TPL_DYNAMIC_ROUTER_KEY = "router";
    public final static String TPL_MIDDLEWARE_KEY = "middleware";
    public final static String TPL_TEMPLATE_KEY = "template";
    public final static String X_HUB_SIG_HEADER_KEY = "x-hub-signature-256";
    public final static  String X_WA_ENGINE_HEADER_KEY = "X-WA-ENGINE-KEY";

    //    https://graph.facebook.com/{{Version}}/{{Phone-Number-ID}}/messages
    public final static String CHANNEL_LOADING_REACTION = "🔄️";
    public final static String CHANNEL_BASE_URL = "https://graph.facebook.com/";
    public final static String CHANNEL_FLOW_VERSION = "3";
    public final static String CHANNEL_SUPPORTED_FLOW_ACTION = "navigate";
    public final static String CHANNEL_URL_SUFFIX = "/messages";
}
