package com.sky.service;

/**
 * 店铺营业状态服务
 */
public interface ShopService {

    /**
     * 设置店铺营业状态
     * @param status 1：营业 0：打烊
     */
    void setStatus(Integer status);

    /**
     * 获取店铺营业状态
     * @return 1：营业 0：打烊
     */
    Integer getStatus();
}
