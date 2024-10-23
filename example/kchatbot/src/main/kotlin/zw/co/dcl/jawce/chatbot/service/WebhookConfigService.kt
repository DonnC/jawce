package zw.co.dcl.jawce.chatbot.service

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import zw.co.dcl.jawce.chatbot.BotConstants
import zw.co.dcl.jawce.chatbot.configs.BotConfigs
import zw.co.dcl.jawce.chatbot.configs.CredentialConfigs
import zw.co.dcl.jawce.engine.model.SessionSettings
import zw.co.dcl.jawce.engine.model.WhatsappSettings
import zw.co.dcl.jawce.engine.model.dto.ChannelOriginConfig
import zw.co.dcl.jawce.engine.model.dto.EngineRequestSettings
import zw.co.dcl.jawce.engine.model.dto.WaEngineConfig
import zw.co.dcl.jawce.engine.service.EntryService
import zw.co.dcl.jawce.session.ISessionManager

@Service
class WebhookConfigService(
    @Qualifier("botTemplates") private val templatesMap: Map<String, Any>,
    @Qualifier("botTriggers") private val triggersMap: Map<String, Any>,
    private val sessionManager: ISessionManager,
    private val botCredentials: CredentialConfigs,
    private val botConfig: BotConfigs
) {

    fun entryInstance(): EntryService {
        val channelOrigin = ChannelOriginConfig(
            false, listOf(), false, null,
            BotConstants.ACCESS_CONTROL_ORIGIN
        )

        return EntryService.getInstance(engineConfig(), channelOrigin)
    }

    private fun engineConfig(): WaEngineConfig {
        val requestSettings = EngineRequestSettings(
            botConfig.botEngineHookBaseUrl,
            botConfig.botEngineHookUrlToken
        )

        return WaEngineConfig(
            sessionManager,
            templatesMap,
            triggersMap,
            requestSettings,
            whatsappChannelSettings(),
            sessionSettings()
        )
    }

    private fun whatsappChannelSettings(): WhatsappSettings {
        var settings = WhatsappSettings()
        settings.hubToken = botCredentials.hubToken
        settings.accessToken = botCredentials.accessToken
        settings.phoneNumberId = botCredentials.phoneNumberId
        settings.apiVersion = BotConstants.API_VERSION
        return settings;
    }

    private fun sessionSettings(): SessionSettings {
        var settings = SessionSettings()
        settings.sessionTTL = botCredentials.sessionTtl ?: 30
        settings.isHandleSessionInactivity = false
        settings.startMenuStageKey = botCredentials.initialStage
        return settings
    }

    fun clearSession(userSessionId: String) {
        sessionManager.clear(userSessionId)
    }
}
