package zw.co.dcl.jawce.engine.api.iface;

import zw.co.dcl.jawce.engine.model.abs.BaseEngineTemplate;
import zw.co.dcl.jawce.engine.model.core.EngineRoute;

import java.util.List;
import java.util.Optional;

public interface ITemplateStorageManager {
    void loadTemplates();

    void loadTriggers();

    boolean exists(String templateName);

    List<EngineRoute> triggers();

    /**
     * Lookup and fetch a template by name
     *
     * @param templateName: template to fetch
     * @return BaseEngineTemplate
     */
    Optional<BaseEngineTemplate> getTemplate(String templateName);
}
