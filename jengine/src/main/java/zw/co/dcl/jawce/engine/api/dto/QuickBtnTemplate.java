package zw.co.dcl.jawce.engine.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class QuickBtnTemplate {
    String recipient;
    String message;
    String footer;
    String title;
    List<String> buttons;
    String messageId;
}
