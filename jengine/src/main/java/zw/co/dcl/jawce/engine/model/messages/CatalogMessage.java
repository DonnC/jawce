package zw.co.dcl.jawce.engine.model.messages;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.SuperBuilder;
import zw.co.dcl.jawce.engine.model.abs.BaseInteractiveMessage;

@EqualsAndHashCode(callSuper = true)
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class CatalogMessage extends BaseInteractiveMessage {
    @JsonProperty("product-id")
    private String productId;
}
