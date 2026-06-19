package zw.co.dcl.jawce.engine.api.pagination;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import zw.co.dcl.jawce.engine.model.core.Hook;
import zw.co.dcl.jawce.engine.model.dto.DynamicChoice;
import zw.co.dcl.jawce.engine.model.dto.TemplateDynamicBody;
import zw.co.dcl.jawce.engine.support.EngineTestSupport;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaginationSupportTest {
    private EngineTestSupport.InMemorySessionManager sessionManager;
    private Hook hook;

    @BeforeEach
    void setUp() {
        this.sessionManager = new EngineTestSupport.InMemorySessionManager();
        this.hook = Hook.builder()
                .sessionId("263771234567")
                .session(sessionManager)
                .build();
    }

    @Test
    void listPaginationReservesNavigationSlotsAndRendersFirstPage() {
        TemplateDynamicBody body = PaginationSupport.render(hook, PaginationRequest.builder()
                .stateKey("accounts")
                .mode(PaginationMode.LIST)
                .pageSize(10)
                .prompt("Select an account")
                .buttonLabel("Choose")
                .sectionTitle("Accounts")
                .choices(sampleChoices(23))
                .build());

        Map<String, Object> template = zw.co.dcl.jawce.engine.api.utils.SerializeUtils.fromTemplate(body.getTemplate());
        Map<String, Object> message = childMap(template, "message");
        Map<String, Object> sections = childMap(message, "sections");
        Map<String, Object> rows = childMap(sections, "Accounts");

        assertEquals(9, rows.size());
        assertTrue(rows.containsKey("acc-1"));
        assertTrue(rows.containsKey("accounts::NEXT::1"));
        assertEquals(9, body.getDynamicChoices().size());
    }

    @Test
    void textPaginationUsesLocalOrdinalsPerPage() {
        TemplateDynamicBody firstPage = PaginationSupport.render(hook, PaginationRequest.builder()
                .stateKey("accounts")
                .mode(PaginationMode.TEXT)
                .pageSize(10)
                .prompt("Select an account")
                .choices(sampleChoices(23))
                .build());

        assertEquals(1, firstPage.getDynamicChoices().get(0).getOrdinal());
        assertEquals(10, firstPage.getDynamicChoices().stream().filter(choice -> choice.getMetadata().get("_jawcePagination") == null).count());

        hook.setAdditionalData(Map.of("dynamicChoice", Map.of(
                "id", "accounts::NEXT::1",
                "label", "Next",
                "metadata", Map.of("_jawcePagination", Map.of(
                        "action", "NEXT",
                        "stateKey", "accounts",
                        "targetPage", 1
                ))
        )));
        PaginationSupport.handleSelection(hook);

        TemplateDynamicBody secondPage = PaginationSupport.render(hook, PaginationRequest.builder()
                .stateKey("accounts")
                .mode(PaginationMode.TEXT)
                .pageSize(10)
                .prompt("Select an account")
                .choices(sampleChoices(23))
                .build());

        DynamicChoice firstVisible = secondPage.getDynamicChoices().stream()
                .filter(choice -> choice.getMetadata().get("_jawcePagination") == null)
                .findFirst()
                .orElseThrow();

        assertEquals(1, firstVisible.getOrdinal());
        assertEquals("acc-11", firstVisible.getId());
    }

    private List<DynamicChoice> sampleChoices(int size) {
        var items = new java.util.ArrayList<Map<String, Object>>();
        for (int i = 1; i <= size; i++) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", "acc-" + i);
            row.put("label", "Account " + i);
            row.put("description", "Description " + i);
            items.add(row);
        }

        return PaginationSupport.mapChoices(
                items,
                item -> item.get("id").toString(),
                item -> item.get("label").toString(),
                item -> item.get("description").toString(),
                (item, ordinal) -> Map.of("accountId", item.get("id"))
        );
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> childMap(Map<String, Object> source, String key) {
        return (Map<String, Object>) source.get(key);
    }
}
