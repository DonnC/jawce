package zw.co.dcl.jawce.engine.internal.service;

import com.samskivert.mustache.Mustache;
import lombok.extern.slf4j.Slf4j;
import zw.co.dcl.jawce.engine.api.exceptions.TemplateRenderException;
import zw.co.dcl.jawce.engine.api.utils.Utils;
import zw.co.dcl.jawce.engine.api.utils.SerializeUtils;

import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Map;

@Slf4j
public class RenderProcessor {
    private void executeTemplate(StringReader template, Writer out, Map<String, Object> data) {
        Mustache.compiler().compile(template).execute(data, out);
    }

    public Map<String, Object> renderTemplate(Map rawMap, Map<String, Object> data) {
        try (Writer writer = new StringWriter()) {
            executeTemplate(new StringReader(SerializeUtils.toJsonString(rawMap)), writer, data);
            var renderedMessage = writer.toString();

            if(Utils.containsMustacheVariables(renderedMessage)) {
                throw new RuntimeException("Template rendering failed, template contains non-rendered placeholders");
            }

            return SerializeUtils.toMap(renderedMessage);
        } catch (Exception e) {
            log.error("ERROR DURING TEMPLATE RENDERING: {}", e.getMessage());
            throw new TemplateRenderException("Failed to render template");
        }
    }
}
