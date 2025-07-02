package zw.co.dcl.jawce.engine.internal.service;

import lombok.extern.slf4j.Slf4j;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import zw.co.dcl.jawce.engine.api.exceptions.InternalException;
import zw.co.dcl.jawce.engine.api.iface.IClientManager;
import zw.co.dcl.jawce.engine.api.utils.SerializeUtils;
import zw.co.dcl.jawce.engine.configs.JawceConfig;
import zw.co.dcl.jawce.engine.constants.EngineConstant;
import zw.co.dcl.jawce.engine.constants.SessionConstant;
import zw.co.dcl.jawce.engine.internal.events.OnceOffHookEvent;
import zw.co.dcl.jawce.engine.internal.mappers.EngineDtoMapper;
import zw.co.dcl.jawce.engine.model.abs.BaseHook;
import zw.co.dcl.jawce.engine.model.core.Hook;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
public class HookService {
    final EngineDtoMapper dtoMapper = Mappers.getMapper(EngineDtoMapper.class);
    final List<String> restHooksFlag = List.of("/", "http://", "https://");

    final IClientManager client;
    final JawceConfig config;
    final ApplicationContext applicationContext;

    public HookService(IClientManager client, JawceConfig config, ApplicationContext applicationContext) {
        this.client = client;
        this.config = config;
        this.applicationContext = applicationContext;
    }

    boolean isRestHook(String hookName) {
        for (String rh : restHooksFlag) {
            if(hookName.toLowerCase().startsWith(rh.toLowerCase())) return true;
        }

        return false;
    }

    Hook processRestHook(Hook arg) {
        // /api/fetch-all | http(s)://...
        try {
            var restArg = dtoMapper.map(arg);
            var url = arg.getHook().startsWith("/") ?
                    Objects.requireNonNullElse(this.config.getRestHookBaseUrl(), "") + arg.getHook()
                    : arg.getHook();

            var fwdHeaders = new HttpHeaders();
            fwdHeaders.setContentType(MediaType.APPLICATION_JSON);
            fwdHeaders.set(EngineConstant.SESSION_ID_HEADER_KEY, arg.getSessionId());

            var restUserAuthKey = arg.getSession().get(arg.getSessionId(), SessionConstant.REST_HOOK_USER_AUTH_KEY, String.class);

            if(restUserAuthKey != null) {
                fwdHeaders.setBearerAuth(restUserAuthKey);
            } else {
                if(config.getRestHookAuthToken() != null) {
                    fwdHeaders.set("Authorization", config.getRestHookAuthToken());
                }
            }

            ResponseEntity<String> response = this.client.post(url, restArg, fwdHeaders);

            if(response.getStatusCodeValue() == 200) {
                var responseHook = SerializeUtils.castValue(SerializeUtils.toMap(response.getBody()), BaseHook.class);
                return this.dtoMapper.map(responseHook);
            } else {
                log.warn("hook call error response, body: {}", response.getBody());
                throw new InternalException("There was a problem in hook endpoint call.");
            }
        } catch (Exception err) {
            log.error("hook call exception: {} | msg: {}", err.getClass().getSimpleName(), err.getMessage());
            throw new InternalException("failed to process hook REST request", err);
        }
    }

    Hook processReflectiveHook(Hook arg) throws Exception {
        // e.g., "com.myapp.MyHookClass.process"
        int lastDot = arg.getHook().lastIndexOf('.');
        if(lastDot == -1) {
            throw new InternalException("Invalid hook path: " + arg.getHook());
        }

        var classNamePath = arg.getHook().substring(0, lastDot);
        var methodName = arg.getHook().substring(lastDot + 1);

        Class<?> hookClass;
        Object hookObj;

        try {
            hookClass = Class.forName(classNamePath);

            try {
                hookObj = this.applicationContext.getBean(hookClass);
                log.debug("Loaded hook bean from Spring context: {}", classNamePath);
            } catch (BeansException ex) {
                log.warn("Spring bean not found, falling back to manual instantiation: {}", classNamePath);
                hookObj = createHookInstance(hookClass, arg);
            }

            var method = hookClass.getDeclaredMethod(methodName, Hook.class);
            var response = method.invoke(hookObj, arg);

            if(!(response instanceof Hook)) {
                throw new InternalException("Reflective hook must return a Hook, but got: " +
                        (response == null ? "null" : response.getClass().getName()));
            }

            return (Hook) response;
        } catch (Exception ex) {
            log.error("Error during reflective hook execution", ex);
            throw ex;
        }
    }

    Object createHookInstance(Class<?> hookClass, Hook arg) throws Exception {
        try {
            var ctor = hookClass.getDeclaredConstructor(Hook.class);
            return ctor.newInstance(arg);
        } catch (NoSuchMethodException e) {
            var instance = hookClass.getDeclaredConstructor().newInstance();
            try {
                var setter = hookClass.getMethod("setHookArg", Hook.class);
                setter.invoke(instance, arg);
                return instance;
            } catch (NoSuchMethodException nsme) {
                throw new InternalException(
                        "Hook class must have a constructor or setter accepting Hook: " + hookClass.getName(), nsme);
            }
        }
    }

    public Hook processHook(Hook arg) throws Exception {
        log.debug("PROCESSING HOOK ARG: {}", arg);
        log.debug("PROCESSING HOOK: {}", arg.getHook());

        if(this.isRestHook(arg.getHook())) {
            return this.processRestHook(arg);
        }

        return this.processReflectiveHook(arg);
    }

    @EventListener
    public void processOnceOffHook(OnceOffHookEvent event) {
        log.debug("PROCESSING ONCE OFF HOOK: {}", event.getArg().getHook());

        try {
            this.processHook(event.getArg());
        } catch (Exception e) {
            log.debug("OnceOffHookEvent processing failed: {}", e.getMessage());
        }
    }
}
