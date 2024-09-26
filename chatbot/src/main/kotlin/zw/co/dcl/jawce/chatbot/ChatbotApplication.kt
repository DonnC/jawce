package zw.co.dcl.jawce.chatbot

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["zw.co.dcl.jawce"])
class ChatbotApplication
fun main(args: Array<String>) {
    runApplication<ChatbotApplication>(*args)
}
