package zw.co.dcl.jawce.engine.internal.dto;

import zw.co.dcl.jawce.engine.model.core.Hook;

import java.util.Collections;
import java.util.Map;

public final class HookResultMapper {
    private HookResultMapper() {
    }

    public static Hook merge(Hook hook, Hook fallback) {
        if(hook == null) {
            return fallback;
        }

        if(fallback == null) {
            return hook;
        }

        Hook merged = Hook.builder().build();
        merged.setSession(fallback.getSession());
        merged.setWaUser(fallback.getWaUser());
        merged.setSessionId(fallback.getSessionId());
        merged.setUserInput(fallback.getUserInput());
        merged.setFlow(fallback.getFlow());
        merged.setAdditionalData(fallback.getAdditionalData());
        merged.setTemplateDynamicBody(fallback.getTemplateDynamicBody());
        merged.setFromTrigger(fallback.isFromTrigger());
        merged.setHook(fallback.getHook());
        merged.setRedirectTo(fallback.getRedirectTo());
        merged.setParams(fallback.getParams() == null ? new java.util.HashMap<>() : new java.util.HashMap<>(fallback.getParams()));

        if(hook.getSession() != null) merged.setSession(hook.getSession());
        if(hook.getWaUser() != null) merged.setWaUser(hook.getWaUser());
        if(hook.getSessionId() != null) merged.setSessionId(hook.getSessionId());
        if(hook.getUserInput() != null) merged.setUserInput(hook.getUserInput());
        if(hook.getFlow() != null) merged.setFlow(hook.getFlow());
        if(hook.getAdditionalData() != null) merged.setAdditionalData(hook.getAdditionalData());
        if(hook.getTemplateDynamicBody() != null) merged.setTemplateDynamicBody(hook.getTemplateDynamicBody());
        if(hook.isFromTrigger()) merged.setFromTrigger(true);
        if(hook.getHook() != null) merged.setHook(hook.getHook());
        if(hook.getRedirectTo() != null) merged.setRedirectTo(hook.getRedirectTo());
        if(hook.getParams() != null && !hook.getParams().isEmpty()) {
            Map<String, Object> params = merged.getParams() == null ? new java.util.HashMap<>() : new java.util.HashMap<>(merged.getParams());
            params.putAll(hook.getParams());
            merged.setParams(params);
        }

        return merged;
    }

    public static String redirectTo(Hook hook) {
        return hook == null ? null : hook.getRedirectTo();
    }

    public static GenerateHookResult toGenerateResult(Hook hook) {
        if(hook == null || hook.getTemplateDynamicBody() == null) {
            return new GenerateHookResult(
                    hook,
                    null,
                    Collections.emptyMap(),
                    Collections.emptyMap(),
                    Collections.emptyList()
            );
        }

        Map<String, Object> renderPayload = hook.getTemplateDynamicBody().getRenderPayload();
        Map<String, Object> flowPayload = hook.getTemplateDynamicBody().getFlowPayload();

        return new GenerateHookResult(
                hook,
                hook.getTemplateDynamicBody().getTemplate(),
                renderPayload,
                flowPayload,
                hook.getTemplateDynamicBody().getDynamicChoices()
        );
    }
}
