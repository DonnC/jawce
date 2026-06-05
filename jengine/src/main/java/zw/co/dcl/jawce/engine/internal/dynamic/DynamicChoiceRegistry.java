package zw.co.dcl.jawce.engine.internal.dynamic;

import zw.co.dcl.jawce.engine.model.dto.DynamicChoice;
import zw.co.dcl.jawce.engine.model.template.ButtonTemplate;
import zw.co.dcl.jawce.engine.model.template.ListTemplate;
import zw.co.dcl.jawce.engine.model.template.TextTemplate;
import zw.co.dcl.jawce.engine.model.abs.BaseEngineTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class DynamicChoiceRegistry {
    private DynamicChoiceRegistry() {
    }

    public static List<DynamicChoice> discoverChoices(BaseEngineTemplate template, List<DynamicChoice> explicitChoices) {
        if(explicitChoices != null && !explicitChoices.isEmpty()) {
            return explicitChoices;
        }

        if(template instanceof ButtonTemplate buttonTemplate) {
            List<DynamicChoice> choices = new ArrayList<>();
            List<String> buttons = buttonTemplate.getMessage().getButtons();
            for (int i = 0; i < buttons.size(); i++) {
                String value = buttons.get(i);
                choices.add(DynamicChoice.builder()
                        .id(value)
                        .label(value)
                        .ordinal(i + 1)
                        .aliases(List.of(normalize(value)))
                        .build());
            }
            return choices;
        }

        if(template instanceof ListTemplate listTemplate) {
            List<DynamicChoice> choices = new ArrayList<>();
            int ordinal = 1;
            for (var section : listTemplate.getMessage().getSections()) {
                for (var row : section.getRows()) {
                    choices.add(DynamicChoice.builder()
                            .id(row.getId())
                            .label(row.getTitle())
                            .description(row.getDescription())
                            .ordinal(ordinal++)
                            .aliases(List.of(normalize(row.getId()), normalize(row.getTitle())))
                            .metadata(Map.of("section", section.getTitle()))
                            .build());
                }
            }
            return choices;
        }

        if(template instanceof TextTemplate) {
            return explicitChoices == null ? List.of() : explicitChoices;
        }

        return List.of();
    }

    public static Optional<DynamicChoice> resolve(List<DynamicChoice> choices, String userInput) {
        if(choices == null || choices.isEmpty() || userInput == null) {
            return Optional.empty();
        }

        String normalizedInput = normalize(userInput);

        for (DynamicChoice choice : choices) {
            if(normalizedInput.equals(normalize(choice.getId()))) {
                return Optional.of(choice);
            }

            if(choice.getLabel() != null && normalizedInput.equals(normalize(choice.getLabel()))) {
                return Optional.of(choice);
            }

            if(choice.getOrdinal() != null && normalizedInput.equals(String.valueOf(choice.getOrdinal()))) {
                return Optional.of(choice);
            }

            if(choice.getAliases() != null) {
                for (String alias : choice.getAliases()) {
                    if(normalizedInput.equals(normalize(alias))) {
                        return Optional.of(choice);
                    }
                }
            }
        }

        return Optional.empty();
    }

    static String normalize(String input) {
        return input == null ? null : input.trim().toLowerCase(Locale.ROOT);
    }
}
