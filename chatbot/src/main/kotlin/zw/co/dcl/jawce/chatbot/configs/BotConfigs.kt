package zw.co.dcl.jawce.chatbot.configs

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.readValue
import lombok.Getter
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.support.ResourcePatternResolver
import org.springframework.core.io.support.ResourcePatternUtils
import org.springframework.web.client.RestTemplate

@Configuration
class BotConfigs() {
    private val logger = LoggerFactory.getLogger(BotConfigs::class.java)

    private val mapper: ObjectMapper = ObjectMapper(YAMLFactory())
    private val resolver: ResourcePatternResolver = ResourcePatternUtils.getResourcePatternResolver(null)

    @Value("\${resources.templates}")
    private var botTemplatesLocation: String? = null;

    @Value("\${resources.triggers}")
    private var botTriggersLocation: String? = null;

    @Getter
    @Value("\${resources.hooks.base-url}")
    var botEngineHookBaseUrl: String? = null;

    @Getter
    @Value("\${resources.hooks.security-token}")
    var botEngineHookUrlToken: String? = null;

    @Bean
    fun restTemplate(): RestTemplate {
        return RestTemplate();
    }

    @Bean("botTemplates")
    fun readBotTemplates(): Map<String, Object> {
        return getResourceAsMap(botTemplatesLocation!!)
    }

    @Bean("botTriggers")
    fun readBotTriggers(): Map<String, Object> {
        return getResourceAsMap(botTriggersLocation!!)
    }

    private fun getResourceAsMap(path: String): Map<String, Object> {
        var map: MutableMap<String, Object> = mutableMapOf();

        val resources = resolver.getResources(path);

        for (resource in resources) {
            logger.warn("Processing bot resource: ${resource.filename}..")
            map.putAll(mapper.readValue<Map<String, Object>>(resource.inputStream))
        }

        return map;
    }
}
