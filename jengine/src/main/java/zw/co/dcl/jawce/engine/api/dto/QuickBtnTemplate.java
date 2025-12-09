package zw.co.dcl.jawce.engine.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class QuickBtnTemplate {
    String recipient;
    String message;
    String footer;
    String title;
    List<String> buttons;
    String messageId;
}
