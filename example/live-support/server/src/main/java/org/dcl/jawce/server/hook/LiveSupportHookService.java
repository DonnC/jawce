package org.dcl.jawce.server.hook;

import lombok.extern.slf4j.Slf4j;
import org.dcl.jawce.server.OnWhatsAppMessageEvent;
import org.dcl.jawce.server.constant.ChatStatus;
import org.dcl.jawce.server.constant.Constant;
import org.dcl.jawce.server.constant.SenderType;
import org.dcl.jawce.server.model.LiveModeCache;
import org.dcl.jawce.server.model.entity.Chat;
import org.dcl.jawce.server.model.entity.Message;
import org.dcl.jawce.server.repository.ChatRepository;
import org.dcl.jawce.server.repository.MessageRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import zw.co.dcl.jawce.engine.api.enums.MessageTypeEnum;
import zw.co.dcl.jawce.engine.api.utils.SerializeUtils;
import zw.co.dcl.jawce.engine.api.utils.WhatsAppUtils;
import zw.co.dcl.jawce.engine.model.core.Hook;
import zw.co.dcl.jawce.engine.model.dto.ResponseStructure;
import zw.co.dcl.jawce.engine.model.dto.TemplateDynamicBody;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Service
public class LiveSupportHookService {
    private final ChatRepository chatRepository;
    private final ApplicationEventPublisher eventPublisher;

    public LiveSupportHookService(ChatRepository chatRepository,
                                  ApplicationEventPublisher eventPublisher) {
        this.chatRepository = chatRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Enable live support mode
     *
     * @param arg: Hook passed by the engine
     * @return updated Hook
     */
    public Hook enable(Hook arg) {
        log.debug("Enable live support hook arg: {}", arg);
        String message = "Connecting you to an Agent. Please wait...";

        try {
            // TODO: check if live mode flag exists in user session
            Chat chat;

            var chatResult = chatRepository.findByCustomerPhoneAndStatus(
                    arg.getWaUser().waId(),
                    ChatStatus.ACTIVE
            );

            if(chatResult.isPresent()) {
                chat = chatResult.get();
                message = "Reconnecting you to your open live chat ticket: *" + Constant.getTicket(chat.getId()) + "* with Agent: _*" + chat.getAssignedAgent() + "*_ ...";
            } else {
                chat = chatRepository.save(Chat.builder()
                        .customerName(arg.getWaUser().name())
                        .customerPhone(arg.getWaUser().waId())
                        .sessionId(arg.getSessionId())
                        .status(ChatStatus.PENDING)
                        .createdAt(LocalDateTime.now())
                        .build());
            }

            var cacheData = LiveModeCache.builder()
                    .active(true)
                    .startedAt(Instant.now().toEpochMilli())
                    .chatId(chat.getId())
                    .build();

            // set a live mode flag in user session
            arg.getSession().save(arg.getSessionId(), Constant.LIVE_MODE_CACHE_KEY, SerializeUtils.toJsonString(cacheData));

            log.info("Live chat flag enabled!");

            // immediately show on agent dashboard
            var waResponse = new ResponseStructure(
                    MessageTypeEnum.TEXT,
                    Map.of("body", "Hello, I'm online and I need help!")
            );
            eventPublisher.publishEvent(new OnWhatsAppMessageEvent(this, arg.getWaUser(), waResponse, chat.getId()));
        } catch (Exception e) {
            log.error("Something went wrong while enabling live mode: {}", e.getMessage());
            message = "Something went wrong 😓\n\nType `menu` to return to menu";
        }

        // set template dynamic variable
        arg.setTemplateDynamicBody(
                TemplateDynamicBody.builder()
                        .renderPayload(Map.of("message", message))
                        .build()
        );

        return arg;
    }
}
