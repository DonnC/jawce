package zw.co.dcl.jawce.engine.model.messages;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import zw.co.dcl.jawce.engine.model.abs.BaseInteractiveMessage;


@EqualsAndHashCode(callSuper = true)
@Data
public class MediaMessage extends BaseInteractiveMessage {
    @JsonProperty("type")
    private String type;
    @JsonProperty("media-id")
    private String mediaId;
    private String url;
    private String caption;
    private String filename;
}
