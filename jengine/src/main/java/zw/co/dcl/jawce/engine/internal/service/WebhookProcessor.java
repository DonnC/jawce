package zw.co.dcl.jawce.engine.internal.service;

import lombok.extern.slf4j.Slf4j;
import zw.co.dcl.jawce.engine.api.dto.PayloadGeneratorDto;
import zw.co.dcl.jawce.engine.api.exceptions.InternalException;
import zw.co.dcl.jawce.engine.api.exceptions.ResponseException;
import zw.co.dcl.jawce.engine.api.exceptions.SessionExpiredException;
import zw.co.dcl.jawce.engine.api.exceptions.SessionInactivityException;
import zw.co.dcl.jawce.engine.api.iface.ISessionManager;
import zw.co.dcl.jawce.engine.api.iface.ITemplateStorageManager;
import zw.co.dcl.jawce.engine.api.utils.PayloadGenerator;
import zw.co.dcl.jawce.engine.api.utils.SerializeUtils;
import zw.co.dcl.jawce.engine.api.utils.Utils;
import zw.co.dcl.jawce.engine.configs.JawceConfig;
import zw.co.dcl.jawce.engine.constants.EngineConstant;
import zw.co.dcl.jawce.engine.constants.SessionConstant;
import zw.co.dcl.jawce.engine.internal.abstracts.BaseTemplateProcessor;
import zw.co.dcl.jawce.engine.internal.dto.PreProcessorResult;
import zw.co.dcl.jawce.engine.internal.dto.ResponseError;
import zw.co.dcl.jawce.engine.internal.dto.Webhook;
import zw.co.dcl.jawce.engine.model.abs.BaseEngineTemplate;
import zw.co.dcl.jawce.engine.model.core.EngineRoute;
import zw.co.dcl.jawce.engine.model.dto.WebhookProcessorResult;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class WebhookProcessor extends BaseTemplateProcessor {
    final int MAX_NESTED_CALLS = 5;
    int nestedCallCount = 0;
    List<PreProcessorResult> nestedPreProcessorResults = new ArrayList<>();

    public WebhookProcessor(HookService hookService, ISessionManager sessionManager, ITemplateStorageManager templateStorageManager, JawceConfig config) {
        super(hookService, sessionManager, templateStorageManager, config);
    }

    /**
     * Check if template have shortcut variables, if available
     * <p>
     * process these at last and update the template
     * <p>
     * Shortcut variables are as defined below
     * <p>
     * {{p.varName}} for data saved on user prop
     * <p>
     * {{s.varName}} for data saved in common user session
     * <p>
     * {{ s.varName:dataType }} -> {{ s.username:String }}
     * <p>
     * Support datatypes are the Derived Java classes
     * Map, String, Long, Integer, Boolean, Double
     */
    Map<String, Object> processShortcutTemplateVariables(Map<String, Object> nextPayload) {
        try {
            var shortcutVars = Utils.extractShortcutMustacheVariables(SerializeUtils.toJsonString(nextPayload));

            if(!shortcutVars.isEmpty()) {
                var userProps = this.session.getUserProps(this.sessionId);
                var renderer = new RenderProcessor();
                Map<String, Object> filledVars = new HashMap<>();

                shortcutVars.forEach(sv -> {
                    var sc = Utils.parseShortcutVar(sv);
                    if(sv.startsWith("p.")) {
                        filledVars.put(sv, userProps.getOrDefault(sc.name(), ""));
                    } else if(sv.startsWith("s.")) {
                        if(sc.classz() != null) {
                            filledVars.put(sv, Objects.requireNonNullElseGet(this.session.get(this.sessionId, sc.name(), sc.classz()), String::new));
                        } else {
                            filledVars.put(sv, Objects.requireNonNullElseGet(this.session.get(this.sessionId, sc.name()), String::new));
                        }
                    }
                });

                return renderer.renderTemplate(nextPayload, filledVars);
            }
        } catch (Exception e) {
            log.error("Failed to process template shortcut variables: {}", e.getMessage());
        }

        return nextPayload;
    }

    /**
     * Determines the next route or stage in the engine workflow based on the current user input,
     * system state, and predefined routing logic.
     * <p>
     * The method evaluates various conditions like session inactivity, retry routes, dynamic routes,
     * and user input to calculate and return the next stage. If no matching route is found or if
     * the input is invalid, an exception is thrown.
     *
     * @return The next route or stage as a string, determined by the engine's routing logic.
     * @throws SessionInactivityException If the session has been inactive for a prolonged period.
     * @throws ResponseException          If the user input does not correspond to any valid route.
     */
    String getNextRoute() {
        if(this.isFirstTime) return this.stage;
        if(hasInteractionActivityExpired()) {
            throw new SessionInactivityException("You have been inactive for a while, to secure your account, kindly login again");
        }

        var checkpoint = this.session.get(this.sessionId, SessionConstant.SESSION_LATEST_CHECKPOINT_KEY);

        var hasRetryGlobalRoute = new AtomicBoolean(false);

        this.template.getRoutes().forEach((e) -> {
            if(e.getNextStage().equals(EngineConstant.BTN_RETRY)) {
                hasRetryGlobalRoute.set(true);
            }
        });

        boolean gotoCheckpoint = !hasRetryGlobalRoute.get()
                && this.userInput.input().equalsIgnoreCase(EngineConstant.BTN_RETRY)
                && checkpoint != null
                && this.session.get(this.sessionId, SessionConstant.SESSION_DYNAMIC_RETRY_KEY) != null
                && !this.isFromTrigger;

        if(gotoCheckpoint) return checkpoint.toString();

        var hasDynamicRoute = getDynamicRouterRoute();
        if(hasDynamicRoute != null) return hasDynamicRoute;

        if(this.isFromTrigger) return this.stage;

        log.debug("Current stage: {} | template: {}", this.stage, this.template);

        // get the next stage, from defined template routes
        for (EngineRoute trigger : this.template.getRoutes()) {
            if(trigger.isRegex()) {
                if(Utils.isRegexPatternMatch(trigger.getUserInput(), this.userInput.input())) {
                    return trigger.getNextStage();
                }
            } else {
                if(this.userInput.input().equalsIgnoreCase(trigger.getUserInput())) {
                    return trigger.getNextStage();
                }
            }
        }

        throw new ResponseException(new ResponseError(
                this.sessionId,
                "Invalid response, please try again",
                this.stage
        ));
    }

    boolean hasInteractionActivityExpired() {
        if(!this.getConfig().isHandleSessionInactivity() || !this.template.isSession()) return false;

        var authData = this.session.get(this.sessionId, SessionConstant.AUTH_SET_KEY, Boolean.class);
        var lastActive = this.session.get(this.sessionId, SessionConstant.LAST_ACTIVITY_KEY, String.class);

        if(authData != null) {
            return Utils.hasInteractionExpired(lastActive, this.getConfig().getSessionTtlMins());
        }

        return false;
    }

    void authenticate(BaseEngineTemplate template) {
        if(template.isAuthenticated()) {
            if(this.session.get(this.sessionId, SessionConstant.AUTH_SET_KEY) == null) {
                throw new SessionExpiredException("Your session has expired. Kindly login again to access our Services");
            }
        }
    }

    PreProcessorResult preProcessor() throws Exception {
        var shouldProcessPostHooks = !this.isFromTrigger || this.session.get(this.sessionId, SessionConstant.SESSION_DYNAMIC_RETRY_KEY) == null;
        if(shouldProcessPostHooks) {
            this.processPostHooks();
        }

        BaseEngineTemplate nextStageTemplate;
        String nextStage;

        if(hasDynamicTemplateBody(SessionConstant.DYNAMIC_NEXT_TEMPLATE_BODY_KEY)) {
            nextStage = EngineConstant.DYNAMIC_BODY_STAGE_KEY;
            nextStageTemplate = SerializeUtils.toTemplate(this.session.get(this.sessionId, SessionConstant.DYNAMIC_NEXT_TEMPLATE_BODY_KEY, Map.class));
        } else {
            nextStage = this.getNextRoute();

            String finalNextStage = nextStage;
            nextStageTemplate = this.getTemplateStorageManager()
                    .getTemplate(nextStage)
                    .orElseThrow(() -> new InternalException(finalNextStage + " stage not found"));

            if(nextStageTemplate.getRouter() != null && nextStageTemplate.isTransient()) {
                this.template = nextStageTemplate;
                log.warn("Found transient dynamic router -> {}, re-processing..", nextStage);
                if(nestedCallCount >= MAX_NESTED_CALLS) {
                    throw new InternalException("Maximum nested calls reached, dynamic processing halted!");
                }

                nestedPreProcessorResults.add(this.preProcessor());
                nestedCallCount++;
            }

            this.authenticate(nextStageTemplate);
        }

        if(!nestedPreProcessorResults.isEmpty()) {
            var latest = nestedPreProcessorResults.get(0);
            nextStage = latest.stage();
            nextStageTemplate = latest.template();
        }

        log.info("Final next stage: {} ", nextStage);
        return new PreProcessorResult(nextStage, nextStageTemplate);
    }

    public WebhookProcessorResult process(Webhook webhook) throws Exception {
        super.setup(webhook);

        this.nestedPreProcessorResults.clear();
        var results = this.preProcessor();
        var nextTemplate = results.template();
        var nextStage = results.stage();

        this.processPreHooks(nextTemplate);

        var messageRequest = new PayloadGeneratorDto(
                nextTemplate,
                hookArg,
                nextStage,
                this.getHookService()
        );

        var payload = new PayloadGenerator(messageRequest).generate();
        payload = processShortcutTemplateVariables(payload);

        nestedCallCount = 0;
        this.isFromTrigger = false;
        this.session.evict(this.sessionId, SessionConstant.SESSION_DYNAMIC_RETRY_KEY);
        return new WebhookProcessorResult(payload, nextStage, this.sessionId, this.template.isSession());
    }
}
