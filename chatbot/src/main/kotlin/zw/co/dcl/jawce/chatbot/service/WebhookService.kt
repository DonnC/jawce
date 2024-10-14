package zw.co.dcl.jawce.chatbot.service

import jakarta.servlet.http.HttpServletRequest
import org.springframework.context.ApplicationEventPublisher
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.util.Assert
import zw.co.dcl.jawce.chatbot.SessionAction
import zw.co.dcl.jawce.chatbot.SessionRequest
import zw.co.dcl.jawce.session.ISessionManager

@Service
class WebhookService(
    private val serviceConfig: WebhookConfigService,
    private val sessionManager: ISessionManager,
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

    fun handleUserSessionRequest(request: SessionRequest): ResponseEntity<Any> {
        Assert.notNull(request.sessionId, "session id must not be null")
        val userSession = sessionManager.session(request.sessionId)

        var message: Any? = null

        when (request.action) {
            SessionAction.ADD -> {
                Assert.notNull(request.key, "session key must not be null")
                Assert.notNull(request.data, "data must not be null")
                userSession.save(request.sessionId, request.key, request.data)

                message = request.data
            }

            SessionAction.CLEAR -> {
                userSession.clear(request.sessionId)
            }

            SessionAction.EVICT -> {
                Assert.notNull(request.key, "session key must not be null")
                userSession.evict(request.sessionId, request.key)
                message = request.key
            }

            SessionAction.FETCH -> {
                request.key?.let {
                    message = if (it == "*") userSession.fetchAll(request.sessionId)
                                else userSession.get(request.sessionId, it)
                }
            }
        }

        return ResponseEntity.ok(message ?: "Request action: ${request.action} has been actioned")
    }
}
