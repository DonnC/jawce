package zw.co.dcl.engine.whatsapp.processor.abstracts;

import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import zw.co.dcl.engine.whatsapp.constants.EngineConstants;
import zw.co.dcl.engine.whatsapp.constants.SessionConstants;
import zw.co.dcl.engine.whatsapp.entity.dto.*;
import zw.co.dcl.engine.whatsapp.exceptions.EngineInternalException;
import zw.co.dcl.engine.whatsapp.exceptions.EngineResponseException;
import zw.co.dcl.engine.whatsapp.service.EngineRequestService;
import zw.co.dcl.engine.whatsapp.utils.CommonUtils;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class ChannelMessageProcessor {
    protected final MsgProcessorDTO dto;
    private final Logger logger = LoggerFactory.getLogger(ChannelMessageProcessor.class);
    protected Map<String, Object> currentStageTpl;
    protected ChannelUserInput currentStageUserInput = null;
    protected boolean isFirstTime = false;
    @Getter
    @Setter
    protected boolean isFromTrigger = false;
    protected HookArgs hookArgs = null;
    protected EngineRequestService engineService;
    private String stage;
    private Map<String, Object> triggerParams;

    protected ChannelMessageProcessor(MsgProcessorDTO dto, EngineRequestService engineService) {
        this.triggerParams = new HashMap<>();
        this.engineService = engineService;
        this.dto = dto;
        this.getCurrentStageTemplate();
        this.getMessageBody();
        this.processStageTrigger();
        this.saveCheckpoint();

        HookArgs args = new HookArgs();
        args.setSession(dto.ISessionManager());
        args.setChannelUser(dto.waCurrentUser());
        args.setUserInput(this.currentStageUserInput.input());
        args.setAdditionalData(this.currentStageUserInput.additionalData());

        this.hookArgs = args;
    }

    private void saveCheckpoint() {
        if (this.currentStageTpl.containsKey(EngineConstants.TPL_CHECKPOINT_KEY)) {
            if (this.dto.tplContextMap().containsKey(stage))
                this.dto.ISessionManager().save(SessionConstants.SESSION_LATEST_CHECKPOINT_KEY, stage);
        }
    }

    protected boolean hasEngineFullDynamicTemplateBody(String key) {
        if (this.dto.ISessionManager().get(key) != null)
            return this.dto.ISessionManager().get(key) instanceof Map dynamicTpl;
        return false;
    }

    private void getCurrentStageTemplate() {
        String currentStage = (String) this.dto.ISessionManager().get(SessionConstants.CURRENT_STAGE);
        logger.info("current stage found: {}", currentStage);

        if (currentStage == null) {
//            assume user is new or session has been cleared
            if (!templateHasKey(this.dto.tplContextMap(), dto.sessionSettings().getStartMenuStageKey()))
                throw new EngineInternalException("start menu with stage: " + dto.sessionSettings().getStartMenuStageKey() + ", not defined in template context map");

            this.currentStageTpl = (Map<String, Object>) this.dto.tplContextMap().get(dto.sessionSettings().getStartMenuStageKey());
            this.dto.ISessionManager().save(SessionConstants.CURRENT_STAGE, dto.sessionSettings().getStartMenuStageKey());
            this.dto.ISessionManager().save(SessionConstants.PREV_STAGE, dto.sessionSettings().getStartMenuStageKey());
            this.isFirstTime = true;
            this.stage = dto.sessionSettings().getStartMenuStageKey();
            return;
        }

        if (hasEngineFullDynamicTemplateBody(SessionConstants.DYNAMIC_CURRENT_TEMPLATE_BODY_KEY)) {
            logger.info("Full CURRENT dynamic template body found. Setting as current tpl..");
            this.stage = EngineConstants.DYNAMIC_BODY_STAGE_KEY;
            this.currentStageTpl = this.dto.ISessionManager().get(
                    SessionConstants.DYNAMIC_CURRENT_TEMPLATE_BODY_KEY,
                    Map.class
            );
            return;
        }

        if (!hasEngineFullDynamicTemplateBody(SessionConstants.DYNAMIC_CURRENT_TEMPLATE_BODY_KEY) &&
                hasEngineFullDynamicTemplateBody(SessionConstants.DYNAMIC_NEXT_TEMPLATE_BODY_KEY)
        ) {
            Map<String, Object> lastDynamicTplMessage = this.dto.ISessionManager().get(
                    SessionConstants.DYNAMIC_NEXT_TEMPLATE_BODY_KEY,
                    Map.class
            );

            if (isDynamicBodyTemplateLastStage(lastDynamicTplMessage)) {
                logger.info("LAST dynamic template body found. Setting as current tpl..");
                this.stage = currentStage;
                this.currentStageTpl = lastDynamicTplMessage;
                this.dto.ISessionManager().evict(SessionConstants.DYNAMIC_NEXT_TEMPLATE_BODY_KEY);
                logger.info("EVICTED next dynamic tpl key");
                return;
            }
        }

        if (!templateHasKey(this.dto.tplContextMap(), currentStage))
            throw new EngineInternalException("route: " + currentStage + " not defined in template context map");

        this.stage = currentStage;
        this.currentStageTpl = (Map<String, Object>) this.dto.tplContextMap().get(currentStage);
    }

    protected void getMessageBody() {
//        TODO: handle incoming media message bodies differently
        String stage = this.dto.ISessionManager().get(SessionConstants.CURRENT_STAGE, String.class);

        this.currentStageUserInput = switch (this.dto.messageType().type()) {
            case TEXT -> new ChannelUserInput(((Map) this.dto.message().get("text")).get("body").toString(), null);
            case BUTTON -> new ChannelUserInput(((Map) this.dto.message().get("button")).get("text").toString(), null);
            case INTERACTIVE ->
                    CommonUtils.getListInteractiveIdInput(this.dto.messageType().intrType(), (Map) this.dto.message().get("interactive"));
            default -> throw new EngineResponseException(CommonUtils.createEngineErrorMsg(
                    stage,
                    this.dto.waCurrentUser().waId(),
                    "unsupported response, kindly provide a valid response!"
            ));
        };

        logger.info("[{}] Extracted user input: {}", stage, this.currentStageUserInput.input());
    }

    protected boolean templateHasKey(Map<String, Object> tpl, String key) {
        return tpl.containsKey(key);
    }

    protected String dynamicRouter() {
        if (templateHasKey(this.currentStageTpl, EngineConstants.TPL_DYNAMIC_ROUTER_KEY)) {
            try {
                processHookParams(null);
                var result = this.engineService.processHook(this.currentStageTpl.get(EngineConstants.TPL_DYNAMIC_ROUTER_KEY).toString(), this.hookArgs);
                if (result.getAdditionalData().containsKey(EngineConstants.REST_HOOK_DYNAMIC_ROUTE_KEY)) {
                    return (String) result.getAdditionalData().get(EngineConstants.REST_HOOK_DYNAMIC_ROUTE_KEY);
                }
            } catch (Exception err) {
                logger.warn("[ENGINE] failed to process dynamic router: {}", err.getMessage());
            }
        }

        return null;
    }

    private void processStageTrigger() {
        for (Map.Entry<String, Object> trigger : dto.triggersContextMap().entrySet()) {
            if (trigger.getValue() instanceof LinkedHashMap<?, ?> triggerMap) {
                if (CommonUtils.isRegexPatternMatch(CommonUtils.getRegexPattern(triggerMap.get("trigger").toString()), this.currentStageUserInput.input().trim())) {
                    if (!templateHasKey(this.dto.tplContextMap(), trigger.getKey()))
                        throw new EngineInternalException("route: " + trigger.getKey() + " not defined in template context map");

                    this.currentStageTpl = (Map<String, Object>) this.dto.tplContextMap().get(trigger.getKey());
                    logger.info("triggered current stage tpl changed to: {} | params: {}", trigger.getKey(), triggerMap);
                    this.setFromTrigger(true);
                    this.stage = trigger.getKey();
                    triggerParams = Map.of(EngineConstants.TPL_TRIGGER_ROUTE_PARAM_KEY, triggerMap.get("route"));
                    return;
                }
            } else {
                if (CommonUtils.isRegexInput(trigger.getValue().toString().trim())) {
                    if (CommonUtils.isRegexPatternMatch(CommonUtils.getRegexPattern(trigger.getValue().toString().trim()), this.currentStageUserInput.input().trim())) {
                        if (!templateHasKey(this.dto.tplContextMap(), trigger.getKey()))
                            throw new EngineInternalException("route: " + trigger.getKey() + " not defined in template context map");
                        this.currentStageTpl = (Map<String, Object>) this.dto.tplContextMap().get(trigger.getKey());
                        logger.info("triggered current stage tpl changed to: {}", trigger.getKey());
                        this.setFromTrigger(true);
                        this.stage = trigger.getKey();
                        return;
                    }
                }
            }
        }

//        if (dto.ISessionManager().get(SessionConstants.CURRENT_MSG_ID_KEY) == null) {
//            dto.ISessionManager().clear();
//            throw new EngineInternalException("Ambiguous old webhook response: skipping..");
//        }
    }

    private void saveProp() {
        if (templateHasKey(currentStageTpl, EngineConstants.TPL_PROP_KEY)) {
            this.dto.ISessionManager().saveProp(
                    this.currentStageTpl.get(EngineConstants.TPL_PROP_KEY).toString().trim(),
                    this.currentStageUserInput.input()
            );
        }
    }


    private void processHookParams(Map<String, Object> template) {
        Map<String, Object> tpl = template == null ? this.currentStageTpl : template;
        if (templateHasKey(tpl, EngineConstants.TPL_METHOD_PARAMS_KEY)) {
            Map<String, Object> tplParams = new HashMap<>((Map) tpl.get(EngineConstants.TPL_METHOD_PARAMS_KEY));
            tplParams.putAll(this.triggerParams);
            this.hookArgs.setMethodArgs(tplParams);
        } else if (!this.triggerParams.isEmpty()) {
            this.hookArgs.setMethodArgs(this.triggerParams);
        }
    }

    private boolean isDynamicBodyTemplateLastStage(Map<String, Object> template) {
//        if true, set the current stage to the last dynamic template
        Map<String, Object> tpl = template == null ? this.currentStageTpl : template;
        if (templateHasKey(tpl, EngineConstants.TPL_METHOD_PARAMS_KEY)) {
            Map<String, Object> tplParams = (Map) template.get(EngineConstants.TPL_METHOD_PARAMS_KEY);

            if (templateHasKey(tplParams, EngineConstants.DYNAMIC_LAST_TEMPLATE_PARAM)) {
                return (boolean) tplParams.get(EngineConstants.DYNAMIC_LAST_TEMPLATE_PARAM);
            }
        }

        return false;
    }

    private void processHook(String key) throws Exception {
        if (templateHasKey(this.currentStageTpl, key)) {
            this.engineService.processHook(this.currentStageTpl.get(key).toString(), this.hookArgs);
        }
    }

    private void onGenerate(Map<String, Object> nextTpl) throws Exception {
        if (templateHasKey(nextTpl, EngineConstants.TPL_ON_GENERATE_KEY)) {
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
