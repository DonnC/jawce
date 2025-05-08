package zw.co.dcl.jawce.engine.internal.mappers;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import zw.co.dcl.jawce.engine.constants.EngineConstant;
import zw.co.dcl.jawce.engine.model.core.EngineRoute;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class EngineRouteDeserializer extends JsonDeserializer<List<EngineRoute>> {

    @Override
    public List<EngineRoute> deserialize(JsonParser p, DeserializationContext ctxt)
            throws IOException {

        ObjectCodec codec = p.getCodec();
        JsonNode node = codec.readTree(p);

        List<EngineRoute> routes = new ArrayList<>();

        if(node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();

            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                String key = entry.getKey();
                String value = entry.getValue().asText();

                // TODO: check if route has inner routes

                EngineRoute route = new EngineRoute();
                route.setUserInput(key);
                route.setNextStage(value);
                route.setRegex(key.startsWith(EngineConstant.TPL_REGEX_PLACEHOLDER_KEY));

                routes.add(route);
            }
        }

        return routes;
    }
}
