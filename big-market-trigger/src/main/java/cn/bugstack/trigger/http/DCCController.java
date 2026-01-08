package cn.bugstack.trigger.http;

import cn.bugstack.api.IDCCService;
import cn.bugstack.api.dto.ActivityDrawResponseDTO;
import cn.bugstack.api.response.Response;
import cn.bugstack.types.enums.ResponseCode;
import cn.bugstack.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.data.Stat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;

/**
 * @Author: coderLan
 * @Description: DCCController
 * @DateTime: 2026-01-06 20:01
 **/
@Slf4j
@RestController
@CrossOrigin("${app.config.cross-origin}")
@RequestMapping("/api/${app.config.api-version}/raffle/dcc/")
public class DCCController implements IDCCService {

    @Autowired(required = false)
    private CuratorFramework client;

    private static final String BASE_CONFIG_PATH = "/big-market-dcc";

    private static final String BASE_CONFIG_PATH_CONFIG = BASE_CONFIG_PATH + "/config";

    /**
     * 更新配置
     * <p>
     * curl http://localhost:8091/api/v1/raffle/dcc/update_config?key=degradeSwitch&value=open
     */
    @RequestMapping(value = "update_config", method = RequestMethod.GET)
    @Override
    public Response<Boolean> updateConfig(@RequestParam String key, @RequestParam String value){
        try {
            log.info("更新配置开始 key:{}, value:{}", key, value);
            if(null == client){
                log.warn("DCC 动态配置值变更拒绝，CuratorFramework 未初始化启动「配置未开启」 key:{} value:{}", key, value);
                return Response.<Boolean>builder()
                        .code(ResponseCode.UN_ERROR.getCode())
                        .info(ResponseCode.UN_ERROR.getInfo())
                        .build();
            }
            // 1. 参数检验
            if(StringUtils.isBlank(key) || StringUtils.isBlank(value)){
                throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), ResponseCode.ILLEGAL_PARAMETER.getInfo());
            }

            // 2. 创建节点
            String keyPath = BASE_CONFIG_PATH_CONFIG + "/" + key;
            if(null == client.checkExists().forPath(keyPath)){
                client.create().creatingParentsIfNeeded().forPath(keyPath);
                log.info("DCC 节点监听 base node {} not absent create new done!", keyPath);
            }
            // 3. 更新节点数据
            Stat stat = client.setData().forPath(keyPath, value.getBytes(StandardCharsets.UTF_8));
            log.info("更新配置完成 key:{}, value:{}, time:{}", key, value, stat.getCtime());
            return Response.<Boolean>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(true)
                    .build();
        } catch(AppException  e){
            log.error("更新配置异常 key:{}, value:{}", key, value);
            return Response.<Boolean>builder()
                    .code(e.getCode())
                    .info(e.getInfo())
                    .data(false)
                    .build();
        }catch (Exception e){
            log.error("更新配置异常 key:{}, value:{}", key, value);
            return Response.<Boolean>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(ResponseCode.UN_ERROR.getInfo())
                    .build();
        }
    }
}
