package zw.co.dcl.jawce.engine.internal.mappers;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import zw.co.dcl.jawce.engine.model.dto.ListSection;
import zw.co.dcl.jawce.engine.model.dto.SectionRowItem;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class SectionsDeserializer extends JsonDeserializer<List<ListSection>> {
    @Override
    public List<ListSection> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        ObjectMapper mapper = (ObjectMapper) p.getCodec();
        JsonNode root = mapper.readTree(p);

        List<ListSection> sections = new ArrayList<>();
        Iterator<Map.Entry<String, JsonNode>> sectionFields = root.fields();

        while (sectionFields.hasNext()) {
            Map.Entry<String, JsonNode> sectionEntry = sectionFields.next();
            String sectionTitle = sectionEntry.getKey();
            JsonNode rowsNode = sectionEntry.getValue();

            List<SectionRowItem> rowItems = new ArrayList<>();
            Iterator<Map.Entry<String, JsonNode>> rows = rowsNode.fields();

            while (rows.hasNext()) {
                Map.Entry<String, JsonNode> row = rows.next();
                String id = row.getKey();
                SectionRowItem item = mapper.treeToValue(row.getValue(), SectionRowItem.class);
                item.setId(id);
                rowItems.add(item);
            }

            ListSection listSection = new ListSection();
            listSection.setTitle(sectionTitle);
            listSection.setRows(rowItems);

            sections.add(listSection);
        }

        return sections;
    }
}
