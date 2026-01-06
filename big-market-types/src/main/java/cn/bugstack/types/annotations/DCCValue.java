package cn.bugstack.types.annotations;

import java.lang.annotation.*;

/**
 * @Author: coderLan
 * @Description: DCCValue 注解
 * @DateTime: 2026-01-06 19:14
 **/
@Retention(RetentionPolicy.RUNTIME)
@Target( {ElementType.FIELD})
@Documented
public @interface DCCValue {

    String value() default "";
}
