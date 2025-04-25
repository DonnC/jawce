package zw.co.dcl.jawce.engine.processor.abstracts;

import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import zw.co.dcl.jawce.engine.configs.EngineConfig;
import zw.co.dcl.jawce.engine.constants.EngineConstants;
import zw.co.dcl.jawce.engine.constants.SessionConstants;
import zw.co.dcl.jawce.engine.enums.WebhookResponseMessageType;
import zw.co.dcl.jawce.engine.exceptions.EngineInternalException;
import zw.co.dcl.jawce.engine.exceptions.EngineResponseException;
import zw.co.dcl.jawce.engine.model.abs.AbsEngineTemplate;
import zw.co.dcl.jawce.engine.model.core.HookArg;
import zw.co.dcl.jawce.engine.model.dto.ChannelUserInput;
import zw.co.dcl.jawce.engine.model.dto.MsgProcessorDTO;
import zw.co.dcl.jawce.engine.service.HookService;
import zw.co.dcl.jawce.engine.service.iface.ISessionManager;
import zw.co.dcl.jawce.engine.utils.CommonUtils;

import java.util.HashMap;
import java.util.Map;

public abstract class ChannelMessageProcessor {
    protected final MsgProcessorDTO dto;
    protected final EngineConfig config;
    private final Logger logger = LoggerFactory.getLogger(ChannelMessageProcessor.class);
    protected AbsEngineTemplate currentTemplate;
    protected ISessionManager session;
    protected ChannelUserInput currentStageUserInput = null;
    protected boolean isFirstTime = false;
    @Getter
    @Setter
    protected boolean isFromTrigger = false;
    protected boolean byPassSession = false;
    protected HookArg hookArg = null;
    private String stage;
    private Map<String, Object> triggerParams;
    private String sessionId;
    private HookService hookService;

    protected ChannelMessageProcessor(MsgProcessorDTO dto, EngineConfig config) {
        this.triggerParams = new HashMap<>();
        this.config = config;
        this.hookService = new HookService(this.config);
        this.dto = dto;
        this.sessionId = dto.user().waId();
        this.session = this.config.getSessionManager().session(this.sessionId);
        this.getCurrentStageTemplate();
        this.getMessageBody();
        this.processStageTrigger();
        this.checkSessionByPass();
        this.saveCheckpoint();

        HookArg arg = new HookArg();
        arg.setSession(this.session);
        arg.setWaUser(dto.user());
        arg.setUserInput(this.currentStageUserInput.input());
        arg.setAdditionalData(this.currentStageUserInput.additionalData());
        this.hookArg = arg;
    }

    private void saveCheckpoint() {
        if(this.currentTemplate.isCheckpoint()) {
            this.session.save(this.sessionId, SessionConstants.SESSION_LATEST_CHECKPOINT_KEY, stage);
        }
    }

    private void checkSessionByPass() {
        if(!this.currentTemplate.isSession()) {
            this.isFromTrigger = false;
            this.session.save(this.sessionId, SessionConstants.CURRENT_STAGE, this.stage);
        }
    }

    protected boolean hasEngineFullDynamicTemplateBody(String key) {
        if(this.session.get(sessionId, key) != null)
            return this.session.get(sessionId, key) instanceof Map dynamicTpl;
        return false;
    }

    private void getCurrentStageTemplate() {
        var currentStage = this.session.get(sessionId, SessionConstants.CURRENT_STAGE, String.class);

        if(currentStage == null) {
            if(this.config.getStorageManager().exists(this.config.getStartTemplateName())) {
                throw new EngineInternalException("start menu with stage: " + this.config.getStartTemplateName() + " not found");
            }

            this.currentTemplate = this.config.getStorageManager()
                    .getTemplate(this.config.getStartTemplateName())
                    .orElseThrow(() -> new EngineInternalException(this.config.getStartTemplateName() + " stage not found"));
            this.session.save(sessionId, SessionConstants.CURRENT_STAGE, this.config.getStartTemplateName());
            this.session.save(sessionId, SessionConstants.PREV_STAGE, this.config.getStartTemplateName());
            this.isFirstTime = true;
            this.stage = this.config.getStartTemplateName();
            return;
        }

        if(hasEngineFullDynamicTemplateBody(SessionConstants.DYNAMIC_CURRENT_TEMPLATE_BODY_KEY)) {
            this.stage = EngineConstants.DYNAMIC_BODY_STAGE_KEY;
//            this.currentTemplate = this.session.get(
//                    sessionId,
//                    SessionConstants.DYNAMIC_CURRENT_TEMPLATE_BODY_KEY,
//                    Map.class
//            );
            return;
        }

        if(!hasEngineFullDynamicTemplateBody(SessionConstants.DYNAMIC_CURRENT_TEMPLATE_BODY_KEY) &&
                hasEngineFullDynamicTemplateBody(SessionConstants.DYNAMIC_NEXT_TEMPLATE_BODY_KEY)
        ) {
            Map<String, Object> lastDynamicTplMessage = this.session.get(
                    sessionId,
                    SessionConstants.DYNAMIC_NEXT_TEMPLATE_BODY_KEY,
                    Map.class
            );

//            if(isDynamicBodyTemplateLastStage(lastDynamicTplMessage)) {
//                this.stage = currentStage;
//                this.currentTemplate = lastDynamicTplMessage;
//                this.session.evict(sessionId, SessionConstants.DYNAMIC_NEXT_TEMPLATE_BODY_KEY);
//                return;
//            }
        }

        if(!this.config.getStorageManager().exists(this.config.getStartTemplateName())) {
            throw new EngineInternalException("route: " + currentStage + " not defined in template context map");
        }

        this.stage = currentStage;
        this.currentTemplate = this.config.getStorageManager()
                .getTemplate(this.config.getStartTemplateName())
                .orElseThrow(() -> new EngineInternalException(this.config.getStartTemplateName() + " stage not found"));
    }

    protected void getMessageBody() {
//        TODO: handle incoming media message bodies differently
        String stage = this.session.get(sessionId, SessionConstants.CURRENT_STAGE, String.class);

        this.currentStageUserInput = switch (this.dto.messageType().type()) {
            case TEXT -> new ChannelUserInput(
                    ((Map) this.dto.message().get(WebhookResponseMessageType.TEXT.name().toLowerCase())).get("body").toString(),
                    null
            );
            case BUTTON -> new ChannelUserInput(
                    ((Map) this.dto.message().get(WebhookResponseMessageType.BUTTON.name().toLowerCase())).get("text").toString(),
                    null
            );
            case LOCATION -> new ChannelUserInput(
                    "location_request",
                    (Map) this.dto.message().get(WebhookResponseMessageType.LOCATION.name().toLowerCase())
            );
            case INTERACTIVE -> CommonUtils.getListInteractiveIdInput(
                    this.dto.messageType().intrType(),
                    (Map) this.dto.message().get(WebhookResponseMessageType.INTERACTIVE.name().toLowerCase())
            );
            default -> throw new EngineResponseException(CommonUtils.createEngineErrorMsg(
                    stage,
                    sessionId,
                    "unsupported response, kindly provide a valid response!"
            ));
        };
    }


    protected String dynamicRouter() {
        if(this.currentTemplate.getRouter() != null) {
            try {
                processHookParams(null);
                this.hookArg.setHook(this.currentTemplate.getRouter());
                var result = this.hookService.processHook(this.hookArg);
                if(result.getAdditionalData().containsKey(EngineConstants.REST_HOOK_DYNAMIC_ROUTE_KEY)) {
                    return (String) result.getAdditionalData().get(EngineConstants.REST_HOOK_DYNAMIC_ROUTE_KEY);
                }
            } catch (Exception err) {
                logger.error("dynamic router err: ", err);
                logger.warn("[ENGINE] failed to process dynamic router: {}", err.getMessage());
            }
        }

        return null;
    }

    private void processStageTrigger() {
//        for (EngineRoute trigger : config.getStorageManager().triggers()) {
//            if(trigger.getValue() instanceof LinkedHashMap<?, ?> triggerMap) {
//                if(CommonUtils.isRegexPatternMatch(CommonUtils.getRegexPattern(triggerMap.get("trigger").toString()), this.currentStageUserInput.input().trim())) {
//                    triggerStageProcessor(trigger);
//                    triggerParams = Map.of(EngineConstants.TPL_TRIGGER_ROUTE_PARAM_KEY, triggerMap.get("route"));
//                    return;
//                }
//            } else {
//                if(CommonUtils.isRegexInput(trigger.getValue().toString().trim())) {
//                    if(CommonUtils.isRegexPatternMatch(CommonUtils.getRegexPattern(trigger.getValue().toString().trim()), this.currentStageUserInput.input().trim())) {
//                        triggerStageProcessor(trigger);
//                        return;
//                    }
//                }
//            }
//        }

        if(this.session.get(sessionId, SessionConstants.CURRENT_MSG_ID_KEY) == null) {
//            this.session.clear();
            throw new EngineInternalException("Ambiguous old webhook response: Null msgId, skipping..");
        }
    }

    private void triggerStageProcessor(Map.Entry<String, Object> trigger) {
//        if(!templateHasKey(this.config.templateContext(), trigger.getKey()))
//            throw new EngineInternalException("route: " + trigger.getKey() + " not defined in template context map");
//
//        this.stage = trigger.getKey();
//        this.currentTemplate = (Map<String, Object>) this.config.templateContext().get(this.stage);
//        logger.info("Triggered template change: {}", this.stage);
//        this.setFromTrigger(true);
//        this.session.save(this.sessionId, SessionConstants.CURRENT_STAGE, this.stage);
    }

    private void saveProp() {
        if(this.currentTemplate.getProp() != null) {
            this.session.saveProp(
                    sessionId,
                    this.currentTemplate.getProp(),
                    this.currentStageUserInput.input()
            );
        }
    }

    /**
     * a fire-and-forget approach
     * If an error happens, ignore
     */
    private void bluetick() {
        // TODO: implement bluetick logic
    }

    private void processHookParams(Map<String, Object> template) {
        this.hookArg.setFromTrigger(this.isFromTrigger);
        if(this.currentTemplate.getParams() != null) {
            Map<String, Object> tplParams = new HashMap<>(this.currentTemplate.getParams());
            tplParams.putAll(this.triggerParams);
            this.hookArg.setParams(tplParams);
        } else if(!this.triggerParams.isEmpty()) {
            this.hookArg.setParams(this.triggerParams);
        }
    }

    private boolean isDynamicBodyTemplateLastStage(Map<String, Object> template) {
//        if true, set the current stage to the last dynamic template
//        Map<String, Object> tpl = template == null ? this.currentTemplate : template;
//        if(templateHasKey(tpl, EngineConstants.TPL_METHOD_PARAMS_KEY)) {
//            Map<String, Object> tplParams = (Map) template.get(EngineConstants.TPL_METHOD_PARAMS_KEY);
//
//            if(templateHasKey(tplParams, EngineConstants.DYNAMIC_LAST_TEMPLATE_PARAM)) {
//                return (boolean) tplParams.get(EngineConstants.DYNAMIC_LAST_TEMPLATE_PARAM);
//            }
//        }

        return false;
    }

    private void processHook(String hook) throws Exception {
        if(hook != null) {
            this.hookArg.setHook(hook);
            this.hookService.processHook(this.hookArg);
        }
    }

    private void onGenerate(AbsEngineTemplate nextTpl) throws Exception {
        this.processHook(nextTpl.getOnGenerate());
    }

    private void onReceive() throws Exception {
        processHook(EngineConstants.TPL_ON_RECEIVE_KEY);
    }

    private void middleware() throws Exception {
        processHook(EngineConstants.TPL_MIDDLEWARE_KEY);
    }

    /**
     * hooks to process after user response is received
     * from channel webhook
     */
    protected void processPostHooks() throws Exception {
        this.bluetick();
        processHookParams(null);
        this.processHook(this.currentTemplate.getOnReceive());
        this.processHook(this.currentTemplate.getMiddleware());
        this.saveProp();
    }

    /**
     * hooks to process before message response is generated
     * and send back to channel for user
     */
    protected void processPreHooks(AbsEngineTemplate nextTpl) throws Exception {
        processHookParams(nextTpl);
        this.processHook(nextTpl.getOnGenerate());
    }
}
