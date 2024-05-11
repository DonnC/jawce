package zw.co.dcl.engine.whatsapp.processor.iface;

import zw.co.dcl.engine.whatsapp.entity.dto.EnginePreProcessor;
import zw.co.dcl.engine.whatsapp.entity.dto.MsgProcessorResponseDTO;

import java.util.Map;

public interface IMessageProcessor {
    String getNextRoute();

    boolean hasInteractionActivityExpired();

    Map<String, Object>  authenticate(Map<String, Object> template);

    EnginePreProcessor preProcessor() throws Exception;

    MsgProcessorResponseDTO process() throws Exception;
}
