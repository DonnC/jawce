package zw.co.dcl.jawce.engine.model.abs;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Data;
import lombok.NoArgsConstructor;
import zw.co.dcl.jawce.engine.constants.TemplateType;
import zw.co.dcl.jawce.engine.internal.mappers.EngineRouteDeserializer;
import zw.co.dcl.jawce.engine.internal.mappers.EngineRouteMapSerializer;
import zw.co.dcl.jawce.engine.model.core.EngineRoute;
import zw.co.dcl.jawce.engine.model.template.*;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type",
        visible = true
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = TextTemplate.class, name = TemplateType.TEXT),
        @JsonSubTypes.Type(value = ButtonTemplate.class, name = TemplateType.BUTTON),
        @JsonSubTypes.Type(value = CtaTemplate.class, name = TemplateType.CTA_BUTTON),
        @JsonSubTypes.Type(value = DynamicTemplate.class, name = TemplateType.DYNAMIC),
        @JsonSubTypes.Type(value = FlowTemplate.class, name = TemplateType.FLOW),
        @JsonSubTypes.Type(value = LocationTemplate.class, name = TemplateType.LOCATION),
        @JsonSubTypes.Type(value = MediaTemplate.class, name = TemplateType.MEDIA),
        @JsonSubTypes.Type(value = RequestLocationTemplate.class, name = TemplateType.REQUEST_LOCATION),
        @JsonSubTypes.Type(value = TemplateTemplate.class, name = TemplateType.TEMPLATE)
})
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class BaseEngineTemplate implements Serializable {
    @JsonProperty("type")
    private String type;

    @JsonSerialize(using = EngineRouteMapSerializer.class)
    @JsonDeserialize(using = EngineRouteDeserializer.class)
    private List<EngineRoute> routes;

    // attr
    @JsonProperty("ack")
    private boolean acknowledged = false;
    private boolean authenticated = false;
    private boolean checkpoint = false;
    private boolean typing = false;
    private String prop;
    private boolean session = true;
    @JsonProperty("transient")
    private boolean isTransient = false;
    @JsonProperty("message-id")
    private String replyMessageId;

    // hooks
    private String template;
    @JsonProperty("on-receive")
    private String onReceive;
    @JsonProperty("on-generate")
    private String onGenerate;
    private String router;
    private String middleware;

    private Map params = new HashMap<>();
}
