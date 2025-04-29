package zw.co.dcl.jawce.engine.service;

import org.mapstruct.factory.Mappers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import zw.co.dcl.jawce.engine.configs.JawceConfig;
import zw.co.dcl.jawce.engine.constants.EngineConstants;
import zw.co.dcl.jawce.engine.constants.SessionConstants;
import zw.co.dcl.jawce.engine.exceptions.EngineInternalException;
import zw.co.dcl.jawce.engine.model.core.HookArg;
import zw.co.dcl.jawce.engine.model.core.HookArgRest;
import zw.co.dcl.jawce.engine.model.mappers.EngineDtoMapper;
import zw.co.dcl.jawce.engine.service.iface.IClientManager;
import zw.co.dcl.jawce.engine.utils.CommonUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

@Service
public class HookService {
    final Logger logger = LoggerFactory.getLogger(HookService.class);
    final EngineDtoMapper dtoMapper = Mappers.getMapper(EngineDtoMapper.class);

    final IClientManager client;
    final JawceConfig config;

    public HookService(JawceConfig config) {
        this.config = config;
        this.client = config.getClientManager();
    }

    HookArg processRestHook(HookArg arg) {
        // /api/fetch-all
        try {
            HookArgRest restArg = dtoMapper.map(arg);
            String url = this.config.getRestHookBaseUrl() + arg.getHook();

            var fwdHeaders = new HttpHeaders();
            fwdHeaders.setContentType(MediaType.APPLICATION_JSON);
            fwdHeaders.set(EngineConstants.JAWCE_RHOOK_SESSION_HEADER_KEY, arg.getSessionId());

            var restUserAuthKey = arg.getSession().get(arg.getSessionId(), SessionConstants.REST_HOOK_USER_AUTH_KEY, String.class);

            if(restUserAuthKey != null) {
                fwdHeaders.setBearerAuth(restUserAuthKey);
            } else {
                if(config.getRestHookAuthToken() != null) {
                    fwdHeaders.set("Authorization", config.getRestHookAuthToken());
                }
            }

            ResponseEntity<String> response = this.client.post(url, restArg, fwdHeaders);

            if(response.getStatusCodeValue() == 200) {
                var responseHook = CommonUtils.convertResponseToHookObj(response.getBody());
                var responseHookArg = this.dtoMapper.map(responseHook);
                responseHookArg.setSession(arg.getSession());
                return responseHookArg;
            } else {
                logger.warn("hook call error response, body: {}", response.getBody());
                throw new EngineInternalException("There was a problem in hook endpoint call.");
            }
        } catch (Exception err) {
            logger.error("hook call exception: {} | msg: {}", err.getClass().getSimpleName(), err.getMessage());
            throw new EngineInternalException("failed to process hook REST request", err);
        }
    }

    HookArg processReflectiveHook(HookArg arg) throws Exception {
        // e.g., "com.myapp.MyHookClass.process"
        int lastDot = arg.getHook().lastIndexOf('.');
        if(lastDot == -1) {
            throw new EngineInternalException("Invalid hook path: " + arg.getHook());
        }

        String classNamePath = arg.getHook().substring(0, lastDot);
        String methodName = arg.getHook().substring(lastDot + 1);

        Class<?> hookClass;
        Object hookObj;

        try {
            hookClass = Class.forName(classNamePath);

            if(this.config.getContext() != null) {
                try {
                    hookObj = this.config.getContext().getBean(hookClass);
                    logger.debug("Loaded hook bean from Spring context: {}", classNamePath);
                } catch (BeansException ex) {
                    logger.warn("Spring bean not found, falling back to manual instantiation: {}", classNamePath);
                    hookObj = createHookInstance(hookClass, arg);
                }
            } else {
                hookObj = createHookInstance(hookClass, arg);
            }

            Method method = hookClass.getDeclaredMethod(methodName);
            Object response = method.invoke(hookObj);

            if(!(response instanceof HookArg)) {
                throw new EngineInternalException("Reflective hook must return a HookArg, but got: " +
                        (response == null ? "null" : response.getClass().getName()));
            }

            return (HookArg) response;

        } catch (Exception ex) {
            logger.error("Error during reflective hook execution", ex);
            throw ex;
        }
    }

    Object createHookInstance(Class<?> hookClass, HookArg arg) throws Exception {
        try {
            Constructor<?> ctor = hookClass.getDeclaredConstructor(HookArg.class);
            return ctor.newInstance(arg);
        } catch (NoSuchMethodException e) {
            Object instance = hookClass.getDeclaredConstructor().newInstance();
            try {
                Method setter = hookClass.getMethod("setHookArg", HookArg.class);
                setter.invoke(instance, arg);
                return instance;
            } catch (NoSuchMethodException nsme) {
                throw new EngineInternalException(
                        "Hook class must have a constructor or setter accepting HookArg: " + hookClass.getName(), nsme);
            }
        }
    }

    public HookArg processHook(HookArg arg) throws Exception {
        logger.debug("PROCESSING HOOK: {}", arg.getHook());

        if(arg.getHook().startsWith(EngineConstants.REST_HOOK_TAG)) {
            return this.processRestHook(arg);
        }

        return this.processReflectiveHook(arg);
    }
}
