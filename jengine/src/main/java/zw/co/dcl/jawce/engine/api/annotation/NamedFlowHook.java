package zw.co.dcl.jawce.engine.api.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface NamedFlowHook {
    String value();
    FlowHookType type() default FlowHookType.GENERIC;
}
