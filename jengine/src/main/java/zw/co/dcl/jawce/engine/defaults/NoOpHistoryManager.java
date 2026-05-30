package zw.co.dcl.jawce.engine.defaults;

import zw.co.dcl.jawce.engine.api.iface.IHistoryManager;
import zw.co.dcl.jawce.engine.model.history.ChatHistoryEvent;

public class NoOpHistoryManager implements IHistoryManager {
    @Override
    public void record(ChatHistoryEvent event) {
        // intentionally blank
    }
}
