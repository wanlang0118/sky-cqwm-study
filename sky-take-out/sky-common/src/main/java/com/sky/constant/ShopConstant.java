package com.sky.constant;

/**
 * 店铺相关常量
 */
public class ShopConstant {

    /**
     * Redis中存储店铺营业状态的key
     */
    public static final String SHOP_STATUS_KEY = "SHOP_STATUS";

    /**
     * 1：营业，0：打烊
     */
    public static final Integer STATUS_OPEN = 1;
    public static final Integer STATUS_CLOSE = 0;
}
