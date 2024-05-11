package zw.co.dcl.engine.whatsapp.utils;

import zw.co.dcl.engine.whatsapp.constants.EngineConstants;
import zw.co.dcl.engine.whatsapp.enums.InteractivePayloadType;
import zw.co.dcl.engine.whatsapp.enums.PayloadType;
import zw.co.dcl.engine.whatsapp.exceptions.EngineInternalException;

import java.util.*;

public class ChannelPayloadGenerator {
    private final Map<String, Object> body;

    public ChannelPayloadGenerator(Map<String, Object> body) {
        this.body = body;
    }

    private String generateFlowToken(String user, String flow) {
        Random random = new Random();
        return flow.trim().toLowerCase() + "_" + user + "_" + random.nextLong(10L, 999L);
    }

    private void createSectionRows(Map<String, Object> section, List<Map> rows, LinkedHashMap<String, Object> map) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            final Map<String, String> rowMap = new HashMap<>();
            rowMap.put("id", entry.getKey());
            rowMap.putAll((Map<? extends String, ? extends String>) entry.getValue());
            rows.add(rowMap);
        }

        section.put("rows", rows);
    }

    public Map<String, Object> text() {
        Map<String, Object> textPayload = new HashMap<>(Map.of("preview_url", true));

        if (this.body.get("message") instanceof ArrayList messages)
            textPayload.put("body", String.join(System.lineSeparator(), messages));
        else
            textPayload.put("body", this.body.get("message"));

        return textPayload;
    }

    public Map<String, Object> reaction() {
        Map<String, Object> reactPayload = new HashMap<>();

        reactPayload.put("message_id", this.body.get("message_id"));
        reactPayload.put("emoji", this.body.get("emoji"));

        return reactPayload;
    }

    public Map<String, Object> template() {
//        TODO: might have different way of handling Template
//        but for now, pass templates as is since they are too dynamic
        return this.body;
    }

    public Map<String, Object> media(PayloadType type) {
        var baseBody = new HashMap<>(
                this.body.containsKey("id") ?
                        Map.of("id", this.body.get("id")) :
                        Map.of("link", this.body.get("link")
                        )
        );

        switch (type) {
            case IMAGE -> {
                if (this.body.containsKey("caption")) baseBody.put("caption", this.body.get("caption"));
            }
            case DOCUMENT -> {
                if (this.body.containsKey("caption")) baseBody.put("caption", this.body.get("caption"));
                if (this.body.containsKey("filename")) baseBody.put("filename", this.body.get("filename"));
            }
            default -> throw new EngineInternalException("unsupported media message type");
        }

        return baseBody;
    }

    public Map<String, Object> button() {
        var buttonList = (ArrayList<String>) this.body.get("buttons");

        Map<String, Object> buttonIntrPayload = new HashMap<>(
                Map.of(
                        "type", InteractivePayloadType.BUTTON.name().toLowerCase(),
                        "body", Map.of("text", body.get("body"))
                )
        );

        if (body.containsKey("title")) {
            if (body.containsKey("id") || body.containsKey("url")) {
                String mediaFormat = body.containsKey("id") ? "id" : "url";
                var mediaLocation = body.containsKey("id") ? body.get("id") : body.get("url");

                buttonIntrPayload.put(
                        "header",
                        Map.of(
                                "type", body.get("title"),
                                body.get("title"), Map.of(mediaFormat, mediaLocation)
                        )
                );
            } else
                buttonIntrPayload.put("header", Map.of("type", "text", "text", body.get("title")));
        }

        if (body.containsKey("footer")) {
            buttonIntrPayload.put("footer", Map.of("text", body.get("footer")));
        }

        List<Map> btnPayload = new ArrayList<>();

        buttonList.forEach((btn) -> btnPayload.add(
                        Map.of(
                                "type", "reply",
                                "reply", Map.of(
                                        "id", btn.toLowerCase(),
                                        "title", btn
                                )
                        )
                )
        );

        buttonIntrPayload.put("action", Map.of("buttons", btnPayload));
        return buttonIntrPayload;
    }

    public Map<String, Object> interactiveList() {
        var sections = (LinkedHashMap<String, Object>) body.get("sections");

        Map<String, Object> listIntrPayload = new HashMap<>(
                Map.of(
                        "type", InteractivePayloadType.LIST.name().toLowerCase(),
                        "body", Map.of(
                                "text", body.get("body")
                        )
                )
        );

        var listType = CommonUtils.detectListSectionType(sections);

        if (body.containsKey("title")) {
            listIntrPayload.put("header", Map.of("type", "text", "text", body.get("title")));
        }

        if (body.containsKey("footer")) {
            listIntrPayload.put("footer", Map.of("text", body.get("footer")));
        }

        Map<String, Object> actionPayload = new HashMap<>();
        actionPayload.put("button", body.get("button"));

        switch (listType) {
            case ROWS_ONLY -> {
                Map<String, Object> section = new HashMap<>();
                List<Map> rows = new ArrayList<>();

                createSectionRows(section, rows, sections);
                actionPayload.put("sections", List.of(section));
            }

            case SECTION_TITLES -> {
                List<Map> multiSections = new ArrayList<>();

                sections.forEach((key, value) -> {
                    Map<String, Object> section = new HashMap<>();
                    section.put("title", key);
                    final LinkedHashMap<String, Object> rowsMap = (LinkedHashMap<String, Object>) value;
                    List<Map> rows = new ArrayList<>();
                    createSectionRows(section, rows, rowsMap);
                    multiSections.add(section);
                });

                actionPayload.put("sections", multiSections);
            }
        }

        listIntrPayload.put("action", actionPayload);

        return listIntrPayload;
    }

    public Map<String, Object> flow(String user, Map<String, Object> flowData) {
        Map<String, Object> flowIntrPayload = new HashMap<>(
                Map.of(
                        "type", InteractivePayloadType.FLOW.name().toLowerCase(),
                        "body", Map.of(
                                "text", body.get("body")
                        )
                )
        );

        if (body.containsKey("title")) {
            flowIntrPayload.put("header", Map.of("type", "text", "text", body.get("title")));
        }

        if (body.containsKey("footer")) {
            flowIntrPayload.put("footer", Map.of("text", body.get("footer")));
        }

        Map<String, Object> actionPayload = new HashMap<>();
        actionPayload.put("name", "flow");

        Map<String, Object> params = new HashMap<>();

        if (body.containsKey("draft")) params.put("mode", "draft");

        params.put("flow_message_version", EngineConstants.CHANNEL_FLOW_VERSION);
        params.put("flow_token", this.generateFlowToken(user, body.get("name").toString()));
        params.put("flow_id", body.get("id"));
        params.put("flow_cta", body.get("button"));
        params.put("flow_action", EngineConstants.CHANNEL_SUPPORTED_FLOW_ACTION);

        Map<String, Object> flowPayload = new HashMap<>();
        flowPayload.put("screen", body.get("name"));
        if (flowData != null) flowPayload.put("data", flowData);

        params.put("flow_action_payload", flowPayload);

        actionPayload.put("parameters", params);
        flowIntrPayload.put("action", actionPayload);

        return flowIntrPayload;
    }

}
