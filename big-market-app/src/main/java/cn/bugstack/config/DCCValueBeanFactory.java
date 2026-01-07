package cn.bugstack.config;

import cn.bugstack.types.annotations.DCCValue;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.CuratorCache;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * @Author: coderLan
 * @Description: 实现BeanPostProcessor，拦截@DCCValue注解
 * @DateTime: 2026-01-06 19:17
 **/
@Slf4j
@Configuration
public class DCCValueBeanFactory implements BeanPostProcessor {

    private static final String BASE_CONFIG_PATH = "/big-market-dcc";

    private static final String BASE_CONFIG_PATH_CONFIG = "/big-market-dcc/config";

    // ZOOKEEPER CLIENT
    private final CuratorFramework client;

    // 缓存：Zookeeper节点路径 → 对应的Bean对象，用于节点变更时快速找到要更新的Bean
    private final Map<String, Object> dccObjGroup = new HashMap<>();

    // 构造器注入
    public DCCValueBeanFactory(CuratorFramework client) throws Exception {
        this.client = client;

        // 1. 创建配置节点
        if (null == client.checkExists().forPath(BASE_CONFIG_PATH_CONFIG)) {
            client.create().creatingParentsIfNeeded().forPath(BASE_CONFIG_PATH_CONFIG);
            log.info("DCC 节点监听 base node {} not absent create new done", BASE_CONFIG_PATH_CONFIG);
        }

        // 2. 创建CuratorCache：Curator的缓存/监听工具，监听指定节点下的所有变更
        CuratorCache curatorCache = CuratorCache.build(client, BASE_CONFIG_PATH_CONFIG);
        // 启动缓存
        curatorCache.start();

        // 3. 添加监听器
        curatorCache.listenable().addListener((type, oldData, data) -> {
            switch (type) {
                case NODE_CHANGED:
                    // 触发节点变更事件
                    String dccValuePath = data.getPath();
                    Object objBean = dccObjGroup.get(dccValuePath);
                    if (null == objBean) {
                        return;
                    }

                    try {
                        Class<?> objBeanClass = objBean.getClass();
                        // 检查objBean是否是代理对象
                        if(AopUtils.isAopProxy(objBean)){
                            objBeanClass = AopUtils.getTargetClass(objBean);
                        }
                        String fieldName = dccValuePath.substring(dccValuePath.lastIndexOf("/") + 1);
                        // 反射获取Bean的字段
                        Field filed = objBeanClass.getDeclaredField(fieldName);
                        filed.setAccessible(true);
                        filed.set(objBean, new String(data.getData()));
                        filed.setAccessible(false);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    break;
                default:
                    break;
            }
        });
    }

    /**
     * Bean初始化后，对有@DCCValue注解的字段进行赋值
     * @param bean
     * @param beanName
     * @return 创建的Bean对象
     * @throws BeansException
     */
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        // 反射获取属性
        Class<?> targetBeanClass = bean.getClass();
        Object targetBeanObject = bean;
        if(AopUtils.isAopProxy(bean)){
            targetBeanClass = AopUtils.getTargetClass(bean);
            targetBeanObject = AopProxyUtils.getSingletonTarget( bean);
        }

        Field[] fields = targetBeanClass.getDeclaredFields();
        for (Field field : fields) {
            // 如果存在@DCCValue注解
            if(field.isAnnotationPresent(DCCValue.class)){
                DCCValue dccValue = field.getAnnotation(DCCValue.class);
                String value = dccValue.value();

                if (null == value || "".equals(value)) {
                    throw new RuntimeException(field.getName() + " @DCCValue is not config value config case 「isSwitch:true」");
                }

                String[] splits = value.split(":");
                String key = splits[0];
                String defaultValue = splits.length == 2 ? splits[1] : null;

                try {
                    // 判断当前节点是否存在
                    String keyPath = BASE_CONFIG_PATH_CONFIG.concat("/").concat( key);
                    if(null == client.checkExists().forPath(keyPath)){
                        // 1. 情况1：节点不存在，创建节点，并设置默认值
                        client.create().creatingParentsIfNeeded().forPath(keyPath);
                        if(StringUtils.isNotBlank(defaultValue)){
                            field.setAccessible(true);
                            field.set(targetBeanObject, defaultValue);
                            field.setAccessible(false);
                        }
                        log.info("DCC 节点监听 创建节点：{}", keyPath);
                    }else {
                        // 2. 情况2：节点存在，根据节点值进行赋值
                        String configValue = new String(client.getData().forPath(keyPath));
                        if(StringUtils.isNotBlank(configValue)){
                            field.setAccessible(true);
                            field.set(targetBeanObject, configValue);
                            field.setAccessible(false);
                        }
                        log.info("DCC 节点监听 设置配置 {} {} {}", keyPath, field.getName(), configValue);
                    }
                }catch (Exception e) {
                    throw new RuntimeException(e);
                }

                // 装配keyPath和Bean对象的映射到缓存中
                dccObjGroup.put(BASE_CONFIG_PATH_CONFIG.concat("/").concat(key), bean);
            }
        }
        return bean;
    }
}
