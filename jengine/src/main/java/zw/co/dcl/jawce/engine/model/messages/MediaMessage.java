package zw.co.dcl.jawce.engine.model.messages;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.SuperBuilder;
import zw.co.dcl.jawce.engine.model.abs.BaseInteractiveMessage;


@EqualsAndHashCode(callSuper = true)
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class MediaMessage extends BaseInteractiveMessage {
    @JsonProperty("type")
    private String type;
    @JsonProperty("media-id")
    private String mediaId;
    private String url;
    private String caption;
    private String filename;
}
