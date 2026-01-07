package cn.bugstack.types.annotations;

import java.lang.annotation.*;

/**
 * @Author: coderLan
 * @Description: 访问限流切点注解
 * @DateTime: 2026-01-06 21:22
 **/
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
@Documented
public @interface RateLimiterAccessInterceptor {

    /** 用哪个字段作为拦截标识，未配置则默认走全部 */
    String key() default "all";

    /** 限制频次 */
    double permitsPerSecond() default 1;

    /** 黑名单拦截（多少次限制后加入黑名单）0 不限制 */
    double blacklistCount() default 0;

    /** 拦截后执行的方法 */
    String fallbackMethod() default "";
}
