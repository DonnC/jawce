package zw.co.dcl.jawce.engine.model.mappers;


import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;
import zw.co.dcl.jawce.engine.model.DefaultHookArgs;
import zw.co.dcl.jawce.engine.model.dto.HookArgs;
import zw.co.dcl.jawce.engine.model.dto.HookArgsRest;

@Mapper(
        componentModel = "spring",
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS
)
public abstract class EngineDtoMapper {
    public abstract HookArgsRest map(HookArgs args);

    public abstract HookArgs map(HookArgsRest args);

    public abstract HookArgs map(DefaultHookArgs args);
}
