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
import zw.co.dcl.jawce.engine.constants.TemplateType;
import zw.co.dcl.jawce.engine.internal.dto.UserInput;
import zw.co.dcl.jawce.engine.internal.dto.Webhook;
import zw.co.dcl.jawce.engine.internal.dto.GenerateHookResult;
import zw.co.dcl.jawce.engine.internal.dto.HookResultMapper;
import zw.co.dcl.jawce.engine.internal.dynamic.DynamicChoiceRegistry;
import zw.co.dcl.jawce.engine.internal.state.ConversationState;
import zw.co.dcl.jawce.engine.internal.service.HookExecutionType;
import zw.co.dcl.jawce.engine.internal.service.RenderProcessor;
import zw.co.dcl.jawce.engine.internal.service.WhatsAppHelperService;
import zw.co.dcl.jawce.engine.internal.service.HookService;
import zw.co.dcl.jawce.engine.model.abs.BaseEngineTemplate;
import zw.co.dcl.jawce.engine.model.core.EngineRoute;
import zw.co.dcl.jawce.engine.model.core.Hook;
import zw.co.dcl.jawce.engine.model.dto.DynamicChoiceSelection;

import java.util.ArrayList;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

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
    protected ConversationState conversationState;
    protected Optional<DynamicChoiceSelection> dynamicChoiceSelection = Optional.empty();
    protected boolean hasActiveDynamicChoices = false;
    protected java.util.List<zw.co.dcl.jawce.engine.model.dto.DynamicChoice> generatedDynamicChoices = new ArrayList<>();
    protected boolean shouldRegisterDynamicChoices = false;
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
        this.isFirstTime = false;
        this.isFromTrigger = false;
        this.hasActiveDynamicChoices = false;
        this.dynamicChoiceSelection = Optional.empty();
        this.generatedDynamicChoices = new ArrayList<>();
        this.shouldRegisterDynamicChoices = false;
        this.message = message;
        this.sessionId = message.user().waId();
        this.session = this.sessionManager.session(this.sessionId);
        this.conversationState = new ConversationState(this.sessionId, this.sessionManager);

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
        this.resolveDynamicChoiceSelection(arg);
        this.hookArg = arg;
    }

    void resolveDynamicChoiceSelection(Hook arg) {
        this.hasActiveDynamicChoices = this.conversationState.hasDynamicChoicesForStage(this.stage);
        if(!this.hasActiveDynamicChoices || this.userInput.input() == null) {
            return;
        }

        var selection = DynamicChoiceRegistry.resolve(this.conversationState.dynamicChoices(), this.userInput.input())
                .map(choice -> DynamicChoiceSelection.builder()
                        .stage(this.stage)
                        .input(this.userInput.input())
                        .choice(choice)
                        .metadata(choice.getMetadata())
                        .build());

        if(selection.isPresent()) {
            Map<String, Object> merged = new HashMap<>();
            if(arg.getAdditionalData() != null) {
                merged.putAll(arg.getAdditionalData());
            }
            merged.putAll(this.conversationState.toAdditionalData(selection.get()));
            arg.setAdditionalData(merged);
        }

        this.dynamicChoiceSelection = selection;
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
            this.conversationState.saveCheckpoint(stage);
        }
    }

    void checkSessionByPass() {
        if(!this.template.isSession()) {
            this.isFromTrigger = false;
            this.conversationState.setCurrentStage(this.stage);
        }
    }

    protected boolean hasDynamicTemplateBody(String key) {
        return this.conversationState.get(key) instanceof Map;
    }

    void getCurrentTemplate() {
        this.stage = this.conversationState.currentStage();

        if(this.stage == null) {
            this.template = this.templateStorageManager
                    .getTemplate(this.config.getStartMenu())
                    .orElseThrow(() -> new InternalException(this.config.getStartMenu() + " stage not found"));

            this.conversationState.initializeAtStartMenu(this.config.getStartMenu());
            this.isFirstTime = true;
            this.stage = this.config.getStartMenu();
            return;
        }

        if(this.conversationState.hasDynamicCurrentTemplateBody()) {
            this.stage = EngineConstant.DYNAMIC_BODY_STAGE_KEY;
            this.template = SerializeUtils.toTemplate(this.conversationState.dynamicCurrentTemplateBody());
            return;
        }

        if(!this.conversationState.hasDynamicCurrentTemplateBody() &&
                this.conversationState.hasDynamicNextTemplateBody()) {
            var lastDynamicTplMessage = SerializeUtils.toTemplate(this.conversationState.dynamicNextTemplateBody());

            if(this.isDynamicBodyTemplateLastStage(lastDynamicTplMessage)) {
                this.template = lastDynamicTplMessage;
                this.conversationState.clearDynamicNextTemplateBody();
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
                Hook routerHook = this.processHook(this.template.getRouter(), HookExecutionType.ROUTER);
                this.hookArg = HookResultMapper.merge(routerHook, this.hookArg);
                return HookResultMapper.redirectTo(this.hookArg);
            } catch (Exception e) {
                log.warn("Failed to process dynamic router hook: {}", e.getMessage());
            }
        }

        return null;
    }

    void processGlobalTriggersOnInput() {
        if(this.userInput.input() != null) {
            if(handlePendingRecoveryAction()) {
                return;
            }

            for (EngineRoute trigger : this.templateStorageManager.triggers()) {
                if(hasTriggered(trigger)) return;
            }
        }

        if(this.isFirstTime) {
            return;
        }

        if(this.conversationState.currentMessageId() == null) {
//            this.session.clear();
            throw new InternalException("Ambiguous old webhook response, skipping..");
        }
    }

    boolean handlePendingRecoveryAction() {
        var recoveryAction = this.conversationState.recoveryIntentFor(this.userInput.input());
        if(recoveryAction.isEmpty()) {
            return false;
        }

        this.conversationState.clearRecoveryActions();

        switch (recoveryAction.get()) {
            case RESTART, RETURN_TO_MENU -> {
                this.template = this.templateStorageManager
                        .getTemplate(this.config.getStartMenu())
                        .orElseThrow(() -> new InternalException(this.config.getStartMenu() + " stage not found"));
                this.stage = this.config.getStartMenu();
                this.isFromTrigger = true;
                this.conversationState.initializeAtStartMenu(this.stage);
                return true;
            }
            default -> {
                return false;
            }
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
        this.conversationState.setCurrentStage(this.stage);

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

    protected Hook processHook(String hook, HookExecutionType hookExecutionType) throws Exception {
        if(hook != null) {
            this.hookArg.setHook(hook);
            return this.hookService.processHook(this.hookArg, hookExecutionType);
        }

        return this.hookArg;
    }

    /**
     * hooks to process after user response is received
     * from channel webhook
     */
    protected void processPostHooks() throws Exception {
        this.ack_message();
        processHookParams(null);
        this.hookArg = HookResultMapper.merge(
                this.processHook(this.template.getOnReceive(), HookExecutionType.RECEIVE),
                this.hookArg
        );
        this.hookArg = HookResultMapper.merge(
                this.processHook(this.template.getMiddleware(), HookExecutionType.MIDDLEWARE),
                this.hookArg
        );
        this.saveProp();
    }

    /**
     * hooks to process before message response is generated
     * and send back to channel for user
     */
    protected BaseEngineTemplate processPreHooks(BaseEngineTemplate nextTemplate) throws Exception {
        processHookParams(nextTemplate);
        Hook hookResult = this.processHook(nextTemplate.getOnGenerate(), HookExecutionType.GENERATE);
        this.hookArg = HookResultMapper.merge(hookResult, this.hookArg);
        GenerateHookResult generateHookResult = HookResultMapper.toGenerateResult(this.hookArg);

        if(generateHookResult.hasTemplateOverride()) {
            applyDynamicChoiceState(generateHookResult, true);
            return generateHookResult.templateOverride();
        }

        if(generateHookResult.hasRenderPayload()) {
            var renderer = new RenderProcessor();
            var renderResult = renderer.renderTemplate(
                    SerializeUtils.fromTemplate(nextTemplate),
                    generateHookResult.renderPayload()
            );
            applyDynamicChoiceState(generateHookResult, false);
            return SerializeUtils.toTemplate(renderResult);
        }

        applyDynamicChoiceState(generateHookResult, false);
        return nextTemplate;
    }

    protected BaseEngineTemplate materializeDynamicTemplate(BaseEngineTemplate nextTemplate) throws Exception {
        if(nextTemplate.getDynamic() != null && !nextTemplate.getDynamic().isBlank()) {
            processHookParams(nextTemplate);
            Hook dynamicHookResult = this.processHook(nextTemplate.getDynamic(), HookExecutionType.DYNAMIC);
            this.hookArg = HookResultMapper.merge(dynamicHookResult, this.hookArg);
            GenerateHookResult generateHookResult = HookResultMapper.toGenerateResult(this.hookArg);

            if(!generateHookResult.hasTemplateOverride()) {
                throw new InternalException("dynamic hook for stage " + this.stage + " did not return a template body");
            }

            applyDynamicChoiceState(generateHookResult, true);
            return generateHookResult.templateOverride();
        }

        if(!TemplateType.DYNAMIC.equals(nextTemplate.getType())) {
            return nextTemplate;
        }

        if(nextTemplate.getTemplate() == null || nextTemplate.getTemplate().isBlank()) {
            return nextTemplate;
        }

        processHookParams(nextTemplate);
        Hook templateHookResult = this.processHook(nextTemplate.getTemplate(), HookExecutionType.TEMPLATE);
        this.hookArg = HookResultMapper.merge(templateHookResult, this.hookArg);
        GenerateHookResult generateHookResult = HookResultMapper.toGenerateResult(this.hookArg);

        if(!generateHookResult.hasTemplateOverride()) {
            throw new InternalException("dynamic stage " + this.stage + " did not return a template body");
        }

        applyDynamicChoiceState(generateHookResult, true);
        return generateHookResult.templateOverride();
    }

    private void applyDynamicChoiceState(GenerateHookResult generateHookResult, boolean templateOverride) {
        this.generatedDynamicChoices = generateHookResult.hasDynamicChoices()
                ? generateHookResult.dynamicChoices()
                : new ArrayList<>();
        this.shouldRegisterDynamicChoices = generateHookResult.hasDynamicChoices() || templateOverride;
    }
}
