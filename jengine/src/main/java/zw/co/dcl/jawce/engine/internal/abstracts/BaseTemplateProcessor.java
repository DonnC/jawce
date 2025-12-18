package zw.co.dcl.jawce.engine.internal.abstracts;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import zw.co.dcl.jawce.engine.api.exceptions.InternalException;
import zw.co.dcl.jawce.engine.api.iface.ISessionManager;
import zw.co.dcl.jawce.engine.api.iface.ITemplateStorageManager;
import zw.co.dcl.jawce.engine.api.utils.SerializeUtils;
import zw.co.dcl.jawce.engine.api.utils.Utils;
import zw.co.dcl.jawce.engine.api.utils.WhatsAppUtils;
import zw.co.dcl.jawce.engine.configs.JawceConfig;
import zw.co.dcl.jawce.engine.constants.EngineConstant;
import zw.co.dcl.jawce.engine.constants.SessionConstant;
import zw.co.dcl.jawce.engine.internal.dto.UserInput;
import zw.co.dcl.jawce.engine.internal.dto.Webhook;
import zw.co.dcl.jawce.engine.internal.service.WhatsAppHelperService;
import zw.co.dcl.jawce.engine.internal.service.HookService;
import zw.co.dcl.jawce.engine.model.abs.BaseEngineTemplate;
import zw.co.dcl.jawce.engine.model.core.EngineRoute;
import zw.co.dcl.jawce.engine.model.core.Hook;

import java.util.HashMap;
import java.util.Map;

@Getter
@Slf4j
public abstract class BaseTemplateProcessor {
    protected final HookService hookService;
    protected final ISessionManager sessionManager;
    protected final ITemplateStorageManager templateStorageManager;
    protected final JawceConfig config;
    protected final WhatsAppHelperService helperService;

    protected Webhook message;
    protected BaseEngineTemplate template;
    protected UserInput userInput = null;
    protected boolean isFirstTime = false;
    protected boolean isFromTrigger = false;
    protected Hook hookArg = null;
    protected String stage;
    protected String sessionId;
    protected ISessionManager session;
    Map<String, Object> params;

    public BaseTemplateProcessor(
            HookService hookService, ISessionManager sessionManager,
            ITemplateStorageManager templateStorageManager, JawceConfig config,
            WhatsAppHelperService helperService
    ) {
        this.hookService = hookService;
        this.sessionManager = sessionManager;
        this.templateStorageManager = templateStorageManager;
        this.config = config;
        this.helperService = helperService;
    }

    protected void setup(Webhook message) {
        this.params = new HashMap<>();
        this.message = message;
        this.sessionId = message.user().waId();
        this.session = this.sessionManager.session(this.sessionId);

        // initialize
        this.getCurrentTemplate();
        this.userInput = WhatsAppUtils.getUserInput(message.response());
        this.processGlobalTriggersOnInput();
        this.checkSessionByPass();
        this.saveCheckpoint();

        // show indicators
        this.showMessageIndicators();

        // init Hook
        var arg = new Hook();
        arg.setSessionId(this.sessionId);
        arg.setSession(this.session);
        arg.setWaUser(message.user());
        arg.setUserInput(this.userInput.input());
        arg.setAdditionalData(this.userInput.data());
        this.hookArg = arg;
    }

    void showMessageIndicators() {
        // show typing or reaction
        try {
            if(this.template.getReaction() != null) {
                this.helperService.sendReaction(this.message.user().waId(), this.template.getReaction(), this.message.user().msgId());
            }

            if(this.template.isTyping()) {
                this.helperService.showTypingIndicator(this.message.user().msgId());
            }
        } catch (Exception e) {
            log.warn("Failed to show message indicators: {}", e.getMessage());
        }
    }

    void saveCheckpoint() {
        if(this.template.isCheckpoint()) {
            this.session.save(this.sessionId, SessionConstant.SESSION_CHECKPOINT_KEY, stage);
        }
    }

    void checkSessionByPass() {
        if(!this.template.isSession()) {
            this.isFromTrigger = false;
            this.session.save(this.sessionId, SessionConstant.CURRENT_STAGE, this.stage);
        }
    }

    protected boolean hasDynamicTemplateBody(String key) {
        return this.session.get(sessionId, key) instanceof Map;
    }

    void getCurrentTemplate() {
        this.stage = this.session.get(this.sessionId, SessionConstant.CURRENT_STAGE, String.class);

        if(this.stage == null) {
            this.template = this.templateStorageManager
                    .getTemplate(this.config.getStartMenu())
                    .orElseThrow(() -> new InternalException(this.config.getStartMenu() + " stage not found"));

            this.session.saveAll(
                    this.sessionId,
                    Map.of(
                            SessionConstant.CURRENT_STAGE, this.config.getStartMenu(),
                            SessionConstant.PREV_STAGE, this.config.getStartMenu()
                    )
            );
            this.isFirstTime = true;
            this.stage = this.config.getStartMenu();
            return;
        }

        if(this.hasDynamicTemplateBody(SessionConstant.DYNAMIC_CURRENT_TEMPLATE_BODY_KEY)) {
            this.stage = EngineConstant.DYNAMIC_BODY_STAGE_KEY;
            this.template = SerializeUtils.toTemplate(session.get(sessionId,
                    SessionConstant.DYNAMIC_CURRENT_TEMPLATE_BODY_KEY,
                    Map.class
            ));
            return;
        }

        if(!hasDynamicTemplateBody(SessionConstant.DYNAMIC_CURRENT_TEMPLATE_BODY_KEY) &&
                hasDynamicTemplateBody(SessionConstant.DYNAMIC_NEXT_TEMPLATE_BODY_KEY)
        ) {
            var lastDynamicTplMessage = SerializeUtils.toTemplate(this.session.get(
                    sessionId,
                    SessionConstant.DYNAMIC_NEXT_TEMPLATE_BODY_KEY,
                    Map.class
            ));

            if(this.isDynamicBodyTemplateLastStage(lastDynamicTplMessage)) {
                this.template = lastDynamicTplMessage;
                this.session.evict(sessionId, SessionConstant.DYNAMIC_NEXT_TEMPLATE_BODY_KEY);
                return;
            }
        }

        this.template = this.templateStorageManager
                .getTemplate(this.stage)
                .orElseThrow(() -> new InternalException(this.stage + " stage not found"));
    }

    protected String getDynamicRouterRoute() {
        if(this.template.getRouter() != null) {
            try {
                processHookParams(null);
                this.hookArg.setHook(this.template.getRouter());
                return this.hookService.processHook(this.hookArg).getRedirectTo();
            } catch (Exception e) {
                log.warn("Failed to process dynamic router hook: {}", e.getMessage());
            }
        }

        return null;
    }

    void processGlobalTriggersOnInput() {
        if(this.userInput.input() != null) {
            for (EngineRoute trigger : this.templateStorageManager.triggers()) {
                if(hasTriggered(trigger)) return;
            }
        }

        if(this.session.get(sessionId, SessionConstant.CURRENT_MSG_ID_KEY) == null) {
//            this.session.clear();
            throw new InternalException("Ambiguous old webhook response, skipping..");
        }
    }

    boolean hasTriggered(EngineRoute trigger) {
        boolean shouldTrigger = false;

        if(trigger.isRegex()) {
            shouldTrigger = Utils.isRegexPatternMatch(trigger.getUserInput(), this.userInput.input());
        } else {
            if(this.userInput.input() != null) {
                shouldTrigger = this.userInput.input().equalsIgnoreCase(trigger.getUserInput());
            }
        }

        if(!shouldTrigger) return false;

        this.template = this.templateStorageManager
                .getTemplate(trigger.getNextStage())
                .orElseThrow(() -> new InternalException(trigger.getNextStage() + " stage not found"));
        this.stage = trigger.getNextStage();
        log.info("Triggered template change: {}", this.stage);
        this.isFromTrigger = true;
        this.session.save(this.sessionId, SessionConstant.CURRENT_STAGE, this.stage);

        if(trigger.getInnerNextStage() != null) {
            this.hookArg.setRedirectTo(trigger.getInnerNextStage());
        }

        return true;
    }

    void saveProp() {
        if(this.template.getProp() != null) {
            this.session.saveProp(
                    this.sessionId,
                    this.template.getProp(),
                    this.userInput.input()
            );
        }
    }

    /**
     * a fire-and-forget approach
     * If an error happens, ignore
     */
    void ack_message() {
        try {
            boolean canMarkAsRead = this.config.isReadReceipts() || this.template.isAcknowledged();

            if(canMarkAsRead) {
                this.helperService.markAsRead(this.message.user().msgId());
            }
        } catch (Exception e) {
            log.warn("Failed to acknowledge template message: {}", e.getMessage());
        }
    }

    void processHookParams(BaseEngineTemplate newTemplate) {
        var innerTemplate = newTemplate == null ? this.template : newTemplate;

        this.hookArg.setFromTrigger(this.isFromTrigger);
        if(!innerTemplate.getParams().isEmpty()) {
            var templateParams = new HashMap<>(innerTemplate.getParams());
            templateParams.putAll(this.params);
            this.hookArg.setParams(templateParams);
        } else if(!this.params.isEmpty()) {
            this.hookArg.setParams(this.params);
        }
    }

    boolean isDynamicBodyTemplateLastStage(BaseEngineTemplate dynamicTemplate) {
        // if true, set the current stage to the last dynamic template
        var tpl = dynamicTemplate == null ? this.template : dynamicTemplate;
        return (boolean) tpl.getParams().getOrDefault(EngineConstant.DYNAMIC_LAST_TEMPLATE_PARAM, false);
    }

    void processHook(String hook) throws Exception {
        if(hook != null) {
            this.hookArg.setHook(hook);
            this.hookService.processHook(this.hookArg);
        }
    }

    /**
     * hooks to process after user response is received
     * from channel webhook
     */
    protected void processPostHooks() throws Exception {
        this.ack_message();
        processHookParams(null);
        this.processHook(this.template.getOnReceive());
        this.processHook(this.template.getMiddleware());
        this.saveProp();
    }

    /**
     * hooks to process before message response is generated
     * and send back to channel for user
     */
    protected void processPreHooks(BaseEngineTemplate nextTemplate) throws Exception {
        processHookParams(nextTemplate);
        this.processHook(nextTemplate.getOnGenerate());
    }
}
