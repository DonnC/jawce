package zw.co.dcl.engine.whatsapp.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import zw.co.dcl.engine.whatsapp.constants.EngineConstants;
import zw.co.dcl.engine.whatsapp.constants.SessionConstants;
import zw.co.dcl.engine.whatsapp.entity.dto.EnginePreProcessor;
import zw.co.dcl.engine.whatsapp.entity.dto.MessageDto;
import zw.co.dcl.engine.whatsapp.entity.dto.MsgProcessorDTO;
import zw.co.dcl.engine.whatsapp.entity.dto.MsgProcessorResponseDTO;
import zw.co.dcl.engine.whatsapp.exceptions.EngineInternalException;
import zw.co.dcl.engine.whatsapp.exceptions.EngineResponseException;
import zw.co.dcl.engine.whatsapp.exceptions.EngineSessionExpiredException;
import zw.co.dcl.engine.whatsapp.exceptions.EngineSessionInactivityException;
import zw.co.dcl.engine.whatsapp.processor.abstracts.ChannelMessageProcessor;
import zw.co.dcl.engine.whatsapp.processor.iface.IMessageProcessor;
import zw.co.dcl.engine.whatsapp.service.EngineRequestService;
import zw.co.dcl.engine.whatsapp.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MessageProcessor extends ChannelMessageProcessor implements IMessageProcessor {
    private final Logger logger = LoggerFactory.getLogger(MessageProcessor.class);
    //        can have nested transient results
    List<EnginePreProcessor> nestedPreProcessorResults;

    public MessageProcessor(MsgProcessorDTO messageProcessorDTO, EngineRequestService engineService) {
        super(messageProcessorDTO, engineService);
    }

    @Override
    public String getNextRoute() {
        if (this.isFirstTime) return dto.sessionSettings().getStartMenuStageKey();

        if (hasInteractionActivityExpired()) {
            logger.info("User interactive session has expired. Login again");
            this.dto.ISessionManager().save(SessionConstants.IS_FROM_ACTIVITY_EXPIRY, true);
            throw new EngineSessionInactivityException("You have been inactive for a while, to secure your account, kindly login again");
        }

        String currentStage = (String) this.dto.ISessionManager().get(SessionConstants.CURRENT_STAGE);
        var currentStageRoutes = (Map<String, Object>) this.currentStageTpl.get(EngineConstants.TPL_ROUTES_KEY);

        var checkpoint = this.dto.ISessionManager().get(SessionConstants.SESSION_LATEST_CHECKPOINT_KEY);

        boolean gotoCheckpoint = !this.templateHasKey(currentStageRoutes, EngineConstants.RETRY_NAME)
                && this.currentStageUserInput.toString().equalsIgnoreCase(EngineConstants.RETRY_NAME)
                && checkpoint != null
                && this.dto.ISessionManager().get(SessionConstants.SESSION_DYNAMIC_RETRY_KEY) != null
                && !this.isFromTrigger;

        if (gotoCheckpoint) return checkpoint.toString();

//        handle dynamic router
        var hasDynamicRoute = dynamicRouter();
        if (hasDynamicRoute != null) return hasDynamicRoute;

        for (Map.Entry<String, Object> route : currentStageRoutes.entrySet()) {
            if (CommonUtils.isRegexInput(route.getKey().trim())) {
                if (CommonUtils.isRegexPatternMatch(
                        CommonUtils.getRegexPattern(route.getKey().trim()),
                        this.currentStageUserInput.input().trim()
                )) {
                    return route.getValue().toString().trim();
                }
            }
        }

        if (this.templateHasKey(currentStageRoutes, this.currentStageUserInput.input().trim()))
            return currentStageRoutes.get(this.currentStageUserInput.input().trim()).toString();

        throw new EngineResponseException(CommonUtils.createEngineErrorMsg(
                currentStage,
                this.dto.waCurrentUser().waId(),
                "Invalid response, please try again"
        ));
    }

    @Override
    public boolean hasInteractionActivityExpired() {
        if (!this.dto.sessionSettings().isHandleSessionInactivity()) return false;

        if (this.dto.ISessionManager().get(SessionConstants.SERVICE_PROFILE_MSISDN_KEY) != null) {
            String lastActive = (String) this.dto.ISessionManager().get(SessionConstants.LAST_ACTIVITY_KEY);
            return CommonUtils.hasInteractionExpired(lastActive, this.dto.sessionSettings().getInactivityTimeout());
        }

        return false;
    }

    @Override
    public Map<String, Object> authenticate(Map<String, Object> template) {
        if (this.templateHasKey(template, EngineConstants.TPL_AUTHENTICATED_KEY)) {
            String loggedInTime = (String) this.dto.ISessionManager().get(SessionConstants.SESSION_EXPIRY);
            String sessionUid = (String) this.dto.ISessionManager().get(SessionConstants.SERVICE_PROFILE_MSISDN_KEY);

//            TODO: check session expiry timeout
//            implemented cache store has TTL, so it will be handled automatically
//            if any of these is null, probably the cache is new or is cleared, therefore no auth
            if (loggedInTime == null || sessionUid == null) {
                this.dto.ISessionManager().clear();
                throw new EngineSessionExpiredException("Your session has expired. Kindly login again to access our WhatsApp Services");
            }
        }

        return template;
    }

    @Override
    public EnginePreProcessor preProcessor() throws Exception {
        if (this.dto.ISessionManager().get(SessionConstants.SESSION_DYNAMIC_RETRY_KEY) == null)
            this.processPostHooks();

        Map<String, Object> nTemplate;
        String nStage;

        if (hasEngineFullDynamicTemplateBody(SessionConstants.DYNAMIC_NEXT_TEMPLATE_BODY_KEY)) {
            logger.info("Full dynamic template body found. Setting as next tpl..");
            nStage = EngineConstants.DYNAMIC_BODY_STAGE_KEY;
            nTemplate = (Map) this.dto.ISessionManager().get(SessionConstants.DYNAMIC_NEXT_TEMPLATE_BODY_KEY);
        }

        else {
            nStage = this.getNextRoute();

            if (!this.templateHasKey(this.dto.tplContextMap(), nStage))
                throw new EngineInternalException("route: " + nStage + " not found");

            nTemplate = (Map) this.dto.tplContextMap().get(nStage);

            if (this.templateHasKey(nTemplate, EngineConstants.TPL_DYNAMIC_ROUTER_KEY)
                    && this.templateHasKey(nTemplate, EngineConstants.TPL_ROUTE_TRANSIENT_KEY)
            ) {
                this.currentStageTpl = nTemplate;
                logger.info("Found transient flow dynamic router -> {}, repeating pre-processor..", nStage);
                nestedPreProcessorResults.add(this.preProcessor());
            }

            nTemplate = this.authenticate(nTemplate);
        }

        if (!nestedPreProcessorResults.isEmpty()) {
            var latest = nestedPreProcessorResults.get(0);
            nStage = latest.stage();
            nTemplate = latest.template();
        }

        logger.info("~Next stage: {} ", nStage);
        return new EnginePreProcessor(nStage, nTemplate);
    }

    @Override
    public MsgProcessorResponseDTO process() throws Exception {
        nestedPreProcessorResults = new ArrayList<>();
        EnginePreProcessor preResult = this.preProcessor();
        Map<String, Object> nextStageTpl = preResult.template();
        String nextStage = preResult.stage();

        this.processPreHooks(nextStageTpl);

        final MessageDto messageDto = new MessageDto(
                engineService,
                nextStageTpl,
                hookArgs,
                (String) this.dto.ISessionManager().get(SessionConstants.CURRENT_STAGE),
                CommonUtils.getPreviousMessageId(nextStageTpl)
        );

        Map<String, Object> payload = switch (nextStageTpl.get("type").toString()) {
            case "dynamic" -> {
                var dText = new DynamicMessage(messageDto);
                yield dText.generatePayload();
            }
            case "text" -> {
                var gText = new GeneralText(messageDto);
                yield gText.generatePayload();
            }
            case "button" -> {
                var btn = new ButtonMessage(messageDto);
                yield btn.generatePayload();
            }
            case "list" -> {
                var intrMessage = new InteractiveListMessage(messageDto);
                yield intrMessage.generatePayload();
            }
            case "flow" -> {
                var flowMessage = new FlowMessage(messageDto);
                yield flowMessage.generatePayload();
            }
            case "document", "image" -> {
                var docMsg = new MediaMessage(messageDto);
                yield docMsg.generatePayload();
            }
            case "template" -> {
                var tText = new TemplateMessage(messageDto);
                yield tText.generatePayload();
            }
            default ->
                    throw new EngineInternalException("specified template type not supported for stage: " + nextStage);
        };

        this.setFromTrigger(false);
        this.dto.ISessionManager().evict(SessionConstants.SESSION_DYNAMIC_RETRY_KEY);

        return new MsgProcessorResponseDTO(payload, nextStage, dto.waCurrentUser().waId());
    }
}
