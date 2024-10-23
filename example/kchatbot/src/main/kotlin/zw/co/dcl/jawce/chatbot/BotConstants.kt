package zw.co.dcl.jawce.chatbot

object BotConstants {
    const val API_VERSION = "v18.0"

    // allow all or restrict specific numbers
    const val ACCESS_CONTROL_ORIGIN = "*"
}

enum class SessionAction {
    CLEAR,
    ADD,
    EVICT,
    FETCH
}

data class SessionRequest(
    val action: SessionAction,
    val sessionId: String,
    val key: String?,
    val data: Any?
)
