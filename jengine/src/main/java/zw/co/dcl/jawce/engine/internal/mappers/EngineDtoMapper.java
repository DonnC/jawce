package zw.co.dcl.jawce.engine.internal.mappers;


import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;
import zw.co.dcl.jawce.engine.model.abs.BaseHook;
import zw.co.dcl.jawce.engine.model.core.Hook;
import zw.co.dcl.jawce.engine.model.core.HookRest;

@Mapper(
        componentModel = "spring",
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS
)
public abstract class EngineDtoMapper {
    public abstract HookRest map(Hook args);

    public abstract Hook map(HookRest args);

    public abstract Hook map(BaseHook args);
}
