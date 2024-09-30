package zw.co.dcl.jawce.chatbot.service

import jakarta.servlet.http.HttpServletRequest
import org.springframework.context.ApplicationEventPublisher
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service

@Service
class WebhookService(
    private val serviceConfig: WebhookConfigService,
    private val eventPublisher: ApplicationEventPublisher
) {
    fun verifyToken(mode: String, challenge: String, token: String): ResponseEntity<Any> {
        if (serviceConfig.entryInstance().verifyHubToken(mode, challenge, token).equals(challenge)) {
            return ResponseEntity.ok(Integer.parseInt(challenge));
        }
        return ResponseEntity.badRequest().build();
    }

    fun processRequest(payload: Map<String, Any>, request: HttpServletRequest): ResponseEntity<Any> {
        eventPublisher.publishEvent(WebhookEvent(this, payload, request))
        return ResponseEntity.ok("ACK")
    }

    fun clearUserSession(userId: String) {
        serviceConfig.clearSession(userId)
    }
}
