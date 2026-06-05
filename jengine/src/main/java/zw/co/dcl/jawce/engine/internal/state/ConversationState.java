package zw.co.dcl.jawce.engine.internal.state;

import zw.co.dcl.jawce.engine.api.iface.ISessionManager;
import zw.co.dcl.jawce.engine.api.utils.SerializeUtils;
import zw.co.dcl.jawce.engine.constants.EngineConstant;
import zw.co.dcl.jawce.engine.constants.SessionConstant;
import zw.co.dcl.jawce.engine.internal.recovery.RecoveryIntent;
import zw.co.dcl.jawce.engine.model.dto.DynamicChoice;
import zw.co.dcl.jawce.engine.model.dto.DynamicChoiceSelection;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class ConversationState {
    private final String sessionId;
    private final ISessionManager session;

    public ConversationState(String sessionId, ISessionManager sessionManager) {
        this.sessionId = sessionId;
        this.session = sessionManager.session(sessionId);
    }

    public String sessionId() {
        return this.sessionId;
    }

    public ISessionManager session() {
        return this.session;
    }

    public void initializeAtStartMenu(String startMenu) {
        this.session.saveAll(
                this.sessionId,
                Map.of(
                        SessionConstant.CURRENT_STAGE, startMenu,
                        SessionConstant.PREV_STAGE, startMenu
                )
        );
    }

    public String currentStage() {
        return this.session.get(this.sessionId, SessionConstant.CURRENT_STAGE, String.class);
    }

    public void setCurrentStage(String stage) {
        this.session.save(this.sessionId, SessionConstant.CURRENT_STAGE, stage);
    }

    public String previousStage() {
        return this.session.get(this.sessionId, SessionConstant.PREV_STAGE, String.class);
    }

    public void setPreviousStage(String stage) {
        this.session.save(this.sessionId, SessionConstant.PREV_STAGE, stage);
    }

    public void advanceTo(String nextStage) {
        this.session.evict(this.sessionId, SessionConstant.CURRENT_STAGE_RETRY_COUNT);
        this.setPreviousStage(this.currentStage());
        this.setCurrentStage(nextStage);
    }

    public void rollbackOrClear(String startMenu) {
        String currentStage = this.currentStage();
        if(currentStage == null || currentStage.equalsIgnoreCase(startMenu)) {
            this.clear();
            return;
        }

        this.setCurrentStage(this.previousStage());
    }

    public boolean isAuthenticated() {
        return this.session.get(this.sessionId, SessionConstant.AUTH_SET_KEY) != null;
    }

    public String lastActivity() {
        return this.session.get(this.sessionId, SessionConstant.LAST_ACTIVITY_KEY, String.class);
    }

    public void touchLastActivity(String timestamp) {
        this.session.save(this.sessionId, SessionConstant.LAST_ACTIVITY_KEY, timestamp);
    }

    public String currentMessageId() {
        return this.session.get(this.sessionId, SessionConstant.CURRENT_MSG_ID_KEY, String.class);
    }

    public void setCurrentMessageId(String messageId) {
        this.session.save(this.sessionId, SessionConstant.CURRENT_MSG_ID_KEY, messageId);
    }

    @SuppressWarnings("unchecked")
    public Set<String> messageHistory() {
        Object queue = this.session.get(this.sessionId, SessionConstant.SESSION_MESSAGE_HISTORY_KEY);
        if(queue == null) {
            return new LinkedHashSet<>();
        }

        if(queue instanceof Set<?> set) {
            return toStringSet(set);
        }

        if(queue instanceof Collection<?> collection) {
            return toStringSet(collection);
        }

        return new LinkedHashSet<>();
    }

    public boolean hasProcessedMessageId(String messageId) {
        return this.messageHistory().contains(messageId);
    }

    public void rememberMessageId(String messageId) {
        Set<String> queue = this.messageHistory();
        queue.add(messageId);

        if(queue.size() > EngineConstant.MESSAGE_QUEUE_COUNT) {
            int removeCount = Math.max(0, queue.size() - 10);
            var iterator = queue.iterator();
            while(iterator.hasNext() && removeCount > 0) {
                iterator.next();
                iterator.remove();
                removeCount--;
            }
        }

        this.session.save(this.sessionId, SessionConstant.SESSION_MESSAGE_HISTORY_KEY, queue);
    }

    private Set<String> toStringSet(Collection<?> values) {
        Set<String> result = new LinkedHashSet<>();
        for(Object value : values) {
            if(value != null) {
                result.add(String.valueOf(value));
            }
        }
        return result;
    }

    public Long currentDebounceTimestamp() {
        return this.session.get(this.sessionId, SessionConstant.CURRENT_DEBOUNCE_KEY, Long.class);
    }

    public void markDebounceAt(long timestamp) {
        this.session.save(this.sessionId, SessionConstant.CURRENT_DEBOUNCE_KEY, timestamp);
    }

    public boolean hasDynamicCurrentTemplateBody() {
        return this.session.get(this.sessionId, SessionConstant.DYNAMIC_CURRENT_TEMPLATE_BODY_KEY) instanceof Map;
    }

    public Map<String, Object> dynamicCurrentTemplateBody() {
        return this.session.get(this.sessionId, SessionConstant.DYNAMIC_CURRENT_TEMPLATE_BODY_KEY, Map.class);
    }

    public boolean hasDynamicNextTemplateBody() {
        return this.session.get(this.sessionId, SessionConstant.DYNAMIC_NEXT_TEMPLATE_BODY_KEY) instanceof Map;
    }

    public Map<String, Object> dynamicNextTemplateBody() {
        return this.session.get(this.sessionId, SessionConstant.DYNAMIC_NEXT_TEMPLATE_BODY_KEY, Map.class);
    }

    public void clearDynamicNextTemplateBody() {
        this.session.evict(this.sessionId, SessionConstant.DYNAMIC_NEXT_TEMPLATE_BODY_KEY);
    }

    public Object checkpointStage() {
        return this.session.get(this.sessionId, SessionConstant.SESSION_CHECKPOINT_KEY);
    }

    public void saveCheckpoint(String stage) {
        this.session.save(this.sessionId, SessionConstant.SESSION_CHECKPOINT_KEY, stage);
    }

    public boolean isRetryPending() {
        return this.session.get(this.sessionId, SessionConstant.SESSION_DYNAMIC_RETRY_KEY) != null;
    }

    public void markRetryPending() {
        String currentStage = this.currentStage();
        if(currentStage != null) {
            this.saveCheckpoint(currentStage);
        }
        this.session.save(this.sessionId, SessionConstant.SESSION_DYNAMIC_RETRY_KEY, true);
    }

    public void clearRetryPending() {
        this.session.evict(this.sessionId, SessionConstant.SESSION_DYNAMIC_RETRY_KEY);
    }

    @SuppressWarnings("unchecked")
    public Map<String, RecoveryIntent> recoveryActions() {
        Object value = this.session.get(this.sessionId, SessionConstant.SESSION_RECOVERY_ACTIONS_KEY);
        if(!(value instanceof Map<?, ?> map)) {
            return Map.of();
        }

        Map<String, RecoveryIntent> actions = new LinkedHashMap<>();
        for(Map.Entry<?, ?> entry : map.entrySet()) {
            if(entry.getKey() == null || entry.getValue() == null) {
                continue;
            }

            String input = String.valueOf(entry.getKey());
            Object actionValue = entry.getValue();

            if(actionValue instanceof RecoveryIntent intent) {
                actions.put(input, intent);
                continue;
            }

            try {
                actions.put(input, RecoveryIntent.valueOf(String.valueOf(actionValue)));
            } catch (IllegalArgumentException ignored) {
                // Ignore unknown persisted recovery actions to keep state loading tolerant.
            }
        }

        return Map.copyOf(actions);
    }

    public void rememberRecoveryActions(Map<String, RecoveryIntent> actions) {
        if(actions == null || actions.isEmpty()) {
            this.clearRecoveryActions();
            return;
        }

        this.session.save(this.sessionId, SessionConstant.SESSION_RECOVERY_ACTIONS_KEY, new LinkedHashMap<>(actions));
    }

    public Optional<RecoveryIntent> recoveryIntentFor(String input) {
        if(input == null) {
            return Optional.empty();
        }

        return this.recoveryActions().entrySet().stream()
                .filter(entry -> input.equalsIgnoreCase(entry.getKey()))
                .map(Map.Entry::getValue)
                .findFirst();
    }

    public void clearRecoveryActions() {
        this.session.evict(this.sessionId, SessionConstant.SESSION_RECOVERY_ACTIONS_KEY);
    }

    public String dynamicChoiceStage() {
        return this.session.get(this.sessionId, SessionConstant.SESSION_DYNAMIC_CHOICE_STAGE_KEY, String.class);
    }

    @SuppressWarnings("unchecked")
    public List<DynamicChoice> dynamicChoices() {
        Object value = this.session.get(this.sessionId, SessionConstant.SESSION_DYNAMIC_CHOICE_REGISTRY_KEY);
        if(value == null) {
            return List.of();
        }

        if(value instanceof List<?> list) {
            List<DynamicChoice> choices = new java.util.ArrayList<>();
            for(Object item : list) {
                if(item instanceof DynamicChoice choice) {
                    choices.add(choice);
                } else if(item instanceof Map<?, ?> map) {
                    choices.add(SerializeUtils.castValue(map, DynamicChoice.class));
                }
            }
            return List.copyOf(choices);
        }

        return List.of();
    }

    public void registerDynamicChoices(String stage, List<DynamicChoice> choices) {
        if(choices == null || choices.isEmpty()) {
            this.clearDynamicChoices();
            return;
        }

        this.session.save(this.sessionId, SessionConstant.SESSION_DYNAMIC_CHOICE_STAGE_KEY, stage);
        this.session.save(this.sessionId, SessionConstant.SESSION_DYNAMIC_CHOICE_REGISTRY_KEY, choices);
    }

    public boolean hasDynamicChoicesForStage(String stage) {
        return stage != null
                && stage.equalsIgnoreCase(this.dynamicChoiceStage())
                && !this.dynamicChoices().isEmpty();
    }

    public void clearDynamicChoices() {
        this.session.evict(this.sessionId, SessionConstant.SESSION_DYNAMIC_CHOICE_STAGE_KEY);
        this.session.evict(this.sessionId, SessionConstant.SESSION_DYNAMIC_CHOICE_REGISTRY_KEY);
    }

    public Map<String, Object> toAdditionalData(DynamicChoiceSelection selection) {
        Map<String, Object> dynamicChoice = new java.util.HashMap<>();
        dynamicChoice.put("stage", selection.getStage());
        dynamicChoice.put("input", selection.getInput());
        dynamicChoice.put("id", selection.getChoice().getId());
        dynamicChoice.put("label", selection.getChoice().getLabel());
        dynamicChoice.put("description", selection.getChoice().getDescription());
        dynamicChoice.put("ordinal", selection.getChoice().getOrdinal());
        dynamicChoice.put("metadata", selection.getChoice().getMetadata());

        return Map.of("dynamicChoice", dynamicChoice);
    }

    public void clear() {
        this.session.clear(this.sessionId);
    }

    public void saveDefaultProfile(String waName, String waMobile) {
        if(this.session.get(this.sessionId, SessionConstant.DEFAULT_WA_USERNAME, String.class) == null) {
            this.session.save(this.sessionId, SessionConstant.DEFAULT_WA_USERNAME, waName);
        }

        if(this.session.get(this.sessionId, SessionConstant.DEFAULT_WA_MOBILE, String.class) == null) {
            this.session.save(this.sessionId, SessionConstant.DEFAULT_WA_MOBILE, waMobile);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type) {
        return this.session.get(this.sessionId, key, type);
    }

    public Object get(String key) {
        return this.session.get(this.sessionId, key);
    }

    public void save(String key, Object value) {
        this.session.save(this.sessionId, key, value);
    }

    public void saveAll(Map<String, Object> values) {
        this.session.saveAll(this.sessionId, values);
    }

    public void evict(String key) {
        this.session.evict(this.sessionId, key);
    }

    public void clearRetaining(List<String> retain) {
        this.session.clear(this.sessionId, retain);
    }
}
