package zw.co.dcl.ehailing.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import zw.co.dcl.jawce.engine.constants.WhatsAppFlowConstant;
import zw.co.dcl.jawce.engine.internal.service.WhatsAppFlowService;
import zw.co.dcl.jawce.engine.model.core.FlowEndpointPayload;
import zw.co.dcl.jawce.engine.model.core.FlowEndpointResponse;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/whatsapp/flow")
public class WhatsAppFlowController {
    private final WhatsAppFlowService flowService;

    public WhatsAppFlowController(WhatsAppFlowService flowService) {
        this.flowService = flowService;
    }


    @PostMapping("/endpoint")
    public ResponseEntity<String> handleFlow(@RequestBody Map<String, Object> incoming) {
        log.debug("Received request to handle flow endpoint: {}", incoming);

        Map<String, Object> responsePayload;

        try {
            // 1. Decrypt incoming payload
            FlowEndpointResponse cfg = flowService.decryptPayload(incoming);
            log.info("Decrypted flow endpoint response: {}", cfg);

            // 2. Inspect payload and build response payload map
            FlowEndpointPayload flowPayload = cfg.payload();

            // TODO: verify response action

            // first check if it's a ping request, respond immediately
            if(flowPayload.getAction().equals(WhatsAppFlowConstant.PING_FLOW_ACTION)) {
                responsePayload = WhatsAppFlowConstant.PING_PAYLOAD;
            }

            // check if there was an error
            else if(flowPayload.getData().containsKey("error") && flowPayload.getData().containsKey("error_message")) {
                // TODO: analyze flow endpoint error received and ack error
                String error = flowPayload.getData().get("error_message").toString();
                log.error("Flow endpoint error: {}", error);

                responsePayload = WhatsAppFlowConstant.ACK_ERROR_PAYLOAD;
            }

            // core flow endpoint request, handle it accordingly
            else {
                // TODO: Example response: adapt to your flow logic
                responsePayload = Map.of(
                        "version", flowPayload.getVersion(),
                        "action", "next_step",
                        "data", Map.of("message", "Thanks, received")
                );
            }

            // 3. Encrypt response
            String encryptedResponse = flowService.encryptResponse(responsePayload, cfg);

            // 4. Return  encrypted flow data as WhatsApp expects
            return flowService.flowResponse(encryptedResponse, WhatsAppFlowConstant.SUCCESS_HTTP_CODE);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }
}
