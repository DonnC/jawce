package zw.co.dcl.jawce.engine.model.messages;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import zw.co.dcl.jawce.engine.model.abs.BaseInteractiveMessage;

@EqualsAndHashCode(callSuper = true)
@Data
public class CatalogMessage extends BaseInteractiveMessage {
    @JsonProperty("product-id")
    private String productId;
}
