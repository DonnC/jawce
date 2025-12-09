package zw.co.dcl.jawce.engine.internal.mappers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import zw.co.dcl.jawce.engine.model.core.EngineRoute;

import java.io.IOException;
import java.util.List;

public class EngineRouteMapSerializer extends JsonSerializer<List<EngineRoute>> {
    @Override
    public void serialize(List<EngineRoute> value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartObject();
        for (EngineRoute route : value) {
            gen.writeStringField(route.getUserInput(), route.getNextStage());
        }
        gen.writeEndObject();
    }
}
