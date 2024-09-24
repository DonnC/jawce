package zw.co.dcl.jawce.engine.exceptions;


/**
 * exception thrown when engine fails to render
 * a dynamic template
 */
public class EngineRenderException extends WaEngineException {
    public EngineRenderException(String message) {
        super(message);
    }

    public EngineRenderException(String message, Throwable cause) {
        super(message, cause);
    }
}
