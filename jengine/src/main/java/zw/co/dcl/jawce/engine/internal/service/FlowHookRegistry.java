package zw.co.dcl.jawce.engine.internal.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.ApplicationContext;
import zw.co.dcl.jawce.engine.api.annotation.NamedFlowHook;
import zw.co.dcl.jawce.engine.api.exceptions.InternalException;
import zw.co.dcl.jawce.engine.api.iface.hook.IGenerateHook;
import zw.co.dcl.jawce.engine.api.iface.hook.IGenericHook;
import zw.co.dcl.jawce.engine.api.iface.hook.IDynamicHook;
import zw.co.dcl.jawce.engine.api.iface.hook.IMiddlewareHook;
import zw.co.dcl.jawce.engine.api.iface.hook.IReceiveHook;
import zw.co.dcl.jawce.engine.api.iface.hook.IRouterHook;
import zw.co.dcl.jawce.engine.api.iface.hook.ITemplateHook;
import zw.co.dcl.jawce.engine.model.core.Hook;

import java.lang.reflect.Method;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
public class FlowHookRegistry {
    private final Map<HookExecutionType, Map<String, RegisteredHookInvoker>> registries = new EnumMap<>(HookExecutionType.class);

    public FlowHookRegistry(ApplicationContext applicationContext) {
        for(HookExecutionType type : HookExecutionType.values()) {
            registries.put(type, new LinkedHashMap<>());
        }

        registerAll(applicationContext.getBeansOfType(IReceiveHook.class), HookExecutionType.RECEIVE, bean -> bean::execute);
        registerAll(applicationContext.getBeansOfType(IGenerateHook.class), HookExecutionType.GENERATE, bean -> bean::execute);
        registerAll(applicationContext.getBeansOfType(IDynamicHook.class), HookExecutionType.DYNAMIC, bean -> bean::execute);
        registerAll(applicationContext.getBeansOfType(IRouterHook.class), HookExecutionType.ROUTER, bean -> bean::execute);
        registerAll(applicationContext.getBeansOfType(IMiddlewareHook.class), HookExecutionType.MIDDLEWARE, bean -> bean::execute);
        registerAll(applicationContext.getBeansOfType(ITemplateHook.class), HookExecutionType.TEMPLATE, bean -> bean::execute);
        registerAll(applicationContext.getBeansOfType(IGenericHook.class), HookExecutionType.GENERIC, bean -> bean::execute);
        registerAnnotatedMethods(applicationContext);
    }

    public boolean hasHook(HookExecutionType type, String name) {
        return registries.getOrDefault(type, Map.of()).containsKey(name);
    }

    public boolean hasAnyHook(String name) {
        return findAny(name).isPresent();
    }

    public Optional<RegisteredHookInvoker> find(HookExecutionType type, String name) {
        return Optional.ofNullable(registries.getOrDefault(type, Map.of()).get(name));
    }

    public Optional<RegisteredHookInvoker> findAny(String name) {
        RegisteredHookInvoker match = null;

        for(Map<String, RegisteredHookInvoker> hooks : registries.values()) {
            if(hooks.containsKey(name)) {
                if(match != null) {
                    throw new InternalException("Ambiguous named hook '" + name + "' across multiple hook types");
                }
                match = hooks.get(name);
            }
        }

        return Optional.ofNullable(match);
    }

    public int totalHooks() {
        return registries.values().stream().mapToInt(Map::size).sum();
    }

    private <T> void registerAll(Map<String, T> beans, HookExecutionType type, CheckedInvokerFactory<T> invokerFactory) {
        beans.forEach((beanName, bean) -> register(beanName, bean, type, invokerFactory));
    }

    private <T> void register(String beanName, T bean, HookExecutionType type, CheckedInvokerFactory<T> invokerFactory) {
        String hookName = resolveHookName(beanName, bean.getClass());
        registerPlan(hookName, type, hook -> invokerFactory.create(bean).invoke(hook));
    }

    private void registerAnnotatedMethods(ApplicationContext applicationContext) {
        for(String beanName : applicationContext.getBeanDefinitionNames()) {
            Class<?> beanType = applicationContext.getType(beanName);
            if(beanType == null) {
                continue;
            }

            for(Method method : beanType.getDeclaredMethods()) {
                NamedFlowHook annotation = method.getAnnotation(NamedFlowHook.class);
                if(annotation == null) {
                    continue;
                }

                if(annotation.value() == null || annotation.value().isBlank()) {
                    throw new InternalException("Named flow hook method must declare a non-blank name on " +
                            beanType.getName() + "." + method.getName());
                }

                HookExecutionType type = HookExecutionType.from(annotation.type());
                Method validatedMethod = resolveMethodHook(beanName, method, applicationContext);
                HookMethodKind methodKind = resolveMethodKind(beanType, validatedMethod);

                registerPlan(annotation.value().trim(), type, hook -> invokeRegisteredMethod(applicationContext, beanName, validatedMethod, methodKind, hook));
            }
        }
    }

    private void registerPlan(String hookName, HookExecutionType type, HookInvocationPlan invocationPlan) {
        Map<String, RegisteredHookInvoker> typedRegistry = registries.get(type);

        if(typedRegistry.containsKey(hookName)) {
            throw new InternalException("Duplicate named hook '" + hookName + "' for type " + type);
        }

        typedRegistry.put(hookName, new RegisteredHookInvoker(
                hookName,
                type,
                invocationPlan
        ));
        log.debug("Registered named hook '{}' for type {}", hookName, type);
    }

    private Method resolveMethodHook(String beanName, Method method, ApplicationContext applicationContext) {
        Object bean = applicationContext.getBean(beanName);
        Class<?> targetClass = AopUtils.getTargetClass(bean);

        try {
            Method invocableMethod = targetClass.getDeclaredMethod(method.getName(), method.getParameterTypes());
            invocableMethod.setAccessible(true);

            if(!Hook.class.isAssignableFrom(invocableMethod.getReturnType())) {
                throw new InternalException("Named flow hook method must return Hook: " +
                        targetClass.getName() + "." + invocableMethod.getName());
            }

            return invocableMethod;
        } catch (NoSuchMethodException e) {
            throw new InternalException("Could not resolve named flow hook method " +
                    beanName + "." + method.getName(), e);
        }
    }

    private HookMethodKind resolveMethodKind(Class<?> beanType, Method method) {
        if(method.getParameterCount() == 0) {
            return HookMethodKind.NO_ARG;
        }

        if(method.getParameterCount() == 1 && Hook.class.equals(method.getParameterTypes()[0])) {
            return HookMethodKind.HOOK_ARG;
        }

        throw new InternalException("Named flow hook method must declare no args or a single Hook arg: " +
                beanType.getName() + "." + method.getName());
    }

    private Hook invokeRegisteredMethod(
            ApplicationContext applicationContext,
            String beanName,
            Method method,
            HookMethodKind methodKind,
            Hook hook
    ) throws Exception {
        Object bean = applicationContext.getBean(beanName);
        Object response = methodKind == HookMethodKind.HOOK_ARG
                ? method.invoke(bean, hook)
                : method.invoke(bean);

        if(!(response instanceof Hook hookResponse)) {
            throw new InternalException("Named flow hook must return Hook, but got: " +
                    (response == null ? "null" : response.getClass().getName()));
        }

        return hookResponse;
    }

    private String resolveHookName(String beanName, Class<?> beanClass) {
        NamedFlowHook annotation = beanClass.getAnnotation(NamedFlowHook.class);
        if(annotation != null && annotation.value() != null && !annotation.value().isBlank()) {
            return annotation.value().trim();
        }

        if(beanName == null || beanName.isBlank()) {
            throw new InternalException("Invalid flow hook bean name for class " + beanClass.getName());
        }

        return beanName;
    }

    @FunctionalInterface
    interface CheckedHookInvoker {
        Hook invoke(Hook hook) throws Exception;
    }

    @FunctionalInterface
    interface CheckedInvokerFactory<T> {
        CheckedHookInvoker create(T bean);
    }

    enum HookMethodKind {
        HOOK_ARG,
        NO_ARG
    }

    public record RegisteredHookInvoker(
            String name,
            HookExecutionType type,
            HookInvocationPlan invoker
    ) {
        public Hook invoke(Hook hook) throws Exception {
            return this.invoker.invoke(hook);
        }
    }
}
