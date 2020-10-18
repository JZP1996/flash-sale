package com.jzp.service;

/**
 * FileName:    CacheService
 * Author:      jzp
 * Date:        2020/6/11 16:47
 * Description: 封装本地缓存的业务层接口
 */
public interface CacheService {

    /**
     * 存放
     */
    void setCommonCache(String key, Object value);

    /**
     * 取
     */
    Object getFromCommonCache(String key);
}
