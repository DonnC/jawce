package zw.co.dcl.engine.whatsapp.processor.interfaces;

import zw.co.dcl.engine.whatsapp.entity.dto.MsgProcessorResponseDTO;

import java.util.Map;

public interface IMessageProcessor {
    String getNextRoute();

    String checkInactivity();

    /**
     *
     * if template requires auth, verify user is authenticated
     * <p>
     * if yes, return the passed template else return login template after..
     * <p>
     * 1. clear all session data
     * <p>
     * 2. navigate back to login screen
     * @param template
     * @return Map
     */
    Map<String, Object>  authenticate(Map<String, Object> template);

    MsgProcessorResponseDTO process() throws Exception;
}
