package zw.co.dcl.jawce.engine.processor.abstracts;

import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import zw.co.dcl.jawce.engine.constants.EngineConstants;
import zw.co.dcl.jawce.engine.constants.SessionConstants;
import zw.co.dcl.jawce.engine.enums.WebhookResponseMessageType;
import zw.co.dcl.jawce.engine.exceptions.EngineInternalException;
import zw.co.dcl.jawce.engine.exceptions.EngineResponseException;
import zw.co.dcl.jawce.engine.model.dto.*;
import zw.co.dcl.jawce.engine.service.RequestService;
import zw.co.dcl.jawce.engine.utils.CommonUtils;
import zw.co.dcl.jawce.session.ISessionManager;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class ChannelMessageProcessor {
    protected final MsgProcessorDTO dto;
    protected final ISessionManager session;
    protected final String sessionId;
    protected final WaEngineConfig config;
    private final Logger logger = LoggerFactory.getLogger(ChannelMessageProcessor.class);
    protected Map<String, Object> currentStageTpl;
    protected ChannelUserInput currentStageUserInput = null;
    protected boolean isFirstTime = false;
    @Getter
    @Setter
    protected boolean isFromTrigger = false;
    protected HookArgs hookArgs = null;
    protected RequestService engineService;
    private String stage;
    private Map<String, Object> triggerParams;

    protected ChannelMessageProcessor(MsgProcessorDTO dto, WaEngineConfig config) {
        this.triggerParams = new HashMap<>();
        this.config = config;
        this.engineService = RequestService.getInstance(config);
        this.dto = dto;
        this.sessionId = dto.waCurrentUser().waId();
        this.session = config.sessionManager().session(sessionId);
        this.getCurrentStageTemplate();
        this.getMessageBody();
        this.processStageTrigger();
        this.saveCheckpoint();

        HookArgs args = new HookArgs();
        args.setSession(this.session);
        args.setChannelUser(dto.waCurrentUser());
        args.setUserInput(this.currentStageUserInput.input());
        args.setAdditionalData(this.currentStageUserInput.additionalData());
        this.hookArgs = args;
    }

    private void saveCheckpoint() {
        if(this.currentStageTpl.containsKey(EngineConstants.TPL_CHECKPOINT_KEY)) {
            if(this.config.templateContext().containsKey(stage))
                this.session.save(sessionId, SessionConstants.SESSION_LATEST_CHECKPOINT_KEY, stage);
        }
    }

    protected boolean hasEngineFullDynamicTemplateBody(String key) {
        if(this.session.get(sessionId, key) != null)
            return this.session.get(sessionId, key) instanceof Map dynamicTpl;
        return false;
    }

    private void getCurrentStageTemplate() {
        String currentStage = (String) this.session.get(sessionId, SessionConstants.CURRENT_STAGE);

        if(currentStage == null) {
//            assume user is new or session has been cleared
            if(!templateHasKey(this.config.templateContext(), config.sessionSettings().getStartMenuStageKey()))
                throw new EngineInternalException("start menu with stage: " + config.sessionSettings().getStartMenuStageKey() + ", not defined in template context map");

            this.currentStageTpl = (Map<String, Object>) this.config.templateContext().get(config.sessionSettings().getStartMenuStageKey());
            this.session.save(sessionId, SessionConstants.CURRENT_STAGE, config.sessionSettings().getStartMenuStageKey());
            this.session.save(sessionId, SessionConstants.PREV_STAGE, config.sessionSettings().getStartMenuStageKey());
            this.isFirstTime = true;
            this.stage = config.sessionSettings().getStartMenuStageKey();
            return;
        }

        if(hasEngineFullDynamicTemplateBody(SessionConstants.DYNAMIC_CURRENT_TEMPLATE_BODY_KEY)) {
            this.stage = EngineConstants.DYNAMIC_BODY_STAGE_KEY;
            this.currentStageTpl = this.session.get(
                    sessionId,
                    SessionConstants.DYNAMIC_CURRENT_TEMPLATE_BODY_KEY,
                    Map.class
            );
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

            if(isDynamicBodyTemplateLastStage(lastDynamicTplMessage)) {
                this.stage = currentStage;
                this.currentStageTpl = lastDynamicTplMessage;
                this.session.evict(sessionId, SessionConstants.DYNAMIC_NEXT_TEMPLATE_BODY_KEY);
                return;
            }
        }

        if(!templateHasKey(this.config.templateContext(), currentStage))
            throw new EngineInternalException("route: " + currentStage + " not defined in template context map");

        this.stage = currentStage;
        this.currentStageTpl = (Map<String, Object>) this.config.templateContext().get(currentStage);
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

    protected boolean templateHasKey(Map<String, Object> tpl, String key) {
        return tpl.containsKey(key);
    }

    protected String dynamicRouter() {
        if(templateHasKey(this.currentStageTpl, EngineConstants.TPL_DYNAMIC_ROUTER_KEY)) {
            try {
                processHookParams(null);
                var result = this.engineService.processHook(this.currentStageTpl.get(EngineConstants.TPL_DYNAMIC_ROUTER_KEY).toString(), this.hookArgs);
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
        for (Map.Entry<String, Object> trigger : config.templateContext().entrySet()) {
            if(trigger.getValue() instanceof LinkedHashMap<?, ?> triggerMap) {
                if(CommonUtils.isRegexPatternMatch(CommonUtils.getRegexPattern(triggerMap.get("trigger").toString()), this.currentStageUserInput.input().trim())) {
                    triggerStageProcessor(trigger);
                    triggerParams = Map.of(EngineConstants.TPL_TRIGGER_ROUTE_PARAM_KEY, triggerMap.get("route"));
                    return;
                }
            } else {
                if(CommonUtils.isRegexInput(trigger.getValue().toString().trim())) {
                    if(CommonUtils.isRegexPatternMatch(CommonUtils.getRegexPattern(trigger.getValue().toString().trim()), this.currentStageUserInput.input().trim())) {
                        triggerStageProcessor(trigger);
                        return;
                    }
                }
            }
        }

        if(this.session.get(sessionId, SessionConstants.CURRENT_MSG_ID_KEY) == null) {
//            this.session.clear();
            throw new EngineInternalException("Ambiguous old webhook response: Null msgId, skipping..");
        }
    }

    private void triggerStageProcessor(Map.Entry<String, Object> trigger) {
        if(!templateHasKey(this.config.templateContext(), trigger.getKey()))
            throw new EngineInternalException("route: " + trigger.getKey() + " not defined in template context map");

        this.currentStageTpl = (Map<String, Object>) this.config.templateContext().get(trigger.getKey());
        logger.info("[{}] Triggered template change: {}", sessionId, trigger.getKey());
        this.setFromTrigger(true);
        this.stage = trigger.getKey();
    }

    private void saveProp() {
        if(templateHasKey(currentStageTpl, EngineConstants.TPL_PROP_KEY)) {
            this.session.saveProp(
                    sessionId,
                    this.currentStageTpl.get(EngineConstants.TPL_PROP_KEY).toString().trim(),
                    this.currentStageUserInput.input()
            );
        }
    }

    /**
     * a fire-and-forget approach
     * If an error happens, ignore
     */
    private void bluetick() {
        if(templateHasKey(currentStageTpl, EngineConstants.TPL_READ_RECEIPT_KEY)) {
            try {
                Map<String, Object> payload = Map.of(
                        "product_id", "whatsapp",
                        "status", "read",
                        "message_id", dto.waCurrentUser().msgId()
                );

                this.engineService.sendWhatsappRequest(
                        new ChannelRequestDto(
                                null,
                                new MsgProcessorResponseDTO(
                                        payload,
                                        null,
                                        sessionId
                                )
                        ),
                        false,
                        null
                );
            } catch (Exception e) {
                logger.warn("Failed to bluetick ✔ message: {}", e.getMessage());
            }
        }
    }

    private void processHookParams(Map<String, Object> template) {
        Map<String, Object> tpl = template == null ? this.currentStageTpl : template;
        this.hookArgs.setFromTrigger(this.isFromTrigger);
        if(templateHasKey(tpl, EngineConstants.TPL_METHOD_PARAMS_KEY)) {
            Map<String, Object> tplParams = new HashMap<>((Map) tpl.get(EngineConstants.TPL_METHOD_PARAMS_KEY));
            tplParams.putAll(this.triggerParams);
            this.hookArgs.setMethodArgs(tplParams);
        } else if(!this.triggerParams.isEmpty()) {
            this.hookArgs.setMethodArgs(this.triggerParams);
        }
    }

    private boolean isDynamicBodyTemplateLastStage(Map<String, Object> template) {
//        if true, set the current stage to the last dynamic template
        Map<String, Object> tpl = template == null ? this.currentStageTpl : template;
        if(templateHasKey(tpl, EngineConstants.TPL_METHOD_PARAMS_KEY)) {
            Map<String, Object> tplParams = (Map) template.get(EngineConstants.TPL_METHOD_PARAMS_KEY);

            if(templateHasKey(tplParams, EngineConstants.DYNAMIC_LAST_TEMPLATE_PARAM)) {
                return (boolean) tplParams.get(EngineConstants.DYNAMIC_LAST_TEMPLATE_PARAM);
            }
        }

        return false;
    }

    private void processHook(String key) throws Exception {
        if(templateHasKey(this.currentStageTpl, key)) {
            this.engineService.processHook(this.currentStageTpl.get(key).toString(), this.hookArgs);
        }
    }

    private void onGenerate(Map<String, Object> nextTpl) throws Exception {
        if(templateHasKey(nextTpl, EngineConstants.TPL_ON_GENERATE_KEY)) {
            this.engineService.processHook(nextTpl.get(EngineConstants.TPL_ON_GENERATE_KEY).toString(), this.hookArgs);
        }
    }

    private void onReceive() throws Exception {
        processHook(EngineConstants.TPL_ON_RECEIVE_KEY);
    }

    private void middleware() throws Exception {
        processHook(EngineConstants.TPL_MIDDLEWARE_KEY);
    }

    private void validator() throws Exception {
        processHook(EngineConstants.TPL_VALIDATOR_KEY);
    }

    /**
     * hooks to process after user response is received
     * from channel webhook
     */
    protected void processPostHooks() throws Exception {
        processHookParams(null);
        this.validator();
        this.onReceive();
        this.middleware();
        this.saveProp();
        this.bluetick();
    }

    /**
     * hooks to process before message response is generated
     * and send back to channel for user
     */
    protected void processPreHooks(Map<String, Object> nextTpl) throws Exception {
        processHookParams(nextTpl);
        this.onGenerate(nextTpl);
    }
}
