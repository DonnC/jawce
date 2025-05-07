package zw.co.dcl.jawce.engine.api.exceptions;


/**
 * exception thrown when engine fails to render
 * a dynamic template
 */
public class TemplateRenderException extends BaseEngineException {
    public TemplateRenderException(String message) {
        super(message);
    }

    public TemplateRenderException(String message, Throwable cause) {
        super(message, cause);
    }
}
