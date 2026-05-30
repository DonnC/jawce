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
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class HookService {
    final EngineDtoMapper dtoMapper = Mappers.getMapper(EngineDtoMapper.class);
    final List<String> restHooksFlag = List.of("/", "http://", "https://");

    final IClientManager client;
    final JawceConfig config;
    final ApplicationContext applicationContext;
    final ConcurrentHashMap<String, ReflectiveHookPlan> reflectiveHookPlans = new ConcurrentHashMap<>();

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
        ReflectiveHookPlan plan = this.reflectiveHookPlans.computeIfAbsent(arg.getHook(), this::buildReflectiveHookPlan);
        Object hookObj = plan.instantiate(this.applicationContext, arg);
        Object response = plan.invoke(hookObj, arg);

        if(!(response instanceof Hook)) {
            throw new InternalException("Reflective hook must return a Hook, but got: " +
                    (response == null ? "null" : response.getClass().getName()));
        }

        return (Hook) response;
    }

    Object createHookInstance(Class<?> hookClass, Hook arg) throws Exception {
        // Try constructor(Hook)
        try {
            Constructor<?> ctor = hookClass.getDeclaredConstructor(Hook.class);
            ctor.setAccessible(true);
            return ctor.newInstance(arg);
        } catch (NoSuchMethodException e) {
            // Try no-arg constructor + setHook(Hook) setter
            try {
                Object instance = hookClass.getDeclaredConstructor().newInstance();
                try {
                    var setter = hookClass.getMethod("setHook", Hook.class);
                    setter.invoke(instance, arg);
                    return instance;
                } catch (NoSuchMethodException nsme) {
                    // No setter — but if the hook method accepts Hook as parameter,
                    // a plain instance is still usable. Return the no-arg instance.
                    return instance;
                }
            } catch (NoSuchMethodException | InstantiationException | IllegalAccessException ex) {
                throw new InternalException(
                        "Hook class must have a constructor or setter or field accepting Hook: " + hookClass.getName(), ex);
            }
        }
    }

    ReflectiveHookPlan buildReflectiveHookPlan(String hookPath) {
        try {
            int lastDot = hookPath.lastIndexOf('.');
            if(lastDot == -1) throw new InternalException("Invalid hook path: " + hookPath);

            var classNamePath = hookPath.substring(0, lastDot);
            var methodName = hookPath.substring(lastDot + 1);

            Class<?> hookClass = Class.forName(classNamePath);
            boolean springManaged = isSpringManaged(hookClass);
            Method hookMethod = resolveHookMethod(hookClass, methodName, hookPath);
            HookMethodKind methodKind = hookMethod.getParameterCount() == 1 ? HookMethodKind.HOOK_ARG : HookMethodKind.NO_ARG;

            Constructor<?> hookCtor = null;
            Method setter = null;
            ManualInstantiationKind instantiationKind = ManualInstantiationKind.NONE;

            if(!springManaged) {
                try {
                    hookCtor = hookClass.getDeclaredConstructor(Hook.class);
                    hookCtor.setAccessible(true);
                    instantiationKind = ManualInstantiationKind.CONSTRUCTOR_WITH_HOOK;
                } catch (NoSuchMethodException e) {
                    try {
                        hookCtor = hookClass.getDeclaredConstructor();
                        hookCtor.setAccessible(true);
                        try {
                            setter = hookClass.getMethod("setHook", Hook.class);
                            setter.setAccessible(true);
                            instantiationKind = ManualInstantiationKind.NO_ARG_WITH_SETTER;
                        } catch (NoSuchMethodException ignored) {
                            instantiationKind = ManualInstantiationKind.NO_ARG_ONLY;
                        }
                    } catch (NoSuchMethodException ex) {
                        throw new InternalException(
                                "Hook class must have a constructor or setter or field accepting Hook: " + hookClass.getName(), ex);
                    }
                }
            }

            return new ReflectiveHookPlan(
                    hookPath,
                    hookClass,
                    springManaged,
                    hookMethod,
                    methodKind,
                    hookCtor,
                    setter,
                    instantiationKind
            );
        } catch (InternalException e) {
            throw e;
        } catch (Exception e) {
            throw new InternalException("Failed to build hook plan for " + hookPath, e);
        }
    }

    Method resolveHookMethod(Class<?> hookClass, String methodName, String hookPath) {
        try {
            Method method = hookClass.getDeclaredMethod(methodName, Hook.class);
            method.setAccessible(true);
            return method;
        } catch (NoSuchMethodException e1) {
            try {
                Method method = hookClass.getDeclaredMethod(methodName);
                method.setAccessible(true);
                return method;
            } catch (NoSuchMethodException e2) {
                throw new InternalException("No suitable method found for hook: " + hookPath, e2);
            }
        }
    }

    boolean isSpringManaged(Class<?> hookClass) {
        try {
            this.applicationContext.getBean(hookClass);
            log.debug("Loaded hook bean plan from Spring context: {}", hookClass.getName());
            return true;
        } catch (BeansException ex) {
            log.warn("Spring bean not found, falling back to manual instantiation: {}", hookClass.getName());
            return false;
        }
    }

    int reflectiveHookPlanCacheSize() {
        return this.reflectiveHookPlans.size();
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

    enum HookMethodKind {
        HOOK_ARG,
        NO_ARG
    }

    enum ManualInstantiationKind {
        NONE,
        CONSTRUCTOR_WITH_HOOK,
        NO_ARG_WITH_SETTER,
        NO_ARG_ONLY
    }

    record ReflectiveHookPlan(
            String hookPath,
            Class<?> hookClass,
            boolean springManaged,
            Method method,
            HookMethodKind methodKind,
            Constructor<?> constructor,
            Method setter,
            ManualInstantiationKind instantiationKind
    ) {
        Object instantiate(ApplicationContext applicationContext, Hook arg) throws Exception {
            if(this.springManaged) {
                return applicationContext.getBean(this.hookClass);
            }

            return switch (this.instantiationKind) {
                case CONSTRUCTOR_WITH_HOOK -> this.constructor.newInstance(arg);
                case NO_ARG_WITH_SETTER -> {
                    Object instance = this.constructor.newInstance();
                    this.setter.invoke(instance, arg);
                    yield instance;
                }
                case NO_ARG_ONLY -> this.constructor.newInstance();
                case NONE -> throw new InternalException("No instantiation strategy found for hook: " + this.hookPath);
            };
        }

        Object invoke(Object hookObj, Hook arg) throws Exception {
            if(this.methodKind == HookMethodKind.HOOK_ARG) {
                return this.method.invoke(hookObj, arg);
            }
            return this.method.invoke(hookObj);
        }
    }
}
