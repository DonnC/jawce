package zw.co.dcl.jawce.engine.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import zw.co.dcl.jawce.engine.configs.JawceConfig;
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

public class Worker {
    final Logger logger = LoggerFactory.getLogger(Worker.class);

    final WhatsAppConfig waConfig;
    final JawceConfig jawceConfig;
    ISessionManager session;

    public Worker(WhatsAppConfig whatsappConfig, JawceConfig jawceConfig, ISessionManager sessionManager) {
        this.waConfig = whatsappConfig;
        this.jawceConfig = jawceConfig;
        this.session = sessionManager;
    }

    Set<String> getMessageQueue(WaUser user) {
        Set<String> queue = new HashSet<>();
        if(this.session.get(user.waId(), SessionConstants.SESSION_MESSAGE_HISTORY_KEY) != null) {
            var qHistory = this.session.get(user.waId(), SessionConstants.SESSION_MESSAGE_HISTORY_KEY, Set.class);
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

            if(CommonUtils.isOldWebhook(waUser.timestamp(), this.jawceConfig.getWebhookTimestampThresholdSecs())) {
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
        this.session.save(user.waId(), SessionConstants.SESSION_MESSAGE_HISTORY_KEY, queue);
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
        this.session = this.session.session(sessionId);

        if(this.jawceConfig.isHandleSessionQueue()) {
            if(this.getMessageQueue(user).contains(user.msgId())) {
                logger.warn("Duplicate message found: {}. Skipping..", msgData);
                return;
            }
        }

        Long lastDebounceTimestamp = this.session.get(sessionId, SessionConstants.CURRENT_DEBOUNCE_KEY, Long.class);
        long currentTime = System.currentTimeMillis();

        if(lastDebounceTimestamp == null || currentTime - lastDebounceTimestamp >= this.jawceConfig.getDebounceTimeoutMs()) {
            this.session.save(sessionId, SessionConstants.CURRENT_DEBOUNCE_KEY, currentTime);
        } else {
            logger.warn("Message ignored due to debounce..");
            return;
        }

        if(this.jawceConfig.isHandleSessionQueue()) {
            this.addToMessageQueue(user);
        }

        try {
            MessageProcessor msgProcessor = new MessageProcessor(
                    new MsgProcessorDTO(
                            user,
                            supportedMsgType,
                            msgData,
                            this.session
                    ),
                    this.jawceConfig
            );

            var channelPayload = msgProcessor.process();

            this.service.sendWhatsappRequest(
                    new ChannelRequestDto(this.session.session(sessionId), channelPayload),
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
