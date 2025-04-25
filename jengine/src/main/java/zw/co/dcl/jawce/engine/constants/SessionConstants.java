package zw.co.dcl.jawce.engine.constants;

public class SessionConstants {
    //    === global ===
    public final static String SESSION_EXPIRY = "kJawceSessionExpiry";
    public final static String REST_HOOK_USER_AUTH_KEY = "kJawceUAK";
    public final static String CURRENT_STAGE_RETRY_COUNT = "kJawceRetryCount";

    //    stages store
    public final static String PREV_STAGE = "kJawcePrevStage";
    public final static String CURRENT_STAGE = "kJawceCurrStage";


    //    if chatbot has authenticated users, when user is authenticated successfully
//    set this key. To be used for inactivity and is authenticated checks
    public final static String ENGINE_AUTH_VALID_KEY = "kJawceUserAuthKey";

    /**
     * To feed a full dynamic template body
     * When complete, evict this key
     */
    public final static String DYNAMIC_CURRENT_TEMPLATE_BODY_KEY = "kJawceCurrentDynamicBody";
    public final static String DYNAMIC_NEXT_TEMPLATE_BODY_KEY = "kJawceNextDynamicBody";

    /**
     * service might have a msisdn check
     * set current user msisdn here which will be checked against whatsapp number
     * on auth hook
     * <p>
     * Should not have country code (+)
     */
    public final static String SERVICE_PROFILE_MSISDN_KEY = "kJawceProfileMsisdn";
    public final static String LAST_ACTIVITY_KEY = "kJawceUserLastActive";

    /**
     * if this key is null or empty, ignore message processing
     * <p>
     * it might be that the webhook resend an old msg id when the service was down
     * We wont know the stage the user was on, so ignore
     */
    public final static String CURRENT_MSG_ID_KEY = "kJawceMsgId";

    public final static String CURRENT_DEBOUNCE_KEY = "kJawceDebounceTs";

    /**
     * if this key is present in the session data,
     * the next-stage will be any stage configured here.
     * <p>
     * Use case: When an exception is encountered and you send a retry message
     * the flow to retry will be added to this key in session
     */
    public final static String SESSION_LATEST_CHECKPOINT_KEY = "kJawceCheckpoint";

    /**
     * if its an error message with retry button, set this key in session
     * and clear it after processing
     */
    public final static String SESSION_DYNAMIC_RETRY_KEY = "kJawceDynamicRetry";


    // session message history queue
    public final static String SESSION_MESSAGE_HISTORY_KEY = "kJawceMsgHistory";
}
