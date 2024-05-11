package zw.co.dcl.engine.whatsapp.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import zw.co.dcl.engine.whatsapp.constants.EngineConstants;
import zw.co.dcl.engine.whatsapp.constants.SessionConstants;
import zw.co.dcl.engine.whatsapp.entity.dto.*;
import zw.co.dcl.engine.whatsapp.exceptions.*;
import zw.co.dcl.engine.whatsapp.processor.ButtonMessage;
import zw.co.dcl.engine.whatsapp.processor.GeneralText;
import zw.co.dcl.engine.whatsapp.processor.MessageProcessor;
import zw.co.dcl.engine.whatsapp.processor.TemplateMessage;
import zw.co.dcl.engine.whatsapp.service.iface.ISessionManager;
import zw.co.dcl.engine.whatsapp.utils.CommonUtils;

import java.util.*;

public class WhatsappEngineProcessor {
    private final Logger logger = LoggerFactory.getLogger(WhatsappEngineProcessor.class);
    private final WaEngineConfig config;
    private final EngineRequestService service;
    private final ChannelOriginConfig channelOriginConfig;

    public WhatsappEngineProcessor(WaEngineConfig config, ChannelOriginConfig channelOriginConfig) {
        this.config = config;
        this.channelOriginConfig = channelOriginConfig;
        this.service = new EngineRequestService(config);
    }

    public String verifyHubToken(String mode, String challenge, String token) {
        if ("subscribe".equals(mode) && token.equals(config.settings().getHubToken())) return challenge;
        else return "Invalid request";
    }

    public WaCurrentUser verifyWebhookPayload(Object payload, HttpServletRequest request) {
        var map = CommonUtils.linkedHashToMap((LinkedHashMap) payload);
        var req = CommonUtils.requestHeadersToMap(request);

        if (CommonUtils.isChannelErrorMessage(map)) throw new EngineWhatsappException(map.toString());
        if (!CommonUtils.isValidChannelMessage(map))
            throw new EngineInternalException("invalid channel message received");

        if (CommonUtils.hasChannelMsgObject(map)) {
            if (!req.containsKey(EngineConstants.X_HUB_SIG_HEADER_KEY))
                throw new EngineInternalException("unverified request payload");

            Map<String, Object> msgData = CommonUtils.extractWaMessage(map);

            var supportedMsgType = CommonUtils.isValidSupportedMessageType(msgData);
            if (!supportedMsgType.isSupported()) throw new EngineInternalException("unsupported message type");

            WaCurrentUser waCurrentUser = CommonUtils.extractWaCurrentUserObj(map);

            if (channelOriginConfig.restrictOrigin()) {
                if (!CommonUtils.isAllowedChannelOrigin(channelOriginConfig.patterns(), waCurrentUser.waId())) {
                    if (channelOriginConfig.alertOnMismatch()) {
                        logger.warn("blocked mobile origin: {}", waCurrentUser.waId());
                        sendQuickMessage(
                                waCurrentUser.waId(),
                                channelOriginConfig.alertMessage() == null ?
                                        "Kindly note that I have not been configured to process messages matching your network provider" :
                                        channelOriginConfig.alertMessage(),
                                waCurrentUser.msgId()
                        );
                        return null;
                    }

                    throw new EngineInternalException("user origin network: [" + waCurrentUser.waId() + "] not allowed");
                }
            }

            if (channelOriginConfig.whitelistedNumbers() instanceof List allowedNumbers) {
                if (!allowedNumbers.contains(waCurrentUser.waId())) {
                    logger.warn("PROCESS MSG: {} is not whitelisted", waCurrentUser.waId());
                    return null;
                }
            }

            if (channelOriginConfig.whitelistedNumbers() instanceof String matcher) {
                if (!matcher.equals("*")) {
                    logger.error("MSG PROCESSING DISABLED: any number is not whitelisted for processing!");
                    return null;
                }
            }

//            check timestamp timeout
            if (CommonUtils.isOldWebhook(waCurrentUser.timestamp(), config.sessionSettings().getWebhookSecTimestampThreshold())) {
                logger.warn("OLD WEBHOOK REQ RECEIVED: {}. DISCARDED", CommonUtils.convertTimestamp(waCurrentUser.timestamp()));
                return null;
            }

            return waCurrentUser;
        }

        logger.warn("No message obj, ignoring..");
        return null;
    }

    private Set<String> getMessageQueue(ISessionManager sessionManager) {
        Set<String> queue = new HashSet<>();
        if (sessionManager.get(SessionConstants.SESSION_MESSAGE_HISTORY_KEY) != null) {
            var qHistory = sessionManager.get(SessionConstants.SESSION_MESSAGE_HISTORY_KEY, Set.class);
            queue = new HashSet<String>(qHistory);
        }
        return queue;
    }

    void addToMessageQueue(ISessionManager sessionManager, String messageId) {
        Set<String> queue = getMessageQueue(sessionManager);
        queue.add(messageId);
        if (queue.size() > EngineConstants.MESSAGE_QUEUE_COUNT) {
            logger.warn("Message queue limit reached, applying FIFO..");
            Iterator<String> iterator = queue.iterator();
            int count = 0;
            while (iterator.hasNext() && count < queue.size() - 10) {
                iterator.next();
                iterator.remove();
                count++;
            }
        }
        sessionManager.save(SessionConstants.SESSION_MESSAGE_HISTORY_KEY, queue);
    }

    /**
     * pass the request payload from your webhook processor
     * <p>
     * The [channelOriginConfig] defines any engine geography restriction.
     * If you want the engine to process users match a given phone number, pass
     * the pattern to match in this config
     * <p>
     * If you need to send back an alert to user, flag the variable in the config.
     *
     * @param payload WhatsApp json webhook payload
     * @param request webhook request to verify webhook data authenticity
     */
    @SneakyThrows
    public void processWebhook(Object payload, HttpServletRequest request) {
        var map = CommonUtils.linkedHashToMap((LinkedHashMap) payload);
        WaCurrentUser waCurrentUser = verifyWebhookPayload(payload, request);

        if (waCurrentUser == null) return;

        Map<String, Object> msgData = CommonUtils.extractWaMessage(map);
        var supportedMsgType = CommonUtils.isValidSupportedMessageType(msgData);

//        initialize session
        ISessionManager session = config.ISessionManager().session(waCurrentUser.waId());

        logger.warn("Message Queue size: {}", getMessageQueue(session).size());
        if (getMessageQueue(session).contains(waCurrentUser.msgId())) {
            logger.warn("Duplicate message found: {}. Skipping..", msgData);
            return;
        }

        Long lastDebounceTimestamp = session.get(SessionConstants.CURRENT_DEBOUNCE_KEY, Long.class);
        long currentTime = System.currentTimeMillis();

        logger.info("CURRENT DEBOUNCE TS: {} | now: {}", lastDebounceTimestamp, currentTime);
        if (lastDebounceTimestamp == null || currentTime - lastDebounceTimestamp >= config.sessionSettings().getDebounceTimeoutInMs()) {
            session.save(SessionConstants.CURRENT_DEBOUNCE_KEY, currentTime);
        } else {
            logger.warn("Message ignored due to debounce..");
            return;
        }

        addToMessageQueue(session, waCurrentUser.msgId());

        try {
            MessageProcessor msgProcessor = new MessageProcessor(
                    new MsgProcessorDTO(
                            config.templateContext(),
                            config.triggerContext(),
                            session,
                            waCurrentUser,
                            supportedMsgType,
                            msgData,
                            config.sessionSettings()
                    ),
                    this.service
            );

            // MapUtils.debugPrint(System.out, "payload", channelPayload.payload());

            var channelPayload = msgProcessor.process();
            this.service.sendWhatsappRequest(
                    new ChannelRequestDto(session, channelPayload),
                    true,
                    channelOriginConfig
            );

            session.save(SessionConstants.CURRENT_MSG_ID_KEY, waCurrentUser.msgId());
        } catch (EngineResponseException e) {
            logger.warn("Invalid user response err, attempting to relay msg to user..");
            var errBody = CommonUtils.getDataDatumArgs(EngineConstants.ENGINE_EXC_MSG_SPLITTER, e.getMessage());
            this.sendQuickMessage(waCurrentUser.waId(), errBody.other(), null);
        } catch (EngineSessionInactivityException | EngineSessionExpiredException e) {
            logger.error("user session expired or has been inactive for a while, login again");
            sendQuickBtnMsg(
                    new QuickBtnPayload(
                            waCurrentUser.waId(),
                            e.getMessage(),
                            "Session Expired",
                            "Security Check",
                            List.of("Start"),
                            null
                    )
            );
        }
    }

    // helper methods to send quick messages without handling session
    public String sendQuickMessage(String recipient, String message, String replyMsgId) {
        HookArgs args = new HookArgs();
        args.setSession(this.config.ISessionManager().session(recipient));
        args.setChannelUser(new WaCurrentUser(null, recipient, replyMsgId, null));

        final MessageDto messageDto = new MessageDto(
                this.service,
                Map.of("message", message, "type", "text"),
                args,
                (String) args.getSession().get(SessionConstants.CURRENT_STAGE),
                replyMsgId
        );

        GeneralText gt = new GeneralText(messageDto);
        return this.service.sendWhatsappRequest(
                new ChannelRequestDto(
                        null,
                        new MsgProcessorResponseDTO(gt.generatePayload(), null, recipient)
                ),
                false,
                this.channelOriginConfig
        );
    }

    public Object sendQuickBtnMsg(QuickBtnPayload payload) {
        assert payload.buttons().size() <= 3;

        HookArgs args = new HookArgs();
        args.setSession(this.config.ISessionManager().session(payload.recipient()));
        args.setChannelUser(new WaCurrentUser(null, payload.recipient(), null, null));

        Map<String, Object> btnTemplate = new java.util.HashMap<>(Map.of(
                "body", payload.message(),
                "buttons", payload.buttons()
        ));

        if (payload.title() != null) btnTemplate.put("title", payload.title());
        if (payload.footer() != null) btnTemplate.put("footer", payload.footer());

        final MessageDto messageDto = new MessageDto(
                this.service,
                Map.of("message", btnTemplate, "type", "button"),
                args,
                (String) args.getSession().get(SessionConstants.CURRENT_STAGE),
                payload.replyMessageId()
        );

        ButtonMessage bm = new ButtonMessage(messageDto);
        return this.service.sendWhatsappRequest(
                new ChannelRequestDto(
                        null,
                        new MsgProcessorResponseDTO(bm.generatePayload(), null, payload.recipient())
                ),
                false,
                this.channelOriginConfig
        );
    }

    public String sendQuickTemplateMessage(WhatsappTemplateBody dto) {
        HookArgs args = new HookArgs();
        args.setSession(this.config.ISessionManager().session(dto.recipient()));
        args.setChannelUser(new WaCurrentUser(null, dto.recipient(), dto.replyMessageId(), null));

        TemplateMessage tm = new TemplateMessage(dto);

        return this.service.sendWhatsappRequest(
                new ChannelRequestDto(
                        null,
                        new MsgProcessorResponseDTO(
                                tm.generatePayload(),
                                null,
                                dto.recipient()
                        )
                ),
                false,
                this.channelOriginConfig
        );
    }
}