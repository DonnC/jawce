package zw.co.dcl.jawce.engine.support;

import zw.co.dcl.jawce.engine.api.utils.SerializeUtils;
import zw.co.dcl.jawce.engine.model.core.Hook;
import zw.co.dcl.jawce.engine.model.dto.DynamicChoice;
import zw.co.dcl.jawce.engine.model.dto.TemplateDynamicBody;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TestHooks {
    public Hook startOnReceive(Hook hook) {
        return appendEvent(hook, "on_receive");
    }

    public Hook nextOnGenerate(Hook hook) {
        return appendEvent(hook, "on_generate");
    }

    public Hook prepareDynamicAccountSelector(Hook hook) {
        hook.getSession().saveGlobal("dynamicPrompt", "Select a payment account");
        return appendEvent(hook, "dynamic_prepare");
    }

    public Hook renderName(Hook hook) {
        hook.setTemplateDynamicBody(
                TemplateDynamicBody.builder()
                        .renderPayload(new HashMap<>(java.util.Map.of("name", "TDD")))
                        .build()
        );
        return hook;
    }

    public Hook routeToReport(Hook hook) {
        hook.setRedirectTo("REPORT");
        return hook;
    }

    @SuppressWarnings("unchecked")
    public Hook renderDynamicAccountSelector(Hook hook) {
        appendEvent(hook, "dynamic_render");
        List<String> accounts = hook.getSession().getGlobal("accounts", List.class);
        List<String> accountList = accounts == null ? List.of() : accounts;
        String prompt = hook.getSession().getGlobal("dynamicPrompt", String.class);
        String body = prompt == null || prompt.isBlank() ? "Choose an account" : prompt;

        Map<String, Object> templateMap;
        List<DynamicChoice> dynamicChoices = new ArrayList<>();

        if (accountList.size() <= 3) {
            templateMap = Map.of(
                    "type", "button",
                    "message", Map.of(
                            "body", body,
                            "buttons", accountList
                    )
            );
        } else if (accountList.size() <= 10) {
            Map<String, Object> rows = new LinkedHashMap<>();
            for (int i = 0; i < accountList.size(); i++) {
                String account = accountList.get(i);
                dynamicChoices.add(DynamicChoice.builder()
                        .id("acc-" + (i + 1))
                        .label(account)
                        .description("Select " + account)
                        .ordinal(i + 1)
                        .aliases(List.of(account))
                        .metadata(Map.of("account", account))
                        .build());
                rows.put(
                        "acc-" + (i + 1),
                        Map.of(
                                "title", account,
                                "description", "Select " + account
                        )
                );
            }

            templateMap = Map.of(
                    "type", "list",
                    "message", Map.of(
                            "body", body,
                            "button", "Accounts",
                            "sections", Map.of("Accounts", rows)
                    )
            );
        } else {
            List<String> lines = new ArrayList<>();
            lines.add(body);
            for (int i = 0; i < accountList.size(); i++) {
                String account = accountList.get(i);
                int ordinal = i + 1;
                lines.add(ordinal + ". " + account);
                dynamicChoices.add(DynamicChoice.builder()
                        .id("acc-" + ordinal)
                        .label(account)
                        .ordinal(ordinal)
                        .aliases(List.of(account))
                        .metadata(Map.of("account", account))
                        .build());
            }

            templateMap = Map.of(
                    "type", "text",
                    "message", String.join("\n", lines)
            );
        }

        hook.setTemplateDynamicBody(
                TemplateDynamicBody.builder()
                        .template(SerializeUtils.toTemplate(templateMap))
                        .dynamicChoices(dynamicChoices)
                        .build()
        );
        return hook;
    }

    @SuppressWarnings("unchecked")
    public Hook captureSelectedDynamicChoice(Hook hook) {
        if(hook.getAdditionalData() != null && hook.getAdditionalData().containsKey("dynamicChoice")) {
            hook.getSession().saveGlobal("selectedDynamicChoice", hook.getAdditionalData().get("dynamicChoice"));
        }
        return hook;
    }

    private Hook appendEvent(Hook hook, String eventName) {
        List<String> events = hook.getSession().getGlobal("events", List.class);
        List<String> nextEvents = events == null ? new ArrayList<>() : new ArrayList<>(events);
        nextEvents.add(eventName);
        hook.getSession().saveGlobal("events", nextEvents);
        return hook;
    }
}
