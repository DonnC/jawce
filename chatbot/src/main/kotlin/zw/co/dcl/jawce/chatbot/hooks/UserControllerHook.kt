package zw.co.dcl.jawce.chatbot.hooks

import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import zw.co.dcl.jawce.engine.model.dto.HookArgsRest

@RestController
@RequestMapping("/users")
class UserControllerHook(private val service: UserHookService) {

    @PostMapping("/capture")
    fun captureRide(@RequestBody args: HookArgsRest): HookArgsRest {
        return service.captureUserRide(args)
    }
}
