package zw.co.dcl.jawce.engine.api;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import zw.co.dcl.jawce.engine.api.dto.PayloadGeneratorDto;
import zw.co.dcl.jawce.engine.api.dto.QuickBtnTemplate;
import zw.co.dcl.jawce.engine.api.exceptions.*;
import zw.co.dcl.jawce.engine.api.iface.ISessionManager;
import zw.co.dcl.jawce.engine.api.utils.PayloadGenerator;
import zw.co.dcl.jawce.engine.api.utils.WhatsAppUtils;
import zw.co.dcl.jawce.engine.configs.JawceConfig;
import zw.co.dcl.jawce.engine.configs.WhatsAppConfig;
import zw.co.dcl.jawce.engine.constants.EngineConstant;
import zw.co.dcl.jawce.engine.constants.SessionConstant;
import zw.co.dcl.jawce.engine.internal.dto.Webhook;
import zw.co.dcl.jawce.engine.internal.events.OnceOffHookEvent;
import zw.co.dcl.jawce.engine.internal.events.OnceOffMessageEvent;
import zw.co.dcl.jawce.engine.internal.events.WebhookEvent;
import zw.co.dcl.jawce.engine.internal.recovery.RecoveryDecision;
import zw.co.dcl.jawce.engine.internal.recovery.RecoveryPolicyResolver;
import zw.co.dcl.jawce.engine.internal.state.ConversationState;
import zw.co.dcl.jawce.engine.internal.service.HistoryEventPublisher;
import zw.co.dcl.jawce.engine.internal.service.WebhookProcessor;
import zw.co.dcl.jawce.engine.internal.service.WhatsAppHelperService;
import zw.co.dcl.jawce.engine.model.core.Hook;
import zw.co.dcl.jawce.engine.model.core.WaUser;
import zw.co.dcl.jawce.engine.model.dto.WebhookProcessorResult;
import zw.co.dcl.jawce.engine.model.history.ChatHistoryEvent;
import zw.co.dcl.jawce.engine.model.history.HistoryEventType;
import zw.co.dcl.jawce.engine.model.messages.ButtonMessage;
import zw.co.dcl.jawce.engine.model.template.ButtonTemplate;

import java.util.*;

@Slf4j
public class Worker {
    final ApplicationEventPublisher eventPublisher;
    final WhatsAppConfig waConfig;
    final JawceConfig jawceConfig;
    final WhatsAppHelperService service;
    final WebhookProcessor webhookProcessor;
    final HistoryEventPublisher historyEventPublisher;
    ISessionManager session;

    public Worker(
            ApplicationEventPublisher eventPublisher,
            WhatsAppConfig whatsAppConfig,
            JawceConfig jawceConfig,
            WhatsAppHelperService whatsAppHelperService,
            WebhookProcessor webhookProcessor,
            ISessionManager sessionManager,
            HistoryEventPublisher historyEventPublisher) {
        this.eventPublisher = eventPublisher;
        this.waConfig = whatsAppConfig;
        this.jawceConfig = jawceConfig;
        this.service = whatsAppHelperService;
        this.webhookProcessor = webhookProcessor;
        this.session = sessionManager;
        this.historyEventPublisher = historyEventPublisher;
    }

    ChatHistoryEvent.ChatHistoryEventBuilder userEventBuilder(WaUser user, HistoryEventType type) {
        return ChatHistoryEvent.builder()
                .timestamp(zw.co.dcl.jawce.engine.api.utils.Utils.currentSystemDate().toString())
                .type(type)
                .sessionId(user.waId())
                .waId(user.waId())
                .messageId(user.msgId());
    }

    void publishInboundEvent(WaUser user, HistoryEventType type, String detail, Map<String, Object> payload, Map<String, Object> metadata) {
        var builder = userEventBuilder(user, type)
                .direction("inbound")
                .detail(detail)
                .payload(payload == null ? Map.of() : payload)
                .metadata(metadata == null ? Map.of() : metadata);
        this.historyEventPublisher.publish(builder.build());
    }

    void publishEngineError(WaUser user, String stage, Exception e) {
        this.historyEventPublisher.publish(userEventBuilder(user, HistoryEventType.ENGINE_ERROR)
                .direction("system")
                .stage(stage)
                .success(false)
                .detail(e.getMessage())
                .metadata(Map.of("errorType", e.getClass().getSimpleName()))
                .build());
    }

    void applyRecoveryDecision(WaUser user, RecoveryDecision decision) {
        var conversationState = new ConversationState(user.waId(), this.session);
        if(decision.isClearSession()) {
            conversationState.clear();
        }

        if(decision.isMarkRetryPending()) {
            conversationState.markRetryPending();
        }

        conversationState.rememberRecoveryActions(decision.getRecoveryActions());

        this.sendQuickButtonMessage(decision.getQuickButtonTemplate());
    }

    void recoverFrom(WaUser user, Exception exception) {
        this.applyRecoveryDecision(user, RecoveryPolicyResolver.resolve(user, exception));
    }

    Set<String> getMessageQueue(WaUser user) {
        return new ConversationState(user.waId(), this.session).messageHistory();
    }

    void addToMessageQueue(WaUser user) {
        var conversationState = new ConversationState(user.waId(), this.session);
        var existingSize = conversationState.messageHistory().size();
        conversationState.rememberMessageId(user.msgId());
        if(existingSize >= EngineConstant.MESSAGE_QUEUE_COUNT) {
            log.warn("Message queue limit reached, applying FIFO..");
        }
    }

    void fireGlobalHook(String sessionId) {
        if(this.jawceConfig.getOnWebhookPrechecksComplete() != null) {
            var tempHook = new Hook();
            tempHook.setHook(this.jawceConfig.getOnWebhookPrechecksComplete());
            tempHook.setSessionId(sessionId);
            tempHook.setSession(this.session);

            eventPublisher.publishEvent(new OnceOffHookEvent(this, tempHook));
        }
    }

    Optional<Webhook> initChecks(Map<String, Object> webhookPayload) {
        if(WhatsAppUtils.isRequestErrorMessage(webhookPayload)) {
            throw new WhatsAppException(webhookPayload.toString());
        }

        var userOpt = WhatsAppUtils.getUser(webhookPayload);
        if(userOpt.isEmpty()) return Optional.empty();

        var user = userOpt.get();
        var sessionId = user.waId();
        var conversationState = new ConversationState(sessionId, this.session);
        var webhookTtl = this.jawceConfig.getWebhookTimestampThresholdSecs();

        if(webhookTtl > 0 && WhatsAppUtils.isOldWebhook(user.timestamp(), webhookTtl)) {
            log.warn("Old webhook received: {}. Discarded!", WhatsAppUtils.convertTimestamp(user.timestamp()));
            publishInboundEvent(
                    user,
                    HistoryEventType.INBOUND_SKIPPED_STALE,
                    "Old webhook discarded",
                    webhookPayload,
                    Map.of("thresholdSecs", webhookTtl)
            );
            return Optional.empty();
        }

        var message = WhatsAppUtils.extractMessage(webhookPayload);
        var responseStructure = WhatsAppUtils.getResponseStructure(message, false);
        this.session = this.session.session(sessionId);

        if(this.jawceConfig.isHandleSessionQueue()) {
            if(conversationState.hasProcessedMessageId(user.msgId())) {
                log.warn("Duplicate message found: {}. Skipping..", message);
                publishInboundEvent(
                        user,
                        HistoryEventType.INBOUND_SKIPPED_DUPLICATE,
                        "Duplicate inbound message skipped",
                        message,
                        Map.of("responseType", responseStructure.type().name())
                );
                return Optional.empty();
            }
        }

        Long lastDebounceTimestamp = conversationState.currentDebounceTimestamp();
        long currentTime = System.currentTimeMillis();

        var debounceTime = this.jawceConfig.isEmulate() ? 0 : this.jawceConfig.getDebounceTimeoutMs();

        if(lastDebounceTimestamp == null || currentTime - lastDebounceTimestamp >= debounceTime) {
            conversationState.markDebounceAt(currentTime);
        } else {
            log.warn("Message ignored due to debounce..");
            publishInboundEvent(
                    user,
                    HistoryEventType.INBOUND_SKIPPED_DEBOUNCE,
                    "Inbound message ignored due to debounce",
                    message,
                    Map.of("debounceTimeoutMs", debounceTime)
            );
            return Optional.empty();
        }

        if(this.jawceConfig.isHandleSessionQueue()) {
            this.addToMessageQueue(user);
        }

        conversationState.saveDefaultProfile(user.name(), user.waId());

        publishInboundEvent(
                user,
                HistoryEventType.INBOUND_RECEIVED,
                "Inbound webhook accepted",
                message,
                Map.of("responseType", responseStructure.type().name())
        );

        return Optional.of(new Webhook(user, responseStructure));
    }

    @SneakyThrows
    public void sendQuickButtonMessage(QuickBtnTemplate button) {
        var btn = ButtonTemplate.builder()
                .replyMessageId(button.getMessageId())
                .message(ButtonMessage.builder()
                        .buttons(button.getButtons())
                        .body(button.getMessage())
                        .footer(button.getFooter())
                        .title(button.getTitle())
                        .build())
                .build();

        var hook = new Hook();
        hook.setSession(this.session.session(button.getRecipient()));
        hook.setSessionId(button.getRecipient());
        hook.setWaUser(new WaUser(button.getRecipient(), button.getRecipient(), null, null));

        var messageRequest = new PayloadGeneratorDto(
                btn,
                hook,
                null,
                null,
                this.jawceConfig.isTagOnReply()
        );

        var payload = new PayloadGenerator(messageRequest).generate();
        var resultPayload = new WebhookProcessorResult(payload, null, button.getRecipient(), false, java.util.List.of());
        this.historyEventPublisher.publish(ChatHistoryEvent.builder()
                .timestamp(zw.co.dcl.jawce.engine.api.utils.Utils.currentSystemDate().toString())
                .type(HistoryEventType.OUTBOUND_GENERATED)
                .direction("outbound")
                .sessionId(button.getRecipient())
                .waId(button.getRecipient())
                .messageId(button.getMessageId())
                .templateType(btn.getType())
                .detail("Generated quick button fallback")
                .payload(payload)
                .metadata(Map.of("source", "quick-button"))
                .build());
        this.service.sendWhatsAppRequest(resultPayload);
    }

    public void processOnceOffMessage(OnceOffMessageEvent event) {
        try {
            var hook = new Hook();
            hook.setSession(this.session.session(event.getUser().waId()));
            hook.setSessionId(event.getUser().waId());
            hook.setWaUser(event.getUser());

            var messageRequest = new PayloadGeneratorDto(
                    event.getTemplate(),
                    hook,
                    null,
                    null,
                    false
            );

            var payload = new PayloadGenerator(messageRequest).generate();
            var resultPayload = new WebhookProcessorResult(payload, null, event.getUser().waId(), false, java.util.List.of());
            this.historyEventPublisher.publish(userEventBuilder(event.getUser(), HistoryEventType.OUTBOUND_GENERATED)
                    .direction("outbound")
                    .templateType(event.getTemplate().getType())
                    .detail("Generated once-off outbound message")
                    .payload(payload)
                    .metadata(Map.of("source", "once-off"))
                    .build());
            var response = this.service.sendWhatsAppRequest(resultPayload);

            log.info("Once-off-message result: {}", WhatsAppUtils.isValidRequestResponse(response));
        } catch (Exception e) {
            log.error("Error processing once off message", e);
        }
    }

    public int verifyHubToken(String mode, String challenge, String token) {
        if("subscribe".equals(mode) && token.equals(this.waConfig.getHubToken())) return Integer.parseInt(challenge);
        throw new RuntimeException("Challenge failed, invalid hub token!");
    }

    public void processWebhook(Map<String, Object> webhookPayload) {
        var webhook = this.initChecks(webhookPayload);

        webhook.ifPresent(message -> {
            this.fireGlobalHook(message.user().waId());

            try {
                var result = this.webhookProcessor.process(message);
                var response = this.service.sendWhatsAppRequest(result);
                new ConversationState(message.user().waId(), this.session).setCurrentMessageId(message.user().msgId());
                log.debug("Webhook process response result: {} for msg: {}", WhatsAppUtils.isValidRequestResponse(response), message.user().msgId());
            } catch (HookException e) {
                log.error("Hook processing failed: {}", e.getMessage());
                publishEngineError(message.user(), new ConversationState(message.user().waId(), this.session).currentStage(), e);
                this.recoverFrom(message.user(), e);
            } catch (TemplateRenderException e) {
                log.error("Template render failed: {}", e.getMessage());
                publishEngineError(message.user(), new ConversationState(message.user().waId(), this.session).currentStage(), e);
                this.recoverFrom(message.user(), e);
            } catch (ResponseException e) {
                log.error("Engine response exception: {}", e.getError());
                publishEngineError(message.user(), e.getError().stage(), e);
                this.recoverFrom(message.user(), e);
            } catch (UserSessionValidationException e) {
                log.error("User session validation failed: {}", e.getMessage());
                publishEngineError(message.user(), new ConversationState(message.user().waId(), this.session).currentStage(), e);
                this.recoverFrom(message.user(), e);
            } catch (SessionExpiredException | SessionInactivityException e) {
                log.error("Session expired / inactive, clearing user session..");
                publishEngineError(message.user(), new ConversationState(message.user().waId(), this.session).currentStage(), e);
                this.recoverFrom(message.user(), e);
            } catch (Exception e) {
                log.error("Engine failed to process webhook: {}", e.getMessage(), e);
                publishEngineError(message.user(), new ConversationState(message.user().waId(), this.session).currentStage(), e);
                this.recoverFrom(message.user(), e);
            } finally {
                MDC.remove(EngineConstant.MDC_WA_ID_KEY);
                MDC.remove(EngineConstant.MDC_WA_NAME_KEY);
            }
        });
    }

    @EventListener
    public void handleMessageEvent(OnceOffMessageEvent event) {
        this.processOnceOffMessage(event);
    }

    @EventListener
    public void handleWebhookEvent(WebhookEvent event) {
        this.processWebhook(event.getPayload());
    }
}
