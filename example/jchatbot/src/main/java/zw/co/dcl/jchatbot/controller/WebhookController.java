package zw.co.dcl.jchatbot.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import zw.co.dcl.jawce.engine.api.annotation.VerifyWhatsAppPayload;
import zw.co.dcl.jchatbot.service.WebhookService;

import java.util.Map;

@RestController
@RequestMapping("/webhook")
public class WebhookController {
    private final WebhookService webhookService;

    public WebhookController(WebhookService webhookService) {
        this.webhookService = webhookService;
    }

    @GetMapping
    @ResponseBody
    ResponseEntity<?> challenge(
            @RequestParam("hub.mode") String mode,
            @RequestParam("hub.challenge") String challenge,
            @RequestParam("hub.verify_token") String token
    ) {
        return ResponseEntity.ok(webhookService.verifyToken(mode, token, challenge));
    }

    @VerifyWhatsAppPayload
    @PostMapping
    ResponseEntity<String> handler(@RequestBody Map<String, Object> payload) {
        return ResponseEntity.ok(webhookService.processRequest(payload));
    }
}
