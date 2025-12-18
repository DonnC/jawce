package org.dcl.jawce.server;

import lombok.Getter;
import lombok.ToString;
import org.springframework.context.ApplicationEvent;
import zw.co.dcl.jawce.engine.model.core.WaUser;
import zw.co.dcl.jawce.engine.model.dto.ResponseStructure;

/**
 * An event send when a new live support message is received from
 *
 * WhatsApp channel
 */
@Getter
@ToString
public class OnWhatsAppMessageEvent extends ApplicationEvent {
    private final WaUser user;
    private final ResponseStructure response;
    private final Long chatId;

    public OnWhatsAppMessageEvent(Object source, WaUser user, ResponseStructure response, Long chatId) {
        super(source);
        this.user = user;
        this.response = response;
        this.chatId = chatId;
    }
}
