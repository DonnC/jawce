package zw.co.dcl.jawce.engine.service;

import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import zw.co.dcl.jawce.engine.constants.EngineConstants;
import zw.co.dcl.jawce.engine.constants.SessionConstants;
import zw.co.dcl.jawce.engine.exceptions.*;
import zw.co.dcl.jawce.engine.model.core.HookArg;
import zw.co.dcl.jawce.engine.model.core.WaUser;
import zw.co.dcl.jawce.engine.model.dto.*;
import zw.co.dcl.jawce.engine.processor.*;
import zw.co.dcl.jawce.engine.utils.CommonUtils;
import zw.co.dcl.jawce.session.ISessionManager;

import java.util.*;

public class EntryService {
    private static volatile EntryService instance;
    private final Logger logger = LoggerFactory.getLogger(EntryService.class);

    private final WaEngineConfig config;
    private final RequestService service;
    private final ChannelOriginConfig channelOriginConfig;

    private EntryService(WaEngineConfig config, ChannelOriginConfig channelOriginConfig) {
        this.config = config;
        this.channelOriginConfig = channelOriginConfig;
        this.service = RequestService.getInstance(config);
    }

    public static EntryService getInstance(WaEngineConfig config, ChannelOriginConfig channelOriginConfig) {
        if(instance == null) {
            synchronized (EntryService.class) {
                if(instance == null) {
                    instance = new EntryService(config, channelOriginConfig);
                }
            }
        }
        return instance;
    }

    private Set<String> getMessageQueue(String sessionId, ISessionManager sessionManager) {
        Set<String> queue = new HashSet<>();
        if(sessionManager.get(sessionId, SessionConstants.SESSION_MESSAGE_HISTORY_KEY) != null) {
            var qHistory = sessionManager.get(sessionId, SessionConstants.SESSION_MESSAGE_HISTORY_KEY, Set.class);
            queue = new HashSet<String>(qHistory);
        }
        return queue;
    }

    public Object sendQuickBtnMsg(QuickBtnPayload payload) {
        assert payload.buttons().size() <= 3;

        HookArg args = new HookArg();
        var userSession = this.config.sessionManager().session(payload.recipient());
        args.setSession(userSession);
        args.setWaUser(new WaUser(null, payload.recipient(), null, null));

        Map<String, Object> btnTemplate = new java.util.HashMap<>(Map.of(
                "body", payload.message(),
                "buttons", new ArrayList<>(payload.buttons())
        ));

        if(payload.title() != null) btnTemplate.put("title", payload.title());
        if(payload.footer() != null) btnTemplate.put("footer", payload.footer());

        final MessageDto messageDto = new MessageDto(
                this.service,
                Map.of("message", btnTemplate, "type", "button"),
                args,
                (String) args.getSession().get(payload.recipient(), SessionConstants.CURRENT_STAGE),
                payload.replyMessageId()
        );

        ButtonMessage bm = new ButtonMessage(messageDto);
        return this.service.sendWhatsappRequest(
                new ChannelRequestDto(
                        userSession,
                        new MsgProcessorResponseDTO(bm.generatePayload(), null, payload.recipient())
                ),
                false,
                this.channelOriginConfig
        );
    }

    public String verifyHubToken(String mode, String challenge, String token) {
        if("subscribe".equals(mode) && token.equals(config.settings().getHubToken())) return challenge;
        else return "Invalid request";
    }

    public WaUser verifyWebhookPayload(Object payload, Map<String, Object> requestHeaders) {
        var map = CommonUtils.linkedHashToMap((LinkedHashMap) payload);

        if(CommonUtils.isChannelErrorMessage(map)) throw new EngineWhatsappException(map.toString());
        if(!CommonUtils.isValidChannelMessage(map))
            throw new EngineInternalException("invalid channel message received");

        if(CommonUtils.hasChannelMsgObject(map)) {
            if(!requestHeaders.containsKey(EngineConstants.X_HUB_SIG_HEADER_KEY))
                throw new EngineInternalException("unverified request payload");

            Map<String, Object> msgData = CommonUtils.extractWaMessage(map);

            var supportedMsgType = CommonUtils.isValidSupportedMessageType(msgData);
            if(!supportedMsgType.isSupported()) throw new EngineInternalException("unsupported message type");

            WaUser waUser = CommonUtils.extractWaCurrentUserObj(map);
            String sessionId = waUser.waId();

            MDC.put(EngineConstants.MDC_ID_KEY, sessionId);

            if(channelOriginConfig.restrictOrigin()) {
                if(!CommonUtils.isAllowedChannelOrigin(channelOriginConfig.patterns(), sessionId)) {
                    if(channelOriginConfig.alertOnMismatch()) {
                        logger.warn("Blocked waId origin");
                        sendQuickMessage(
                                sessionId,
                                channelOriginConfig.alertMessage() == null ?
                                        "Kindly note that I have not been configured to process messages matching your network provider" :
                                        channelOriginConfig.alertMessage(),
                                waUser.msgId()
                        );
                        return null;
                    }

                    throw new EngineInternalException("user origin network: [" + sessionId + "] not allowed");
                }
            }

            if(channelOriginConfig.whitelistedNumbers() instanceof List allowedNumbers) {
                if(!allowedNumbers.contains(sessionId)) {
                    logger.warn("PROCESS MSG: waId not whitelisted");
                    return null;
                }
            }

            if(channelOriginConfig.whitelistedNumbers() instanceof String matcher) {
                if(!matcher.equals("*")) {
                    logger.error("MSG PROCESSING DISABLED: any number is not whitelisted for processing!");
                    return null;
                }
            }

//            check timestamp timeout
            if(CommonUtils.isOldWebhook(waUser.timestamp(), config.sessionSettings().getWebhookSecTimestampThreshold())) {
                logger.warn("OLD WEBHOOK REQ RECEIVED: {}. DISCARDED", CommonUtils.convertTimestamp(waUser.timestamp()));
                return null;
            }

            return waUser;
        }

        logger.warn("No message obj, ignoring..");
        return null;
    }

    void addToMessageQueue(String sessionId, ISessionManager sessionManager, String messageId) {
        Set<String> queue = getMessageQueue(sessionId, sessionManager);
        queue.add(messageId);
        if(queue.size() > EngineConstants.MESSAGE_QUEUE_COUNT) {
            logger.warn("Message queue limit reached, applying FIFO..");
            Iterator<String> iterator = queue.iterator();
            int count = 0;
            while (iterator.hasNext() && count < queue.size() - 10) {
                iterator.next();
                iterator.remove();
                count++;
            }
        }
        sessionManager.save(sessionId, SessionConstants.SESSION_MESSAGE_HISTORY_KEY, queue);
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
     * @param payload        WhatsAppConfig json webhook payload
     * @param requestHeaders webhook request to verify webhook data authenticity
     */
    @SneakyThrows
    public void processWebhook(Object payload, Map<String, Object> requestHeaders) {
        var map = CommonUtils.linkedHashToMap((LinkedHashMap) payload);
        WaUser waUser = verifyWebhookPayload(payload, requestHeaders);

        if(waUser == null) return;

        String sessionId = waUser.waId();

        Map<String, Object> msgData = CommonUtils.extractWaMessage(map);
        var supportedMsgType = CommonUtils.isValidSupportedMessageType(msgData);

        var session = config.sessionManager().session(sessionId);

        if(config.sessionSettings().isHandleSessionQueue()) {
            if(getMessageQueue(sessionId, session).contains(waUser.msgId())) {
                logger.warn("Duplicate message found: {}. Skipping..", msgData);
                return;
            }
        }

        Long lastDebounceTimestamp = session.get(sessionId, SessionConstants.CURRENT_DEBOUNCE_KEY, Long.class);
        long currentTime = System.currentTimeMillis();

        if(lastDebounceTimestamp == null || currentTime - lastDebounceTimestamp >= config.sessionSettings().getDebounceTimeoutInMs()) {
            session.save(sessionId, SessionConstants.CURRENT_DEBOUNCE_KEY, currentTime);
        } else {
            logger.warn("Message ignored due to debounce..");
            return;
        }

        if(config.sessionSettings().isHandleSessionQueue()) {
            addToMessageQueue(sessionId, session, waUser.msgId());
        }

        try {
            MessageProcessor msgProcessor = new MessageProcessor(
                    new MsgProcessorDTO(
                            waUser,
                            supportedMsgType,
                            msgData
                    ),
                    this.config
            );

            var channelPayload = msgProcessor.process();

            this.service.sendWhatsappRequest(
                    new ChannelRequestDto(session, channelPayload),
                    true,
                    channelOriginConfig
            );

            session.save(sessionId, SessionConstants.CURRENT_MSG_ID_KEY, waUser.msgId());
        } catch (EngineRenderException e) {
            sendQuickBtnMsg(
                    new QuickBtnPayload(
                            sessionId,
                            "Failed to process your message",
                            null,
                            "Message",
                            List.of("Retry", "Report"),
                            null
                    )
            );
        } catch (EngineResponseException e) {
            var errBody = CommonUtils.getDataDatumArgs(EngineConstants.ENGINE_EXC_MSG_SPLITTER, e.getMessage());
            logger.warn("[{}] Stage: {} | {} Invalid response err", waUser.msgId(), errBody.data(), errBody.datum());

            sendQuickBtnMsg(
                    new QuickBtnPayload(
                            sessionId,
                            "%s.\n\n%s".formatted(errBody.other(), "You may click the button to return to Menu"),
                            null,
                            "Message",
                            List.of("Menu", "Report"),
                            null
                    )
            );
        } catch (UserSessionValidationException e) {
            logger.warn("Ambiguous session mismatch, Dialing user: {} | Session user: {}",
                    waUser.msgId(),
                    session.get(sessionId, SessionConstants.SERVICE_PROFILE_MSISDN_KEY, String.class)
            );
            logger.warn(e.getMessage());

            sendQuickBtnMsg(
                    new QuickBtnPayload(
                            sessionId,
                            "Could not process request (AMB.ERR)",
                            "Tip: type / for shortcuts",
                            "Message",
                            List.of("Menu"),
                            null
                    )
            );
        } catch (EngineSessionInactivityException | EngineSessionExpiredException e) {
            session.clear(sessionId);
            logger.error("[{}] Session expired / inactive - cleared", waUser.msgId());
            sendQuickBtnMsg(
                    new QuickBtnPayload(
                            sessionId,
                            e.getMessage(),
                            "Session Expired",
                            "Security Check 🔐",
                            List.of("Menu"),
                            null
                    )
            );
        }

        finally {
            MDC.remove(EngineConstants.MDC_ID_KEY);
        }
    }

    // helper methods to send quick messages without handling session
    public String sendQuickMessage(String recipient, String message, String replyMsgId) {
        HookArg args = new HookArg();
        var userSession = this.config.sessionManager().session(recipient);
        args.setSession(userSession);
        args.setWaUser(new WaUser(null, recipient, replyMsgId, null));

        final MessageDto messageDto = new MessageDto(
                this.service,
                Map.of("message", message, "type", "text"),
                args,
                userSession.get(recipient, SessionConstants.CURRENT_STAGE, String.class),
                replyMsgId
        );

        GeneralText gt = new GeneralText(messageDto);
        return this.service.sendWhatsappRequest(
                new ChannelRequestDto(
                        userSession,
                        new MsgProcessorResponseDTO(gt.generatePayload(), null, recipient)
                ),
                false,
                this.channelOriginConfig
        );
    }

    public Object sendRetryBtnMsg(
            String recipient,
            String message,
            String title,
            String footer,
            boolean isOutsideMenu
    ) {
        List<String> buttons = new ArrayList<>();
        buttons.add(isOutsideMenu ? "Home" : "Menu");
        buttons.add(EngineConstants.RETRY_NAME);

        return sendQuickBtnMsg(
                new QuickBtnPayload(
                        recipient,
                        message,
                        footer,
                        title,
                        buttons,
                        null
                )
        );
    }

    public void reactToMessage(String recipient, String messageId, String emoji) {
        HookArg args = new HookArg();
        var userSession = this.config.sessionManager().session(recipient);
        args.setSession(userSession);
        args.setWaUser(new WaUser(null, recipient, messageId, null));

        if(emoji == null || emoji.isEmpty()) {
            emoji = EngineConstants.CHANNEL_LOADING_REACTION;
        }

        ReactionMessage rm = new ReactionMessage(
                recipient,
                Map.of(
                        "message_id", messageId,
                        "emoji", emoji
                )
        );

        this.service.sendWhatsappRequest(
                new ChannelRequestDto(
                        userSession,
                        new MsgProcessorResponseDTO(
                                rm.generatePayload(),
                                null,
                                recipient
                        )
                ),
                false,
                this.channelOriginConfig
        );
    }

    public String sendQuickTemplateMessage(WhatsappTemplateBody dto) {
        HookArg args = new HookArg();
        var userSession = this.config.sessionManager().session(dto.recipient());
        args.setSession(userSession);
        args.setWaUser(new WaUser(null, dto.recipient(), dto.replyMessageId(), null));

        TemplateMessage tm = new TemplateMessage(dto);

        return this.service.sendWhatsappRequest(
                new ChannelRequestDto(
                        userSession,
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

    /**
     * send a once off request - without tracking user session
     * <p>
     * helpful when other services tries to send message via this engine
     *
     * @param dto payload type
     * @return WhatsAppConfig upstream response
     */
    public String sendOnceOffMessage(OnceOffRequestDto dto) {
        return this.service.sendOnceOffWhatsappRequest(dto, null);
    }
}
