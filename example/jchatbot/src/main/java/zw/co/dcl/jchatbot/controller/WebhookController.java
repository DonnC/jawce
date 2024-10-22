package zw.co.dcl.jchatbot.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import zw.co.dcl.jchatbot.service.WebhookService;

import java.util.Map;

@RestController
@RequestMapping("/webhook")
public class WebhookController {
    @Autowired
    private WebhookService webhookService;

    @GetMapping
    @ResponseBody
    ResponseEntity<?> verifyHubToken(
            @RequestParam("hub.mode") String mode,
            @RequestParam("hub.challenge") String challenge,
            @RequestParam("hub.verify_token") String token
    ) {
        return webhookService.verifyToken(mode, token, challenge);
    }

    @PostMapping
    ResponseEntity<String> handleWebhook(
            @RequestBody Map<String, Object> payload,
            HttpServletRequest request
    ) {
        return webhookService.processRequest(payload, request);
    }
}
