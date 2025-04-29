package zw.co.dcl.jawce.engine.processor.iface;

import zw.co.dcl.jawce.engine.model.abs.AbsEngineTemplate;
import zw.co.dcl.jawce.engine.model.dto.EnginePreProcessor;
import zw.co.dcl.jawce.engine.model.dto.MsgProcessorResponseDTO;

import java.util.Map;

public interface IMessageProcessor {
    String getNextRoute();

    boolean hasInteractionActivityExpired();

    Map<String, Object>  authenticate(AbsEngineTemplate template);

    EnginePreProcessor preProcessor() throws Exception;

    MsgProcessorResponseDTO process() throws Exception;
}
