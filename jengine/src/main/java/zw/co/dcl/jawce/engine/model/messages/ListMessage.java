package zw.co.dcl.jawce.engine.model.messages;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.*;
import lombok.experimental.SuperBuilder;
import zw.co.dcl.jawce.engine.internal.mappers.SectionsDeserializer;
import zw.co.dcl.jawce.engine.internal.mappers.SectionsSerializer;
import zw.co.dcl.jawce.engine.model.abs.BaseInteractiveMessage;
import zw.co.dcl.jawce.engine.model.dto.ListSection;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ListMessage extends BaseInteractiveMessage {
    private String button;

    @JsonSerialize(using = SectionsSerializer.class)
    @JsonDeserialize(using = SectionsDeserializer.class)
    private List<ListSection> sections;
}
