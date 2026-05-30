package zw.co.dcl.jawce.engine.support;

import zw.co.dcl.jawce.engine.api.utils.SerializeUtils;
import zw.co.dcl.jawce.engine.model.core.Hook;
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
        List<String> accounts = hook.getSession().getGlobal("accounts", List.class);
        List<String> accountList = accounts == null ? List.of() : accounts;

        Map<String, Object> templateMap;

        if (accountList.size() <= 3) {
            templateMap = Map.of(
                    "type", "button",
                    "message", Map.of(
                            "body", "Choose an account",
                            "buttons", accountList
                    )
            );
        } else if (accountList.size() <= 10) {
            Map<String, Object> rows = new LinkedHashMap<>();
            for (int i = 0; i < accountList.size(); i++) {
                String account = accountList.get(i);
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
                            "body", "Choose an account",
                            "button", "Accounts",
                            "sections", Map.of("Accounts", rows)
                    )
            );
        } else {
            List<String> lines = new ArrayList<>();
            lines.add("Choose an account");
            for (int i = 0; i < accountList.size(); i++) {
                lines.add((i + 1) + ". " + accountList.get(i));
            }

            templateMap = Map.of(
                    "type", "text",
                    "message", String.join("\n", lines)
            );
        }

        hook.setTemplateDynamicBody(
                TemplateDynamicBody.builder()
                        .template(SerializeUtils.toTemplate(templateMap))
                        .build()
        );
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
