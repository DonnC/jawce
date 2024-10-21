package zw.co.dcl.jawce.chatbot

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching
import org.springframework.scheduling.annotation.EnableAsync

@SpringBootApplication(scanBasePackages = ["zw.co.dcl.jawce.chatbot"])
@EnableAsync
@EnableCaching
class ChatbotApplication
fun main(args: Array<String>) {
    runApplication<ChatbotApplication>(*args)
}
