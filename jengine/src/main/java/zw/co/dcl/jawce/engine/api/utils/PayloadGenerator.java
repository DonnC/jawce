package zw.co.dcl.jawce.engine.api.utils;

import lombok.extern.slf4j.Slf4j;
import zw.co.dcl.jawce.engine.api.dto.PayloadGeneratorDto;
import zw.co.dcl.jawce.engine.api.enums.InteractivePayloadType;
import zw.co.dcl.jawce.engine.api.enums.PayloadType;
import zw.co.dcl.jawce.engine.api.exceptions.InternalException;
import zw.co.dcl.jawce.engine.constants.EngineConstant;
import zw.co.dcl.jawce.engine.constants.TemplateType;
import zw.co.dcl.jawce.engine.internal.abstracts.BasePayloadGenerator;
import zw.co.dcl.jawce.engine.model.template.*;

import java.util.*;

@Slf4j
public class PayloadGenerator extends BasePayloadGenerator {
    public PayloadGenerator(PayloadGeneratorDto dto) {
        super(dto);
    }

    String generateFlowToken(String user, String flow) {
        Random random = new Random();
        return flow.trim().toLowerCase() + "_" + user + "_" + random.nextLong(10L, 999L);
    }

    void createSectionRows(Map<String, Object> section, List<Map> rows, LinkedHashMap<String, Object> map) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            final Map<String, String> rowMap = new HashMap<>();
            rowMap.put("id", entry.getKey());
            rowMap.putAll((Map<? extends String, ? extends String>) entry.getValue());
            rows.add(rowMap);
        }

        section.put("rows", rows);
    }

    public Map<String, Object> text() {
        var payload = new HashMap<>(
                WhatsappUtils.getCommonPayload(
                        this.hookArg.getWaUser().waId(),
                        PayloadType.TEXT,
                        this.replyMessageId
                )
        );

        Map<String, Object> textPayload = new HashMap<>(Map.of("preview_url", true));

        if(this.template instanceof TextTemplate textTemplate) {
            textPayload.put("body", textTemplate.getMessage());

            payload.put(PayloadType.TEXT.name().toLowerCase(), textPayload);
            return payload;
        }

        throw new InternalException("Invalid template type");
    }

    public Map<String, Object> locationRequest() {
        var payload = new HashMap<>(WhatsappUtils.getCommonPayload(
                this.hookArg.getWaUser().waId(),
                PayloadType.INTERACTIVE,
                this.replyMessageId));


        var locReqPayload = new HashMap<>();

        if(this.template instanceof RequestLocationTemplate locTemplate) {

            locReqPayload.put("type", InteractivePayloadType.LOCATION_REQUEST_MESSAGE.name().toLowerCase());
            locReqPayload.put("body", Map.of("text", locTemplate.getMessage()));
            locReqPayload.put("action", Map.of("name", "send_location"));

            payload.put(PayloadType.INTERACTIVE.name().toLowerCase(), locReqPayload);

            return payload;
        }

        throw new InternalException("Invalid template type");
    }

    public Map<String, Object> media() {
        var payload = new HashMap<>(
                WhatsappUtils.getCommonPayload(
                        this.hookArg.getWaUser().waId(),
                        PayloadType.MEDIA,
                        this.replyMessageId
                )
        );

        if(this.template instanceof MediaTemplate mediaTemplate) {
            var message = mediaTemplate.getMessage();

            Map<String, Object> baseBody = new HashMap<>(
                    message.getMediaId() != null ?
                            Map.of("id", message.getMediaId()) :
                            Map.of("link", message.getUrl())
            );

            if(message.getCaption() != null) baseBody.put("caption", message.getCaption());
            if(message.getFilename() != null) baseBody.put("filename", message.getFilename());

            payload.put("type", mediaTemplate.getMessage().getType());
            payload.put(mediaTemplate.getMessage().getType(), baseBody);

            return payload;
        }

        throw new InternalException("Invalid template type");
    }

    public Map<String, Object> button() {
        var payload = new HashMap<>(WhatsappUtils.getCommonPayload(
                this.hookArg.getWaUser().waId(),
                PayloadType.INTERACTIVE,
                replyMessageId
        ));


        if(this.template instanceof ButtonTemplate buttonTemplate) {
            var message = buttonTemplate.getMessage();

            Map<String, Object> interactivePayload = new HashMap<>(
                    Map.of(
                            "type", InteractivePayloadType.BUTTON.name().toLowerCase(),
                            "body", Map.of("text", message.getBody())
                    )
            );

            if(message.getTitle() != null) {
                interactivePayload.put("header", Map.of("type", "text", "text", message.getTitle()));
            }

            if(message.getFooter() != null) {
                interactivePayload.put("footer", Map.of("text", message.getFooter()));
            }

            List<Map> btnPayload = new ArrayList<>();

            message.getButtons().forEach((btn) -> btnPayload.add(
                            Map.of(
                                    "type", "reply",
                                    "reply", Map.of(
                                            "id", btn,
                                            "title", btn
                                    )
                            )
                    )
            );

            interactivePayload.put("action", Map.of("buttons", btnPayload));
            payload.put("interactive", interactivePayload);
            return payload;
        }

        throw new InternalException("Invalid template type");
    }

    public Map<String, Object> flow() {
        var payload = new HashMap<>(WhatsappUtils.getCommonPayload(
                this.hookArg.getWaUser().waId(),
                PayloadType.INTERACTIVE,
                this.replyMessageId
        ));

        var flowInitialData = this.processTemplate();

        if(this.template instanceof FlowTemplate flowTemplate) {
            var message = flowTemplate.getMessage();

            Map<String, Object> interactivePayload = new HashMap<>(
                    Map.of(
                            "type", InteractivePayloadType.FLOW.name().toLowerCase(),
                            "body", Map.of(
                                    "text", message.getBody()
                            )
                    )
            );

            if(message.getTitle() != null) {
                interactivePayload.put("header", Map.of("type", "text", "text", message.getTitle()));
            }

            if(message.getFooter() != null) {
                interactivePayload.put("footer", Map.of("text", message.getFooter()));
            }

            Map<String, Object> actionPayload = new HashMap<>();
            actionPayload.put("name", "flow");

            Map<String, Object> params = new HashMap<>();

            if(message.isDraft()) params.put("mode", "draft");

            params.put("flow_message_version", EngineConstant.CHANNEL_FLOW_VERSION);
            params.put("flow_token", this.generateFlowToken(this.hookArg.getWaUser().name(), message.getName()));
            params.put("flow_id", message.getFlowId());
            params.put("flow_cta", message.getButton());
            params.put("flow_action", EngineConstant.CHANNEL_SUPPORTED_FLOW_ACTION);

            Map<String, Object> flowPayload = new HashMap<>();
            flowPayload.put("screen", message.getName());
            if(flowInitialData != null) flowPayload.put("data", flowInitialData);

            params.put("flow_action_payload", flowPayload);

            actionPayload.put("parameters", params);
            interactivePayload.put("action", actionPayload);

            payload.put("interactive", interactivePayload);
            return payload;
        }

        throw new InternalException("Invalid template type");
    }

    public Map<String, Object> generate() {
        return switch (this.dto.template().getType()) {
            case TemplateType.TEXT -> this.text();
            case TemplateType.BUTTON -> this.button();
            case TemplateType.FLOW -> this.flow();
            case TemplateType.MEDIA, TemplateType.DOCUMENT, TemplateType.IMAGE -> this.media();
            case TemplateType.REQUEST_LOCATION -> this.locationRequest();
            default ->
                    throw new InternalException("specified template type not supported for stage: " + this.stage);
        };
    }
}
