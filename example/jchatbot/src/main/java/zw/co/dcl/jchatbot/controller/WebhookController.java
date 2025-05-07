package zw.co.dcl.jchatbot.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
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

    @PostMapping
    ResponseEntity<String> handler(@RequestBody Map<String, Object> payload, HttpServletRequest request) {
        return ResponseEntity.ok(webhookService.processRequest(payload, request));
    }
}
