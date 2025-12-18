package org.dcl.jawce.server.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.dcl.jawce.server.constant.MessageType;

import java.io.Serializable;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class WebSocketMessage implements Serializable {
    private MessageType type;
    private WebSocketPayload payload;
}
