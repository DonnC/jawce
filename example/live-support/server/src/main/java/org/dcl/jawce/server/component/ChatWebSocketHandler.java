package org.dcl.jawce.server.component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.dcl.jawce.server.OnWhatsAppMessageEvent;
import org.dcl.jawce.server.constant.ChatStatus;
import org.dcl.jawce.server.constant.Constant;
import org.dcl.jawce.server.constant.SenderType;
import org.dcl.jawce.server.model.LiveModeCache;
import org.dcl.jawce.server.model.WebSocketMessage;
import org.dcl.jawce.server.model.WebSocketPayload;
import org.dcl.jawce.server.model.entity.Chat;
import org.dcl.jawce.server.model.entity.Message;
import org.dcl.jawce.server.repository.ChatRepository;
import org.dcl.jawce.server.repository.MessageRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import zw.co.dcl.jawce.engine.api.iface.ISessionManager;
import zw.co.dcl.jawce.engine.api.utils.SerializeUtils;
import zw.co.dcl.jawce.engine.internal.events.OnceOffMessageEvent;
import zw.co.dcl.jawce.engine.model.core.WaUser;
import zw.co.dcl.jawce.engine.model.template.TextTemplate;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final ObjectMapper mapper = new ObjectMapper()
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            .registerModule(new JavaTimeModule())
            .findAndRegisterModules();

    private final ChatRepository chatRepository;
    private final MessageRepository messageRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ISessionManager sessionManager;

    public ChatWebSocketHandler(ChatRepository chatRepository, MessageRepository messageRepository,
                                ApplicationEventPublisher eventPublisher, ISessionManager sessionManager) {
        this.chatRepository = chatRepository;
        this.messageRepository = messageRepository;
        this.eventPublisher = eventPublisher;
        this.sessionManager = sessionManager;
    }

    private TextMessage createChatTextMessage(Object chat, String channel) {
        try {
            JsonNode payload = mapper.valueToTree(chat);

            ObjectNode wrapper = mapper.createObjectNode();
            wrapper.put("type", channel);
            wrapper.set("payload", payload);

            String json = mapper.writeValueAsString(wrapper);
            return new TextMessage(json);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize chat for websocket", e);
            return new TextMessage("{\"type\":\"error\",\"message\":\"serialization_failed\"}");
        }
    }

    private Chat getChat(Long chatId) {
        return chatRepository
                .findById(chatId)
                .orElseThrow(() -> new RuntimeException("Chat with id " + chatId + " not found"));
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.put(session.getId(), session);
        log.info("Agent connected: {}", session.getId());

        // Send all non-closed chats to the agent
        List<Chat> chats = chatRepository.findByStatusNot(ChatStatus.CLOSED);
        log.info("Active chats: {}", chats.size());
        sendMessage(session, createChatTextMessage(chats, Constant.CHANNEL_CHAT_LIST));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        log.info("Received web request: {}", message.getPayload());

        Map<String, Object> rawMessage = mapper.readValue(message.getPayload(), Map.class);
        WebSocketMessage socketMessage = SerializeUtils.castValue(rawMessage, WebSocketMessage.class);

        Chat chat = getChat(socketMessage.getPayload().getChatId());
        String sessionId = chat.getSessionId();

        String cacheString = this.sessionManager.session(sessionId).get(sessionId, Constant.LIVE_MODE_CACHE_KEY, String.class);

        if(cacheString == null) throw new RuntimeException("No live mode cache found for sessionId: " + sessionId);

        LiveModeCache liveModeCache = SerializeUtils.castValue(cacheString, LiveModeCache.class);

        if(!liveModeCache.getActive()) {
            log.warn("Live mode cache is not active for sessionId: {}. Closing..", sessionId);
            handleCloseChat(chat);
            return;
        }

        switch (socketMessage.getType()) {
            case CLAIM_CHAT -> handleClaimChat(chat, socketMessage.getPayload());
            case CLOSE_CHAT -> handleCloseChat(chat);
            case SEND_MESSAGE -> handleSendMessage(chat, socketMessage.getPayload());
            default -> log.warn("Unknown message type: {}", socketMessage.getType());
        }
    }

    private void handleClaimChat(Chat chat, WebSocketPayload payload) {
        chat.setStatus(ChatStatus.ACTIVE);
        chat.setAssignedAgent(payload.getAgentId());
        chatRepository.save(chat);

        broadcast(createChatTextMessage(chat, Constant.CHANNEL_CHAT_UPDATE));

        String responseMessage = "> A new support ticket with id:" + Constant.getTicket(chat.getId()) +
                " has been created." +
                "\n" +
                "> You are now chatting with Agent: " +
                payload.getAgentId() +
                "\n\n" +
                "How may I help you today *" + chat.getCustomerName() + "*?";

        this.sendWhatsAppResponse(chat, responseMessage);

        log.info("Chat: {} claimed by agent: {}", chat.getId(), payload.getAgentId());
    }

    private void handleCloseChat(Chat chat) {
        String sessionId = chat.getSessionId();

        chat.setStatus(ChatStatus.CLOSED);
        chatRepository.save(chat);

        broadcast(createChatTextMessage(chat, Constant.CHANNEL_CHAT_UPDATE));
        log.info("Chat {} closed", chat.getId());

        // clear session key, automated bot will handle the next user message
        sessionManager.session(sessionId).evict(sessionId, Constant.LIVE_MODE_CACHE_KEY);

        String closeMessage = "This support chat has been *closed*.\n\nYou are now reconnected to the automated assistant.";
        this.sendWhatsAppResponse(chat, closeMessage);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session.getId());
        log.info("Agent disconnected: {}", session.getId());
    }


    private void handleSendMessage(Chat chat, WebSocketPayload payload) {
        // Update chat's last message
        chat.setLastMessage(payload.getMessage());
        chat.setLastMessageTime(LocalDateTime.now());
        chatRepository.save(chat);

        var msg = messageRepository.save(Message.builder()
                .chat(chat)
                .content(payload.getMessage())
                .sender(SenderType.agent)
                .timestamp(LocalDateTime.now())
                .build());

        broadcast(createChatTextMessage(chat, Constant.CHANNEL_CHAT_UPDATE));
        broadcast(createChatTextMessage(msg, Constant.CHANNEL_MESSAGE));

        log.info("Message sent in chat {}: {}", chat.getId(), payload.getMessage());

        this.sendWhatsAppResponse(chat, payload.getMessage());
    }

    private void broadcast(TextMessage message) {
        sessions.values().forEach(session -> sendMessage(session, message));
    }

    private void sendMessage(WebSocketSession session, TextMessage message) {
        try {
            if(session.isOpen()) {
                session.sendMessage(message);
            }
        } catch (IOException e) {
            log.error("Error sending message", e);
        }
    }

    private void sendWhatsAppResponse(Chat chat, String message) {
        var waUser = new WaUser(chat.getCustomerName(), chat.getCustomerPhone(), null, null);
        var textTemplate = TextTemplate
                .builder()
                .message(message)
                .build();
        eventPublisher.publishEvent(new OnceOffMessageEvent(this, waUser, textTemplate));
    }

    @EventListener
    public void onWhatsAppMessageReceived(OnWhatsAppMessageEvent event) {
        log.info("Received onWhatsAppMessageEvent: {}", event);

        Chat chat = getChat(event.getChatId());

        // TODO: assume text messages only
        String messageContent = (String) event.getResponse().body().get("body");

        Message msg = Message.builder()
                .chat(chat)
                .content(messageContent)
                .sender(SenderType.user)
                .timestamp(LocalDateTime.now())
                .build();
        messageRepository.save(msg);

        chat.setLastMessage(messageContent);
        chat.setLastMessageTime(LocalDateTime.now());
        chatRepository.save(chat);

        broadcast(createChatTextMessage(msg, Constant.CHANNEL_MESSAGE));
        broadcast(createChatTextMessage(chat, Constant.CHANNEL_CHAT_UPDATE));
    }
}
