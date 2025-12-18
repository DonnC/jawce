package org.dcl.jawce.server.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LiveModeCache implements Serializable {
    private Boolean active;
    private Long startedAt;
    private Long chatId;
}
