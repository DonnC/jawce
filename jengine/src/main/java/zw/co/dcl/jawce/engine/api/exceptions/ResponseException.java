package zw.co.dcl.jawce.engine.api.exceptions;

import lombok.Getter;
import zw.co.dcl.jawce.engine.internal.dto.ResponseError;

/**
 * if caught, send the response back to User
 * to get the error message for actioning
 */
public class ResponseException extends BaseEngineException {
    @Getter
    ResponseError error;

    public ResponseException(ResponseError error) {
        super(error.message());
    }

    public ResponseException(ResponseError error, Throwable cause) {
        super(error.message(), cause);
    }
}
