package zw.co.dcl.engine.whatsapp.constants;

public class SessionConstants {
    //    === global ===
    public final static String SESSION_EXPIRY = "kEngineSessionExpiry";
    public final static String PREV_STAGE = "kEnginePrevStage";
    public final static String HOOK_USER_SESSION_ACCESS_TOKEN = "kEngineUAK";
    public final static String CURRENT_STAGE_RETRY_COUNT = "kRetryCount";

    /**
     * To feed a full dynamic template body
     * When complete, evict this key
     */
    public final static String DYNAMIC_CURRENT_TEMPLATE_BODY_KEY = "kCurrentDynamicBody";
    public final static String DYNAMIC_NEXT_TEMPLATE_BODY_KEY = "kNextDynamicBody";

    /**
     * service might have a msisdn check
     * set current user msisdn here which will be checked against whatsapp number
     * on auth hook
     */
    public final static String SERVICE_PROFILE_MSISDN_KEY = "kServiceMsisdn";

//    set a value if its from a session expired
//    can be used to set a dynamic message e.g
//    tell a user that: pardon interruption but session has expired unlike just
//    booting user back to login

    //    should call session.clearSessionExpiryEntry on successful login
    public final static String IS_FROM_ACTIVITY_EXPIRY = "kEngineActivityExpiry";
    public final static String LAST_ACTIVITY_KEY = "kEngineUserLastActive";

    public final static String CURRENT_STAGE = "kEngineCurrStage";

    //    value is a Map
    public final static String PROPS_KEY = "kEngineFlowProps";

    /**
     * if this key is null or empty, ignore message processing
     *
     * it might be that the webhook resend an old msg id when the service was down
     * We wont know the stage the user was on, so ignore
     */
    public final static String CURRENT_MSG_ID_KEY = "kCurrentMsgId";

    public final static String CURRENT_DEBOUNCE_KEY = "kDebounceTs";

    /**
     * if this key is present in the session data,
     * the next-stage will be any stage configured here.
     * <p>
     * Use case: When an exception is encountered and you send a retry message
     * the flow to retry will be added to this key in session
     */
    public final static String SESSION_LATEST_CHECKPOINT_KEY = "kStageCheckpoint";

    /**
     * if its an error message with retry button, set this key in session
     * and clear it after processing
     */
    public final static String SESSION_DYNAMIC_RETRY_KEY = "kDynamicRetry";


    // session message history queue
    public final static String SESSION_MESSAGE_HISTORY_KEY = "kMsgHistory";
}
