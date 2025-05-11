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
import zw.co.dcl.jawce.engine.api.utils.WhatsappUtils;
import zw.co.dcl.jawce.engine.configs.JawceConfig;
import zw.co.dcl.jawce.engine.configs.WhatsAppConfig;
import zw.co.dcl.jawce.engine.constants.EngineConstant;
import zw.co.dcl.jawce.engine.constants.SessionConstant;
import zw.co.dcl.jawce.engine.internal.dto.Webhook;
import zw.co.dcl.jawce.engine.internal.events.OnceOffHookEvent;
import zw.co.dcl.jawce.engine.internal.events.WebhookEvent;
import zw.co.dcl.jawce.engine.internal.service.ClientHelperService;
import zw.co.dcl.jawce.engine.internal.service.WebhookProcessor;
import zw.co.dcl.jawce.engine.model.core.Hook;
import zw.co.dcl.jawce.engine.model.core.WaUser;
import zw.co.dcl.jawce.engine.model.dto.WebhookProcessorResult;
import zw.co.dcl.jawce.engine.model.messages.ButtonMessage;
import zw.co.dcl.jawce.engine.model.template.ButtonTemplate;

import java.util.*;

@Slf4j
public class Worker {
    final ApplicationEventPublisher eventPublisher;
    final WhatsAppConfig waConfig;
    final JawceConfig jawceConfig;
    final ClientHelperService service;
    final WebhookProcessor webhookProcessor;
    ISessionManager session;

    public Worker(
            ApplicationEventPublisher eventPublisher,
            WhatsAppConfig whatsAppConfig,
            JawceConfig jawceConfig,
            ClientHelperService clientHelperService,
            WebhookProcessor webhookProcessor,
            ISessionManager sessionManager) {
        this.eventPublisher = eventPublisher;
        this.waConfig = whatsAppConfig;
        this.jawceConfig = jawceConfig;
        this.service = clientHelperService;
        this.webhookProcessor = webhookProcessor;
        this.session = sessionManager;
    }

    Set<String> getMessageQueue(WaUser user) {
        Set<String> queue = new HashSet<>();
        if(this.session.get(user.waId(), SessionConstant.SESSION_MESSAGE_HISTORY_KEY) != null) {
            var qHistory = this.session.get(user.waId(), SessionConstant.SESSION_MESSAGE_HISTORY_KEY, Set.class);
            queue = new HashSet<String>(qHistory);
        }
        return queue;
    }

    void addToMessageQueue(WaUser user) {
        Set<String> queue = getMessageQueue(user);
        queue.add(user.msgId());
        if(queue.size() > EngineConstant.MESSAGE_QUEUE_COUNT) {
            log.warn("Message queue limit reached, applying FIFO..");
            Iterator<String> iterator = queue.iterator();
            int count = 0;
            while (iterator.hasNext() && count < queue.size() - 10) {
                iterator.next();
                iterator.remove();
                count++;
            }
        }
        this.session.save(user.waId(), SessionConstant.SESSION_MESSAGE_HISTORY_KEY, queue);
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
        var userOpt = WhatsappUtils.getUser(webhookPayload, this.jawceConfig.getWebhookTimestampThresholdSecs());
        if(userOpt.isEmpty()) return Optional.empty();

        var user = userOpt.get();
        var sessionId = user.waId();
        var message = WhatsappUtils.extractMessage(webhookPayload);
        var messageType = WhatsappUtils.isValidSupportedMessageType(message);
        this.session = this.session.session(sessionId);

        if(this.jawceConfig.isHandleSessionQueue()) {
            if(this.getMessageQueue(user).contains(user.msgId())) {
                log.warn("Duplicate message found: {}. Skipping..", message);
                return Optional.empty();
            }
        }

        Long lastDebounceTimestamp = this.session.get(sessionId, SessionConstant.CURRENT_DEBOUNCE_KEY, Long.class);
        long currentTime = System.currentTimeMillis();

        if(lastDebounceTimestamp == null || currentTime - lastDebounceTimestamp >= this.jawceConfig.getDebounceTimeoutMs()) {
            this.session.save(sessionId, SessionConstant.CURRENT_DEBOUNCE_KEY, currentTime);
        } else {
            log.warn("Message ignored due to debounce..");
            return Optional.empty();
        }

        if(this.jawceConfig.isHandleSessionQueue()) {
            this.addToMessageQueue(user);
        }

        return Optional.of(new Webhook(user, messageType, message));
    }

    @SneakyThrows
    public void sendQuickButtonMessage(QuickBtnTemplate button) {
        var btn = new ButtonTemplate();
        btn.setReplyMessageId(button.getMessageId());

        var btnMsg = new ButtonMessage();
        btnMsg.setButtons(button.getButtons());
        btnMsg.setBody(button.getMessage());
        btnMsg.setFooter(button.getFooter());
        btnMsg.setTitle(button.getTitle());

        btn.setMessage(btnMsg);

        var hook = new Hook();
        hook.setSessionId(button.getRecipient());
        hook.setWaUser(new WaUser(button.getRecipient(), button.getRecipient(), null, null));

        var messageRequest = new PayloadGeneratorDto(
                btn,
                hook,
                null,
                null
        );

        var payload = new PayloadGenerator(messageRequest).generate();
        var resultPayload = new WebhookProcessorResult(payload, null, button.getRecipient(), false);
        this.service.sendWhatsAppRequest(resultPayload);
    }

    public int verifyHubToken(String mode, String challenge, String token) {
        if("subscribe".equals(mode) && token.equals(this.waConfig.getHubToken())) return Integer.parseInt(challenge);
        throw new RuntimeException("Challenge failed, invalid hub token!");
    }

    public void processWebhook(Map<String, Object> webhookPayload) {
        var webhook = this.initChecks(webhookPayload);
        if(webhook.isEmpty()) return;
        this.fireGlobalHook(webhook.get().user().waId());

        try {
            var result = this.webhookProcessor.process(webhook.get());
            this.service.sendWhatsAppRequest(result);
            session.save(webhook.get().user().waId(), SessionConstant.CURRENT_MSG_ID_KEY, webhook.get().user().msgId());
        } catch (HookException e) {
            log.error("Hook processing failed: {}", e.getMessage());

            this.sendQuickButtonMessage(
                    QuickBtnTemplate.builder()
                            .title("Message")
                            .buttons(List.of("Retry"))
                            .message(e.getMessage())
                            .build()
            );
        } catch (TemplateRenderException e) {
            log.error("Template render failed: {}", e.getMessage());

            this.sendQuickButtonMessage(
                    QuickBtnTemplate.builder()
                            .title("Message")
                            .buttons(List.of("Retry", "Report"))
                            .message("Failed to process your message")
                            .build()
            );
        } catch (ResponseException e) {
            log.error("Engine response exception: {}", e.getError());

            this.sendQuickButtonMessage(
                    QuickBtnTemplate.builder()
                            .title("Message")
                            .buttons(List.of("Menu", "Report"))
                            .message("%s.\n\n%s".formatted(e.getError().message(), "You may click the button to return to Menu"))
                            .build()
            );
        } catch (UserSessionValidationException e) {
            log.error("User session validation failed: {}", e.getMessage());

            this.sendQuickButtonMessage(
                    QuickBtnTemplate.builder()
                            .title("Message")
                            .buttons(List.of("Menu"))
                            .message("Could not process request\n\n_AMB Err_")
                            .build()
            );

        } catch (SessionExpiredException | SessionInactivityException e) {
            log.error("Session expired / inactive, clearing user session..");

            session.clear(webhook.get().user().waId());

            this.sendQuickButtonMessage(
                    QuickBtnTemplate.builder()
                            .title("Security Check 🔐")
                            .footer("Session Expired")
                            .buttons(List.of("Menu"))
                            .message(e.getMessage())
                            .build()
            );
        } catch (Exception e) {
            log.error("Engine failed to process webhook: {}", e.getMessage(), e);

            this.sendQuickButtonMessage(
                    QuickBtnTemplate.builder()
                            .title("Message")
                            .buttons(List.of("Menu", "Report"))
                            .message("Something went wrong. Please try again later.")
                            .build()
            );
        } finally {
            MDC.remove(EngineConstant.MDC_WA_ID_KEY);
            MDC.remove(EngineConstant.MDC_WA_NAME_KEY);
        }
    }

    @EventListener
    public void handleWebhookEvent(WebhookEvent event) {
        this.processWebhook(event.getPayload());
    }
}
