package zw.co.dcl.jawce.engine.constants;

public class SessionConstant {
    //    === global ===
    public final static String SESSION_EXPIRY = "jSessionExpiry";
    public final static String REST_HOOK_USER_AUTH_KEY = "jHuak";
    public final static String CURRENT_STAGE_RETRY_COUNT = "jRetryCount";

    //    stages store
    public final static String PREV_STAGE = "jPrev";
    public final static String CURRENT_STAGE = "jCurrent";


    //    if chatbot has authenticated users, when user is authenticated successfully
//    set this key. To be used for inactivity and is authenticated checks
    public final static String AUTH_SET_KEY = "jAuth";

    /**
     * To feed a full dynamic template body
     * When complete, evict this key
     */
    public final static String DYNAMIC_CURRENT_TEMPLATE_BODY_KEY = "jCurrentDynamic";
    public final static String DYNAMIC_NEXT_TEMPLATE_BODY_KEY = "jPrevDynamic";


    public final static String LAST_ACTIVITY_KEY = "jLastActive";

    /**
     * if this key is null or empty, ignore message processing
     * <p>
     * it might be that the webhook resend an old msg id when the service was down
     * We wont know the stage the user was on, so ignore
     */
    public final static String CURRENT_MSG_ID_KEY = "jMsgId";

    public final static String CURRENT_DEBOUNCE_KEY = "jDebounce";

    /**
     * if this key is present in the session data,
     * the next-stage will be any stage configured here.
     * <p>
     * Use case: When an exception is encountered and you send a retry message
     * the flow to retry will be added to this key in session
     */
    public final static String SESSION_LATEST_CHECKPOINT_KEY = "jCheckpoint";

    /**
     * if its an error message with retry button, set this key in session
     * and clear it after processing
     */
    public final static String SESSION_DYNAMIC_RETRY_KEY = "jRetryDynamic";


    // session message history queue
    public final static String SESSION_MESSAGE_HISTORY_KEY = "jMsgHistory";
}
