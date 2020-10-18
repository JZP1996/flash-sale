package com.jzp.service.impl;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.jzp.service.CacheService;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.concurrent.TimeUnit;

/**
 * FileName:    CacheServiceImpl
 * Author:      jzp
 * Date:        2020/6/11 16:49
 * Description: 封装本地缓存的业务层实现类
 */
@Service
public class CacheServiceImpl implements CacheService {

    private Cache<String, Object> commonCache;

    @PostConstruct
    public void init() {
        commonCache = CacheBuilder.newBuilder()
                /* 设置缓存容器的初始容量为 10 */
                .initialCapacity(10)
                /* 设置缓存容器的最大容量为 100，超过 100 会按照 LRU 策略移除 */
                .maximumSize(100)
                /* 设置写缓存后多少秒过期 */
                .expireAfterWrite(30, TimeUnit.SECONDS)
                .build();
    }

    @Override
    public void setCommonCache(String key, Object value) {
        commonCache.put(key, value);
    }

    @Override
    public Object getFromCommonCache(String key) {
        return commonCache.getIfPresent(key);
    }
}
