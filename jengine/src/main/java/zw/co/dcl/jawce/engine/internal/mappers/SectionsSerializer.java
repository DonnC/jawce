package zw.co.dcl.jawce.engine.internal.mappers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import zw.co.dcl.jawce.engine.model.dto.ListSection;
import zw.co.dcl.jawce.engine.model.dto.SectionRowItem;

import java.io.IOException;
import java.util.List;

public class SectionsSerializer extends JsonSerializer<List<ListSection>> {

    @Override
    public void serialize(List<ListSection> value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartObject();
        for (ListSection section : value) {
            gen.writeObjectFieldStart(section.getTitle());

            for (SectionRowItem row : section.getRows()) {
                gen.writeObjectField(row.getId(), row);
            }

            gen.writeEndObject();
        }
        gen.writeEndObject();
    }
}
