package zw.co.dcl.engine.whatsapp.entity.mappers;

import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;
import zw.co.dcl.engine.whatsapp.entity.DefaultHookArgs;
import zw.co.dcl.engine.whatsapp.entity.dto.HookArgs;
import zw.co.dcl.engine.whatsapp.entity.dto.HookArgsRest;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2024-05-10T16:34:31+0200",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 19.0.2 (Oracle Corporation)"
)
@Component
public class EngineDtoMapperImpl extends EngineDtoMapper {

    @Override
    public HookArgsRest map(HookArgs args) {
        if ( args == null ) {
            return null;
        }

        HookArgsRest hookArgsRest = new HookArgsRest();

        if ( args.getChannelUser() != null ) {
            hookArgsRest.setChannelUser( args.getChannelUser() );
        }
        if ( args.getUserInput() != null ) {
            hookArgsRest.setUserInput( args.getUserInput() );
        }
        if ( args.getFlow() != null ) {
            hookArgsRest.setFlow( args.getFlow() );
        }
        Map<String, Object> map = args.getAdditionalData();
        if ( map != null ) {
            hookArgsRest.setAdditionalData( new LinkedHashMap<String, Object>( map ) );
        }
        if ( args.getTemplateDynamicBody() != null ) {
            hookArgsRest.setTemplateDynamicBody( args.getTemplateDynamicBody() );
        }
        Map<String, Object> map1 = args.getMethodArgs();
        if ( map1 != null ) {
            hookArgsRest.setMethodArgs( new LinkedHashMap<String, Object>( map1 ) );
        }

        return hookArgsRest;
    }

    @Override
    public HookArgs map(HookArgsRest args) {
        if ( args == null ) {
            return null;
        }

        HookArgs hookArgs = new HookArgs();

        if ( args.getChannelUser() != null ) {
            hookArgs.setChannelUser( args.getChannelUser() );
        }
        if ( args.getUserInput() != null ) {
            hookArgs.setUserInput( args.getUserInput() );
        }
        if ( args.getFlow() != null ) {
            hookArgs.setFlow( args.getFlow() );
        }
        Map<String, Object> map = args.getAdditionalData();
        if ( map != null ) {
            hookArgs.setAdditionalData( new LinkedHashMap<String, Object>( map ) );
        }
        if ( args.getTemplateDynamicBody() != null ) {
            hookArgs.setTemplateDynamicBody( args.getTemplateDynamicBody() );
        }
        Map<String, Object> map1 = args.getMethodArgs();
        if ( map1 != null ) {
            hookArgs.setMethodArgs( new LinkedHashMap<String, Object>( map1 ) );
        }

        return hookArgs;
    }

    @Override
    public HookArgs map(DefaultHookArgs args) {
        if ( args == null ) {
            return null;
        }

        HookArgs hookArgs = new HookArgs();

        if ( args.getChannelUser() != null ) {
            hookArgs.setChannelUser( args.getChannelUser() );
        }
        if ( args.getUserInput() != null ) {
            hookArgs.setUserInput( args.getUserInput() );
        }
        if ( args.getFlow() != null ) {
            hookArgs.setFlow( args.getFlow() );
        }
        Map<String, Object> map = args.getAdditionalData();
        if ( map != null ) {
            hookArgs.setAdditionalData( new LinkedHashMap<String, Object>( map ) );
        }
        if ( args.getTemplateDynamicBody() != null ) {
            hookArgs.setTemplateDynamicBody( args.getTemplateDynamicBody() );
        }
        Map<String, Object> map1 = args.getMethodArgs();
        if ( map1 != null ) {
            hookArgs.setMethodArgs( new LinkedHashMap<String, Object>( map1 ) );
        }

        return hookArgs;
    }
}
