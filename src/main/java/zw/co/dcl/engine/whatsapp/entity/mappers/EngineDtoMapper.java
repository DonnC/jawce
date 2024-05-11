package zw.co.dcl.engine.whatsapp.entity.mappers;


import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;
import zw.co.dcl.engine.whatsapp.entity.DefaultHookArgs;
import zw.co.dcl.engine.whatsapp.entity.dto.HookArgs;
import zw.co.dcl.engine.whatsapp.entity.dto.HookArgsRest;

import java.util.Map;

@Mapper(
        componentModel = "spring",
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS
)
public abstract class EngineDtoMapper {
    public abstract HookArgsRest map(HookArgs args);

    public abstract HookArgs map(HookArgsRest args);

    public abstract HookArgs map(DefaultHookArgs args);
}