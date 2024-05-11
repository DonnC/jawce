package zw.co.dcl.engine.whatsapp.processor;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.samskivert.mustache.Mustache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Map;

public class ProcessorRender {
    private final Logger logger = LoggerFactory.getLogger(ProcessorRender.class);
    private final ObjectMapper mapper = new ObjectMapper();

    private void executeTemplate(StringReader template, Writer out, Map<String, Object> data) {
        Mustache.compiler().compile(template).execute(data, out);
    }

    public Map<String, Object> renderTemplate(Map rawMap, Map<String, Object> data) {
        try (Writer writer = new StringWriter()) {
            executeTemplate(new StringReader(mapper.writeValueAsString(rawMap)), writer, data);
            return mapper.readValue(writer.toString(), new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception e) {
            logger.info("failed to render tpl: {}", e.getMessage());
            return rawMap;
        }
    }
}
