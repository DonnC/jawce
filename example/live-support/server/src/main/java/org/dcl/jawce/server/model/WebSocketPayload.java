package org.dcl.jawce.server.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class WebSocketPayload implements Serializable {
    private Long chatId;
    private String agentId;
    private String message;
}
