package zw.co.dcl.jawce.engine.processor;

import com.samskivert.mustache.Mustache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import zw.co.dcl.jawce.engine.exceptions.EngineRenderException;
import zw.co.dcl.jawce.engine.utils.CommonUtils;

import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Map;

public class RenderProcessor {
    private final Logger logger = LoggerFactory.getLogger(RenderProcessor.class);

    private void executeTemplate(StringReader template, Writer out, Map<String, Object> data) {
        Mustache.compiler().compile(template).execute(data, out);
    }

    public Map<String, Object> renderTemplate(Map rawMap, Map<String, Object> data) {
        try (Writer writer = new StringWriter()) {
            executeTemplate(new StringReader(CommonUtils.toJsonString(rawMap)), writer, data);
            var renderedMessage = writer.toString();

            if(CommonUtils.containsMustacheVariables(renderedMessage)) {
                throw new RuntimeException("Template rendering failed, template contains unrendered placeholders");
            }

            return CommonUtils.objectToMap(renderedMessage);
        } catch (Exception e) {
            logger.error("TEMPLATE RENDERING: {}", e.getMessage());
            throw new EngineRenderException("Failed to render template");
        }
    }
}
