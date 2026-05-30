package zw.co.dcl.jawce.engine.api.iface;

import zw.co.dcl.jawce.engine.model.history.ChatHistoryEvent;

/**
 * Stores normalized chatbot activity outside the runtime session state.
 * <p>
 * Implementations are responsible for persistence strategy, retention,
 * and any downstream fan-out that a host application needs.
 */
public interface IHistoryManager {
    void record(ChatHistoryEvent event);
}
