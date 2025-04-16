package zw.co.dcl.jawce.engine.model.mappers;


import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;
import zw.co.dcl.jawce.engine.model.abs.AbsHookArg;
import zw.co.dcl.jawce.engine.model.core.HookArg;
import zw.co.dcl.jawce.engine.model.core.HookArgRest;

@Mapper(
        componentModel = "spring",
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS
)
public abstract class EngineDtoMapper {
    public abstract HookArgRest map(HookArg args);

    public abstract HookArg map(HookArgRest args);

    public abstract HookArg map(AbsHookArg args);
}
