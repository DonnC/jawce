package zw.co.dcl.engine.whatsapp.entity.dto;

public record RestSaveSession(
        String user,
        String key,
        Boolean prop,
        Object data
) {
}
