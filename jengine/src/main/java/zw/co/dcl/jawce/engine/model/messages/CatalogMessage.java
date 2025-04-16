package zw.co.dcl.jawce.engine.model.messages;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import zw.co.dcl.jawce.engine.model.abs.AbsInteractiveMessage;

@EqualsAndHashCode(callSuper = true)
@Data
public class CatalogMessage extends AbsInteractiveMessage {
    @JsonProperty("product-id")
    private String productId;
}
