package zw.co.dcl.jawce.engine.model.dto;

import zw.co.dcl.jawce.engine.api.enums.MessageTypeEnum;

import java.util.Map;

/**
 *
 * parse message data and return response(model) and the response response data
 *
 * @param body a Map
 * @param type message response send by user
 */
public record ResponseStructure(
        MessageTypeEnum type,
        Map<String, Object> body
) {
}
