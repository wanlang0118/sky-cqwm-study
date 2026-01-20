package com.sky.service.impl;

import com.sky.constant.ShopConstant;
import com.sky.service.ShopService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 店铺营业状态服务实现
 */
@Service
@Slf4j
public class ShopServiceImpl implements ShopService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 设置店铺营业状态
     *
     * @param status 1：营业 0：打烊
     */
    @Override
    public void setStatus(Integer status) {
        log.info("设置店铺营业状态为：{}", status);
        stringRedisTemplate.opsForValue().set(ShopConstant.SHOP_STATUS_KEY, String.valueOf(status));
    }

    /**
     * 获取店铺营业状态
     *
     * @return 1：营业 0：打烊
     */
    @Override
    public Integer getStatus() {
        String value = stringRedisTemplate.opsForValue().get(ShopConstant.SHOP_STATUS_KEY);
        Integer status = value == null ? ShopConstant.STATUS_CLOSE : Integer.valueOf(value);
        log.info("获取店铺营业状态为：{}", status);
        return status;
    }
}
