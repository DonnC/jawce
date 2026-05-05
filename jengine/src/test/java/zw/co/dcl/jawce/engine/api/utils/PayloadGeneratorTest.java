package zw.co.dcl.jawce.engine.api.utils;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import zw.co.dcl.jawce.engine.api.dto.PayloadGeneratorDto;
import zw.co.dcl.jawce.engine.api.iface.ISessionManager;
import zw.co.dcl.jawce.engine.constants.EngineConstant;
import zw.co.dcl.jawce.engine.internal.service.HookService;
import zw.co.dcl.jawce.engine.model.core.Hook;
import zw.co.dcl.jawce.engine.model.core.WaUser;
import zw.co.dcl.jawce.engine.model.dto.TemplateDynamicBody;
import zw.co.dcl.jawce.engine.model.messages.LocationMessage;
import zw.co.dcl.jawce.engine.model.messages.TemplateMessage;
import zw.co.dcl.jawce.engine.model.template.LocationTemplate;
import zw.co.dcl.jawce.engine.model.template.TemplateTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PayloadGeneratorTest {
    @Test
    void locationTemplateRendersLocationPayload() {
        Hook hook = Hook.builder()
                .waUser(new WaUser("Test User", "263771234567", "wamid-1", 1L))
                .build();

        LocationTemplate template = LocationTemplate.builder()
                .message(LocationMessage.builder()
                        .lat("-17.8252")
                        .lon("31.0335")
                        .name("Harare")
                        .address("Harare CBD")
                        .build())
                .build();

        PayloadGenerator generator = new PayloadGenerator(
                new PayloadGeneratorDto(template, hook, "LOCATION", null, false)
        );

        Map<String, Object> payload = generator.generate();

        assertEquals("location", payload.get("type"));
        Map<String, Object> location = (Map<String, Object>) payload.get("location");
        assertEquals("-17.8252", location.get("latitude"));
        assertEquals("31.0335", location.get("longitude"));
        assertEquals("Harare", location.get("name"));
        assertEquals("Harare CBD", location.get("address"));
    }

    @Test
    void whatsappTemplateRendersTemplatePayloadAndComponents() throws Exception {
        ISessionManager sessionManager = Mockito.mock(ISessionManager.class);
        HookService hookService = Mockito.mock(HookService.class);

        Hook hookResult = Hook.builder()
                .templateDynamicBody(TemplateDynamicBody.builder()
                        .renderPayload(Map.of(
                                EngineConstant.WHATSAPP_TEMPLATE_KEY,
                                List.of(Map.of(
                                        "type", "body",
                                        "parameters", List.of(Map.of("type", "text", "text", "Donald"))
                                ))
                        ))
                        .build())
                .build();

        Mockito.when(hookService.processHook(Mockito.any(Hook.class))).thenReturn(hookResult);

        Hook hook = Hook.builder()
                .waUser(new WaUser("Test User", "263771234567", "wamid-2", 1L))
                .session(sessionManager)
                .build();

        TemplateTemplate template = TemplateTemplate.builder()
                .template("example.hooks.TemplateHook.render")
                .message(TemplateMessage.builder()
                        .name("account_update")
                        .language("en_US")
                        .build())
                .build();

        PayloadGenerator generator = new PayloadGenerator(
                new PayloadGeneratorDto(template, hook, "TPL", hookService, false)
        );

        Map<String, Object> payload = generator.generate();

        assertEquals("template", payload.get("type"));
        Map<String, Object> templatePayload = (Map<String, Object>) payload.get("template");
        assertEquals("account_update", templatePayload.get("name"));
        assertEquals("en_US", ((Map<?, ?>) templatePayload.get("language")).get("code"));
        assertTrue(((List<?>) templatePayload.get("components")).size() == 1);
    }
}
