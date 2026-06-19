package zw.co.dcl.jawce.engine.api.pagination;

import zw.co.dcl.jawce.engine.api.exceptions.InternalException;
import zw.co.dcl.jawce.engine.model.core.Hook;
import zw.co.dcl.jawce.engine.model.dto.DynamicChoice;
import zw.co.dcl.jawce.engine.model.dto.ListSection;
import zw.co.dcl.jawce.engine.model.dto.SectionRowItem;
import zw.co.dcl.jawce.engine.model.dto.TemplateDynamicBody;
import zw.co.dcl.jawce.engine.model.messages.ButtonMessage;
import zw.co.dcl.jawce.engine.model.messages.ListMessage;
import zw.co.dcl.jawce.engine.model.template.ButtonTemplate;
import zw.co.dcl.jawce.engine.model.template.ListTemplate;
import zw.co.dcl.jawce.engine.model.template.TextTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

public final class PaginationSupport {
    private static final String SESSION_PAGINATION_KEY_PREFIX = "jPagination:";
    private static final String PAGINATION_METADATA_KEY = "_jawcePagination";
    private static final String METADATA_ACTION_KEY = "action";
    private static final String METADATA_STATE_KEY = "stateKey";
    private static final String METADATA_TARGET_PAGE_KEY = "targetPage";
    private static final int MAX_LIST_ROWS = 10;
    private static final int MAX_BUTTONS = 3;

    private PaginationSupport() {
    }

    public static <T> List<DynamicChoice> mapChoices(
            List<T> items,
            Function<T, String> idMapper,
            Function<T, String> labelMapper,
            Function<T, String> descriptionMapper,
            BiFunction<T, Integer, Map<String, Object>> metadataMapper) {
        if(items == null || items.isEmpty()) {
            return List.of();
        }

        List<DynamicChoice> choices = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            T item = items.get(i);
            int ordinal = i + 1;
            String label = labelMapper.apply(item);
            String id = idMapper.apply(item);
            String description = descriptionMapper == null ? null : descriptionMapper.apply(item);
            Map<String, Object> metadata = metadataMapper == null ? Map.of() : safeMetadata(metadataMapper.apply(item, ordinal));

            List<String> aliases = new ArrayList<>();
            if(label != null && !label.isBlank()) {
                aliases.add(label);
            }
            if(id != null && !id.isBlank() && !Objects.equals(id, label)) {
                aliases.add(id);
            }

            choices.add(DynamicChoice.builder()
                    .id(id)
                    .label(label)
                    .description(description)
                    .ordinal(ordinal)
                    .aliases(List.copyOf(aliases))
                    .metadata(metadata)
                    .build());
        }
        return List.copyOf(choices);
    }

    public static TemplateDynamicBody render(Hook hook, PaginationRequest request) {
        validate(hook, request);

        List<DynamicChoice> allChoices = request.getChoices() == null ? List.of() : request.getChoices();
        if(allChoices.isEmpty()) {
            clear(hook, request.getStateKey());
            return TemplateDynamicBody.builder()
                    .template(TextTemplate.builder()
                            .message(request.getEmptyMessage())
                            .build())
                    .dynamicChoices(List.of())
                    .build();
        }

        int effectivePageSize = effectivePageSize(request.getMode(), request.getPageSize(), allChoices.size());
        int totalPages = Math.max(1, (int) Math.ceil((double) allChoices.size() / effectivePageSize));
        PaginationState currentState = Optional.ofNullable(state(hook, request.getStateKey()))
                .orElse(PaginationState.builder()
                        .stateKey(request.getStateKey())
                        .mode(request.getMode())
                        .page(0)
                        .pageSize(effectivePageSize)
                        .totalItems(allChoices.size())
                        .totalPages(totalPages)
                        .build());

        int currentPage = clamp(currentState.getPage() == null ? 0 : currentState.getPage(), 0, totalPages - 1);
        int start = currentPage * effectivePageSize;
        int end = Math.min(start + effectivePageSize, allChoices.size());

        List<DynamicChoice> pageChoices = localizeOrdinals(allChoices.subList(start, end));
        List<DynamicChoice> finalChoices = new ArrayList<>(pageChoices);

        if(totalPages > 1 && currentPage > 0) {
            finalChoices.add(navigationChoice(request, PaginationAction.PREVIOUS, currentPage - 1));
        }

        if(totalPages > 1 && currentPage < totalPages - 1) {
            finalChoices.add(navigationChoice(request, PaginationAction.NEXT, currentPage + 1));
        }

        saveState(hook, PaginationState.builder()
                .stateKey(request.getStateKey())
                .mode(request.getMode())
                .page(currentPage)
                .pageSize(effectivePageSize)
                .totalItems(allChoices.size())
                .totalPages(totalPages)
                .build());

        return TemplateDynamicBody.builder()
                .template(buildTemplate(request, finalChoices, pageChoices, currentPage, totalPages))
                .dynamicChoices(List.copyOf(finalChoices))
                .build();
    }

    public static Optional<PaginationSelection> selection(Hook hook) {
        if(hook == null || hook.getAdditionalData() == null) {
            return Optional.empty();
        }

        Object rawDynamicChoice = hook.getAdditionalData().get("dynamicChoice");
        if(!(rawDynamicChoice instanceof Map<?, ?> dynamicChoiceMap)) {
            return Optional.empty();
        }

        Map<String, Object> dynamicChoice = new LinkedHashMap<>();
        dynamicChoiceMap.forEach((key, value) -> dynamicChoice.put(String.valueOf(key), value));

        String id = asString(dynamicChoice.get("id"));
        String label = asString(dynamicChoice.get("label"));
        Map<String, Object> metadata = toMap(dynamicChoice.get("metadata"));
        Map<String, Object> paginationMetadata = toMap(metadata.get(PAGINATION_METADATA_KEY));

        if(paginationMetadata.isEmpty()) {
            return Optional.of(PaginationSelection.builder()
                    .action(PaginationAction.ITEM)
                    .choiceId(id)
                    .label(label)
                    .metadata(metadata)
                    .build());
        }

        String actionValue = asString(paginationMetadata.get(METADATA_ACTION_KEY));
        PaginationAction action = "PREVIOUS".equalsIgnoreCase(actionValue)
                ? PaginationAction.PREVIOUS
                : PaginationAction.NEXT;

        return Optional.of(PaginationSelection.builder()
                .stateKey(asString(paginationMetadata.get(METADATA_STATE_KEY)))
                .action(action)
                .targetPage(asInteger(paginationMetadata.get(METADATA_TARGET_PAGE_KEY)))
                .choiceId(id)
                .label(label)
                .metadata(metadata)
                .build());
    }

    public static Optional<PaginationSelection> handleSelection(Hook hook) {
        Optional<PaginationSelection> selection = selection(hook);
        if(selection.isEmpty() || !selection.get().isNavigation()) {
            return selection;
        }

        PaginationSelection paginationSelection = selection.get();
        PaginationState currentState = state(hook, paginationSelection.getStateKey());
        if(currentState == null) {
            return selection;
        }

        int totalPages = currentState.getTotalPages() == null ? 1 : currentState.getTotalPages();
        int targetPage = clamp(
                paginationSelection.getTargetPage() == null ? 0 : paginationSelection.getTargetPage(),
                0,
                Math.max(0, totalPages - 1)
        );
        currentState.setPage(targetPage);
        saveState(hook, currentState);

        return Optional.of(paginationSelection);
    }

    public static PaginationState state(Hook hook, String stateKey) {
        if(hook == null || hook.getSession() == null || hook.getSessionId() == null || stateKey == null) {
            return null;
        }

        return hook.getSession().get(hook.getSessionId(), sessionKey(stateKey), PaginationState.class);
    }

    public static void clear(Hook hook, String stateKey) {
        if(hook == null || hook.getSession() == null || hook.getSessionId() == null || stateKey == null) {
            return;
        }

        hook.getSession().evict(hook.getSessionId(), sessionKey(stateKey));
    }

    private static void validate(Hook hook, PaginationRequest request) {
        if(hook == null || hook.getSession() == null || hook.getSessionId() == null) {
            throw new InternalException("Pagination rendering requires a hook with session and sessionId");
        }

        if(request == null) {
            throw new InternalException("Pagination request is required");
        }

        if(request.getStateKey() == null || request.getStateKey().isBlank()) {
            throw new InternalException("Pagination stateKey is required");
        }

        if(request.getMode() == null) {
            throw new InternalException("Pagination mode is required");
        }
    }

    private static void saveState(Hook hook, PaginationState state) {
        hook.getSession().save(hook.getSessionId(), sessionKey(state.getStateKey()), state);
    }

    private static String sessionKey(String stateKey) {
        return SESSION_PAGINATION_KEY_PREFIX + stateKey;
    }

    private static int effectivePageSize(PaginationMode mode, int requestedPageSize, int totalItems) {
        int basePageSize = requestedPageSize <= 0 ? 10 : requestedPageSize;

        if(mode == PaginationMode.LIST) {
            int reservedSlots = totalItems > MAX_LIST_ROWS ? 2 : 0;
            return Math.max(1, Math.min(basePageSize, MAX_LIST_ROWS - reservedSlots));
        }

        if(mode == PaginationMode.BUTTON) {
            int reservedSlots = totalItems > MAX_BUTTONS ? 2 : 0;
            return Math.max(1, Math.min(basePageSize, MAX_BUTTONS - reservedSlots));
        }

        return Math.max(1, basePageSize);
    }

    private static List<DynamicChoice> localizeOrdinals(List<DynamicChoice> choices) {
        List<DynamicChoice> localized = new ArrayList<>();
        for (int i = 0; i < choices.size(); i++) {
            DynamicChoice choice = choices.get(i);
            localized.add(DynamicChoice.builder()
                    .id(choice.getId())
                    .label(choice.getLabel())
                    .description(choice.getDescription())
                    .ordinal(i + 1)
                    .aliases(choice.getAliases() == null ? List.of() : List.copyOf(choice.getAliases()))
                    .metadata(choice.getMetadata() == null ? Map.of() : Map.copyOf(choice.getMetadata()))
                    .build());
        }
        return List.copyOf(localized);
    }

    private static DynamicChoice navigationChoice(PaginationRequest request, PaginationAction action, int targetPage) {
        String label = action == PaginationAction.PREVIOUS ? request.getPreviousLabel() : request.getNextLabel();
        List<String> aliases = action == PaginationAction.PREVIOUS
                ? List.of(label, "back", "prev", "previous")
                : List.of(label, "next", "more");

        return DynamicChoice.builder()
                .id(request.getStateKey() + "::" + action.name() + "::" + targetPage)
                .label(label)
                .description(action == PaginationAction.PREVIOUS ? "Show previous page" : "Show next page")
                .aliases(aliases)
                .metadata(Map.of(
                        PAGINATION_METADATA_KEY,
                        Map.of(
                                METADATA_ACTION_KEY, action.name(),
                                METADATA_STATE_KEY, request.getStateKey(),
                                METADATA_TARGET_PAGE_KEY, targetPage
                        )
                ))
                .build();
    }

    private static zw.co.dcl.jawce.engine.model.abs.BaseEngineTemplate buildTemplate(
            PaginationRequest request,
            List<DynamicChoice> finalChoices,
            List<DynamicChoice> itemChoices,
            int currentPage,
            int totalPages) {
        return switch (request.getMode()) {
            case LIST -> buildListTemplate(request, finalChoices, currentPage, totalPages);
            case BUTTON -> buildButtonTemplate(request, finalChoices, currentPage, totalPages);
            case TEXT -> buildTextTemplate(request, finalChoices, itemChoices, currentPage, totalPages);
        };
    }

    private static ListTemplate buildListTemplate(
            PaginationRequest request,
            List<DynamicChoice> choices,
            int currentPage,
            int totalPages) {
        List<SectionRowItem> rows = new ArrayList<>();
        for (DynamicChoice choice : choices) {
            rows.add(SectionRowItem.builder()
                    .id(choice.getId())
                    .title(choice.getLabel())
                    .description(choice.getDescription())
                    .build());
        }

        String body = withPageIndicator(request.getPrompt(), currentPage, totalPages);
        return ListTemplate.builder()
                .message(ListMessage.builder()
                        .title(request.getTitle())
                        .body(body)
                        .footer(request.getFooter())
                        .button(request.getButtonLabel())
                        .sections(List.of(ListSection.builder()
                                .title(request.getSectionTitle())
                                .rows(rows)
                                .build()))
                        .build())
                .build();
    }

    private static ButtonTemplate buildButtonTemplate(
            PaginationRequest request,
            List<DynamicChoice> choices,
            int currentPage,
            int totalPages) {
        List<String> buttons = new ArrayList<>();
        for (DynamicChoice choice : choices) {
            buttons.add(choice.getLabel());
        }

        String body = withPageIndicator(request.getPrompt(), currentPage, totalPages);
        return ButtonTemplate.builder()
                .message(ButtonMessage.builder()
                        .title(request.getTitle())
                        .body(body)
                        .footer(request.getFooter())
                        .buttons(buttons)
                        .build())
                .build();
    }

    private static TextTemplate buildTextTemplate(
            PaginationRequest request,
            List<DynamicChoice> choices,
            List<DynamicChoice> itemChoices,
            int currentPage,
            int totalPages) {
        List<String> lines = new ArrayList<>();
        lines.add(withPageIndicator(request.getPrompt(), currentPage, totalPages));
        lines.add("");

        for (DynamicChoice choice : itemChoices) {
            lines.add(choice.getOrdinal() + ". " + choice.getLabel());
        }

        List<String> navigationHints = new ArrayList<>();
        for (DynamicChoice choice : choices) {
            Map<String, Object> metadata = safeMetadata(choice.getMetadata());
            Map<String, Object> paginationMetadata = toMap(metadata.get(PAGINATION_METADATA_KEY));
            if(!paginationMetadata.isEmpty()) {
                navigationHints.add(choice.getLabel());
            }
        }

        if(!navigationHints.isEmpty()) {
            lines.add("");
            lines.add("Reply " + String.join(" / ", navigationHints) + " to navigate");
        }

        return TextTemplate.builder()
                .message(String.join("\n", lines))
                .build();
    }

    private static String withPageIndicator(String prompt, int currentPage, int totalPages) {
        String safePrompt = prompt == null || prompt.isBlank() ? "Select an option" : prompt;
        if(totalPages <= 1) {
            return safePrompt;
        }

        return safePrompt + "\nPage " + (currentPage + 1) + " of " + totalPages;
    }

    private static Map<String, Object> safeMetadata(Map<String, Object> metadata) {
        return metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    private static Map<String, Object> toMap(Object value) {
        if(!(value instanceof Map<?, ?> map)) {
            return Map.of();
        }

        Map<String, Object> normalized = new LinkedHashMap<>();
        map.forEach((key, rawValue) -> normalized.put(String.valueOf(key), rawValue));
        return normalized;
    }

    private static String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static Integer asInteger(Object value) {
        if(value instanceof Integer integer) {
            return integer;
        }

        if(value instanceof Number number) {
            return number.intValue();
        }

        if(value == null) {
            return null;
        }

        return Integer.parseInt(String.valueOf(value));
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(value, max));
    }
}
