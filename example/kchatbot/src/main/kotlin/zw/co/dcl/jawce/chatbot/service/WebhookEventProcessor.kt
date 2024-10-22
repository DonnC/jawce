package zw.co.dcl.jawce.chatbot.service

import jakarta.servlet.http.HttpServletRequest
import org.springframework.context.ApplicationEvent
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import zw.co.dcl.jawce.chatbot.requestHeadersToMap

class WebhookEvent(
    source: Any,
    val payload: Map<String, Any>,
    val request: HttpServletRequest
) : ApplicationEvent(source) {}

@Service
class WebhookEventProcessor(private val config: WebhookConfigService) {
    @Async
    @EventListener
    fun handleWebhookEvent(webhookEvent: WebhookEvent) {
        config
            .entryInstance()
            .processWebhook(
                webhookEvent.payload,
                requestHeadersToMap(webhookEvent.request)
            )
    }
}
