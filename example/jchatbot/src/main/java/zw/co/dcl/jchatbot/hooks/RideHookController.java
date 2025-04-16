package zw.co.dcl.jchatbot.hooks;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import zw.co.dcl.jawce.engine.model.core.HookArgRest;

@RestController
@RequestMapping("/ride")
class RideHookController {
    @Autowired
    private RideHookService service;

    @PostMapping("/capture")
    Object captureRide(@RequestBody HookArgRest args) {
        return service.setup(args).processRide();
    }
}
