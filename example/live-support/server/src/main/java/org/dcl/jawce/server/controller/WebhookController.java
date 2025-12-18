package org.dcl.jawce.server.controller;

import org.dcl.jawce.server.service.LsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import zw.co.dcl.jawce.engine.api.annotation.VerifyWhatsAppPayload;

import java.util.Map;

@RestController
@RequestMapping("/webhook")
public class WebhookController {
    private final LsService lsService;

    public WebhookController(LsService lsService) {
        this.lsService = lsService;
    }

    @GetMapping
    @ResponseBody
    ResponseEntity<?> challenge(
            @RequestParam("hub.mode") String mode,
            @RequestParam("hub.challenge") String challenge,
            @RequestParam("hub.verify_token") String token
    ) {
        return ResponseEntity.ok(lsService.verifyToken(mode, token, challenge));
    }

    @VerifyWhatsAppPayload
    @PostMapping
    ResponseEntity<String> handler(@RequestBody Map<String, Object> payload) {
        return ResponseEntity.ok(lsService.processRequest(payload));
    }
}
