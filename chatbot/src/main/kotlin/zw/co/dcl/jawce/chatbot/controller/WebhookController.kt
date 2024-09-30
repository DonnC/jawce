package zw.co.dcl.jawce.chatbot.controller

import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import zw.co.dcl.jawce.chatbot.service.WebhookService

@RestController
@RequestMapping("/webhook")
class WebhookController(private val webhookService: WebhookService) {
    @GetMapping
    @ResponseBody
    private fun verifyHubToken(
        @RequestParam("hub.mode") mode: String,
        @RequestParam("hub.challenge") challenge: String,
        @RequestParam("hub.verify_token") token: String
    ): ResponseEntity<Any> {
        return webhookService.verifyToken(
            mode = mode,
            challenge = challenge,
            token = token
        )
    }

    @PostMapping
    private fun handleWebhook(
        @RequestBody payload: Map<String, Any>,
        request: HttpServletRequest
    ): ResponseEntity<Any> {
        return webhookService.processRequest(payload = payload, request = request)
    }

    @DeleteMapping("/api/{userSessionId}")
    private fun resetUserSession(@PathVariable("userSessionId") userSessionId: String): ResponseEntity<String> {
        webhookService.clearUserSession(userSessionId)
        return ResponseEntity<String>.ok("session cleared")
    }
}
