package zw.co.dcl.jawce.engine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import zw.co.dcl.jawce.engine.configs.EngineConfig;
import zw.co.dcl.jawce.engine.configs.WhatsAppConfig;
import zw.co.dcl.jawce.engine.constants.EngineConstants;
import zw.co.dcl.jawce.engine.constants.SessionConstants;
import zw.co.dcl.jawce.engine.exceptions.*;
import zw.co.dcl.jawce.engine.model.core.HookArg;
import zw.co.dcl.jawce.engine.model.core.WaUser;
import zw.co.dcl.jawce.engine.model.dto.*;
import zw.co.dcl.jawce.engine.model.template.ButtonTemplate;
import zw.co.dcl.jawce.engine.processor.ButtonMessage;
import zw.co.dcl.jawce.engine.processor.MessageProcessor;
import zw.co.dcl.jawce.engine.service.iface.ISessionManager;
import zw.co.dcl.jawce.engine.utils.CommonUtils;

import java.util.*;

public class JawceEngineWorker {
    final Logger logger = LoggerFactory.getLogger(JawceEngineWorker.class);

    final WhatsAppConfig waConfig;
    final EngineConfig engineConfig;

    public JawceEngineWorker(WhatsAppConfig whatsappConfig, EngineConfig engineConfig) {
        this.waConfig = whatsappConfig;
        this.engineConfig = engineConfig;
    }

    ISessionManager session(String sessionId) {
        return this.engineConfig.getSessionManager().session(sessionId);
    }

    Set<String> getMessageQueue(WaUser user) {
        Set<String> queue = new HashSet<>();
        if(this.session(user.waId()).get(user.waId(), SessionConstants.SESSION_MESSAGE_HISTORY_KEY) != null) {
            var qHistory = this.session(user.waId()).get(user.waId(), SessionConstants.SESSION_MESSAGE_HISTORY_KEY, Set.class);
            queue = new HashSet<String>(qHistory);
        }
        return queue;
    }

    Optional<WaUser> verifyWebhookPayload(Object payload, Map<String, Object> requestHeaders) {
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

            MDC.put(EngineConstants.MDC_ID_KEY, waUser.waId());

            if(CommonUtils.isOldWebhook(waUser.timestamp(), this.engineConfig.getWebhookTimestampThresholdSecs())) {
                logger.warn("OLD WEBHOOK REQ RECEIVED: {}. DISCARDED", CommonUtils.convertTimestamp(waUser.timestamp()));
                return Optional.empty();
            }

            return Optional.of(waUser);
        }

        logger.warn("No message obj, ignoring..");
        return Optional.empty();
    }

    void addToMessageQueue(WaUser user) {
        Set<String> queue = getMessageQueue(user);
        queue.add(user.msgId());
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
        this.session(user.waId()).save(user.waId(), SessionConstants.SESSION_MESSAGE_HISTORY_KEY, queue);
    }


    public Object sendQuickBtnMsg(ButtonTemplate payload) {
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
        if("subscribe".equals(mode) && token.equals(this.waConfig.getHubToken())) return challenge;
        else throw new RuntimeException("Invalid hub token");
    }

    public void processWebhook(Object payload, Map<String, Object> requestHeaders) {
        var map = CommonUtils.linkedHashToMap((LinkedHashMap) payload);
        var userOpt = this.verifyWebhookPayload(payload, requestHeaders);

        if(userOpt.isEmpty()) return;

        var user = userOpt.get();
        var sessionId = user.waId();
        var msgData = CommonUtils.extractWaMessage(map);
        var supportedMsgType = CommonUtils.isValidSupportedMessageType(msgData);
        var session = this.session(sessionId);

        if(this.engineConfig.isHandleSessionQueue()) {
            if(this.getMessageQueue(user).contains(user.msgId())) {
                logger.warn("Duplicate message found: {}. Skipping..", msgData);
                return;
            }
        }

        Long lastDebounceTimestamp = session.get(sessionId, SessionConstants.CURRENT_DEBOUNCE_KEY, Long.class);
        long currentTime = System.currentTimeMillis();

        if(lastDebounceTimestamp == null || currentTime - lastDebounceTimestamp >= this.engineConfig.getDebounceTimeoutMs()) {
            session.save(sessionId, SessionConstants.CURRENT_DEBOUNCE_KEY, currentTime);
        } else {
            logger.warn("Message ignored due to debounce..");
            return;
        }

        if(this.engineConfig.isHandleSessionQueue()) {
            this.addToMessageQueue(user);
        }

        try {
            MessageProcessor msgProcessor = new MessageProcessor(
                    new MsgProcessorDTO(
                            user,
                            supportedMsgType,
                            msgData
                    ),
                    this.engineConfig
            );

            var channelPayload = msgProcessor.process();

            this.service.sendWhatsappRequest(
                    new ChannelRequestDto(session, channelPayload),
                    true,
                    channelOriginConfig
            );

            session.save(sessionId, SessionConstants.CURRENT_MSG_ID_KEY, user.msgId());
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
        } finally {
            MDC.remove(EngineConstants.MDC_ID_KEY);
        }
    }
}
