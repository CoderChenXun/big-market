package cn.bugstack.api;

import cn.bugstack.api.response.Response;

/**
 * @Author: coderLan
 * @Description:
 * @DateTime: 2026-01-06 20:01
 **/
public interface IDCCService {

    Response<Boolean> updateConfig(String key, String value);
}
