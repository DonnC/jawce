package zw.co.dcl.jawce.engine.support;

import zw.co.dcl.jawce.engine.api.pagination.PaginationRequest;
import zw.co.dcl.jawce.engine.api.pagination.PaginationSelection;
import zw.co.dcl.jawce.engine.api.pagination.PaginationSupport;
import zw.co.dcl.jawce.engine.api.pagination.PaginationMode;
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

    @SuppressWarnings("unchecked")
    public Hook renderPaginatedAccountsList(Hook hook) {
        List<Map<String, Object>> rawAccounts = hook.getSession().getGlobal("pagedAccounts", List.class);
        List<Map<String, Object>> accounts = rawAccounts == null ? List.of() : rawAccounts;

        var request = PaginationRequest.builder()
                .stateKey("accounts")
                .mode(PaginationMode.LIST)
                .pageSize(10)
                .title("Accounts")
                .prompt("Select an account")
                .buttonLabel("Choose")
                .sectionTitle("Account options")
                .choices(PaginationSupport.mapChoices(
                        accounts,
                        account -> account.get("id").toString(),
                        account -> account.get("label").toString(),
                        account -> account.get("description").toString(),
                        (account, ordinal) -> Map.of("accountId", account.get("id"), "accountLabel", account.get("label"))
                ))
                .build();

        hook.setTemplateDynamicBody(PaginationSupport.render(hook, request));
        return hook;
    }

    @SuppressWarnings("unchecked")
    public Hook renderPaginatedAccountsText(Hook hook) {
        List<Map<String, Object>> rawAccounts = hook.getSession().getGlobal("pagedAccounts", List.class);
        List<Map<String, Object>> accounts = rawAccounts == null ? List.of() : rawAccounts;

        var request = PaginationRequest.builder()
                .stateKey("accounts")
                .mode(PaginationMode.TEXT)
                .pageSize(10)
                .prompt("Select an account")
                .choices(PaginationSupport.mapChoices(
                        accounts,
                        account -> account.get("id").toString(),
                        account -> account.get("label").toString(),
                        account -> null,
                        (account, ordinal) -> Map.of("accountId", account.get("id"), "accountLabel", account.get("label"))
                ))
                .build();

        hook.setTemplateDynamicBody(PaginationSupport.render(hook, request));
        return hook;
    }

    public Hook capturePaginatedAccountSelection(Hook hook) {
        var selection = PaginationSupport.handleSelection(hook);
        selection.ifPresent(value -> hook.getSession().saveGlobal("paginationSelection", SerializeUtils.toMap(value)));

        if(selection.isPresent() && selection.get().isItem() && hook.getAdditionalData() != null && hook.getAdditionalData().containsKey("dynamicChoice")) {
            hook.getSession().saveGlobal("selectedDynamicChoice", hook.getAdditionalData().get("dynamicChoice"));
        }

        return hook;
    }

    public Hook routePaginatedAccountSelection(Hook hook) {
        PaginationSelection selection = PaginationSupport.selection(hook).orElse(null);
        if(selection != null && selection.isNavigation()) {
            Object rerenderStage = hook.getParams() == null ? null : hook.getParams().get("rerenderStage");
            hook.setRedirectTo(rerenderStage == null ? "START-MENU" : rerenderStage.toString());
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
