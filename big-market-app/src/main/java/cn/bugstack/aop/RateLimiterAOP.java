package cn.bugstack.aop;

import cn.bugstack.types.annotations.DCCValue;
import cn.bugstack.types.annotations.RateLimiterAccessInterceptor;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 * @Author: coderLan
 * @Description: 限速AOP
 * @DateTime: 2026-01-06 21:27
 **/
@Slf4j
@Aspect
@Component
public class RateLimiterAOP {

    // 动态配置，实现限速AOP的动态开关
    @DCCValue("rateLimiterSwitch:close")
    private String rateLimiterSwitch;

    // 个人限频一秒钟
    private final Cache<String, RateLimiter> loginRecord = CacheBuilder.newBuilder()
            .expireAfterWrite(1,TimeUnit.SECONDS)
            .build();

    // 个人限频黑名单24h - 分布式业务场景，可以记录到 Redis 中
    private final Cache<String, Long> blacklist = CacheBuilder.newBuilder()
            .expireAfterWrite(24, TimeUnit.HOURS)
            .build();

    @Pointcut("@annotation(cn.bugstack.types.annotations.RateLimiterAccessInterceptor)")
    public void aopPoint(){

    }

    @Around("aopPoint() && @annotation(rateLimiterAccessInterceptor)")
    public Object doRouter(ProceedingJoinPoint jp, RateLimiterAccessInterceptor rateLimiterAccessInterceptor) throws Throwable {
        // 步骤0：限流开关关闭，直接放行
        if(StringUtils.isBlank(rateLimiterSwitch) || "close".equals(rateLimiterSwitch)){
            return jp.proceed();
        }

        // 步骤1：获取注解中配置的“限流维度字段名”（如uId、orderId）
        String key = rateLimiterAccessInterceptor.key();
        if(StringUtils.isBlank(key)){
            throw new RuntimeException("annotation RateLimiter uId is null");
        }

        // 步骤二：从方法参数中取出限流维度的实际值，如userId = 1001
        String keyAttr = getAttrValue(key,jp.getArgs());
        log.info("限流key：{}", keyAttr);

        // 步骤三：黑名单拦截（超频次后24小时拦截）
        if(!"all".equals(keyAttr) && rateLimiterAccessInterceptor.blacklistCount() != 0 && null != blacklist.getIfPresent(keyAttr) && blacklist.getIfPresent(keyAttr) > rateLimiterAccessInterceptor.blacklistCount()){
            log.info("限流-黑名单拦截（24h）：{}", keyAttr);
            // 调用降级方返回结果
            return fallbackMethodResult(jp, rateLimiterAccessInterceptor.fallbackMethod());
        }

        // 步骤四：获取/创建令牌桶实例（从缓存取，无则新建）
        RateLimiter rateLimiter = loginRecord.getIfPresent(keyAttr);
        if(null == rateLimiter){
            // 创建令牌桶实例
            rateLimiter = RateLimiter.create(rateLimiterAccessInterceptor.permitsPerSecond());
            // 缓存令牌桶实例
            loginRecord.put(keyAttr, rateLimiter);
        }

        // 步骤五：尝试获取令牌
        if(!rateLimiter.tryAcquire()){
            // 令牌获取失败（触发限流），更新黑名单计数
            if (rateLimiterAccessInterceptor.blacklistCount() != 0) {
                blacklist.put(keyAttr, blacklist.getIfPresent(keyAttr) == null ? 1L : blacklist.getIfPresent(keyAttr) + 1L);
            }

            log.info("限流-超频次拦截：{}", keyAttr);
            // 调用降级方法返回结果
            return fallbackMethodResult(jp, rateLimiterAccessInterceptor.fallbackMethod());
        }

        // 步骤六：正常执行方法
        return jp.proceed();
    }

    private Object fallbackMethodResult(ProceedingJoinPoint jp, String fallbackMethod) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Signature jpSignature = jp.getSignature();
        MethodSignature methodSignature = (MethodSignature) jpSignature;
        // 获取降级方法（参数列表与原方法一致）
        Method method = jp.getTarget().getClass().getMethod(fallbackMethod, methodSignature.getParameterTypes());
        // 反射调用降级方法
        return method.invoke(jp.getThis(), jp.getArgs());
    }

    private String getAttrValue(String key, Object[] args) {
        // 特殊处理：若第一个参数是String，直接返回（如参数本身就是用户ID字符串）
        if (args[0] instanceof String) {
            return args[0].toString();
        }

        String fieldValue = null;
        for (Object arg : args) {
            try{
                if(StringUtils.isNotBlank(fieldValue)){
                    break;
                }
                fieldValue = String.valueOf(this.getValueByName(arg, key));
            }catch (Exception e){
                log.error("获取路由属性值失败 key：{}",key, e);
            }
        }
        return fieldValue;
    }

    // 通过反射从参数对象中获取属性值
    private Object getValueByName(Object arg, String key) {
        try{
            Field field = getFieldByName(arg, key);
            if(null ==  field){
                return null;
            }
            field.setAccessible(true);
            // 获取属性值
            Object o = field.get(arg);
            field.setAccessible( false);
            return o;
        } catch (IllegalAccessException e) {
            return null;
        }
    }

    private Field getFieldByName(Object arg, String key) {
        try {
            Field field = null;
            try {
                // 获取属性
                field = arg.getClass().getDeclaredField(key);
            }catch (NoSuchFieldException e){
                // 从父类中获取属性
                field = arg.getClass().getSuperclass().getDeclaredField(key);
            }
            return field;
        } catch (NoSuchFieldException e) {
            return null;
        }
    }
}
