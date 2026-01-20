package com.sky.service;

import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.ShoppingCart;

import java.util.List;

/**
 * 购物车相关服务
 */
public interface ShoppingCartService {

    /**
     * 添加到购物车（菜品或套餐），存在则数量+1
     * @param shoppingCartDTO
     * @return 最新的购物车条目
     */
    ShoppingCart add(ShoppingCartDTO shoppingCartDTO);

	/**
     * 查看购物车
     * @return
     */
    List<ShoppingCart> showShoppingCart();

    /**
     * 清空购物车
     */
    void cleanShoppingCart();

    /**
     * 购物车减一/删除
     * @param shoppingCartDTO
     * @return 减后条目（若被删空返回null）
     */
    ShoppingCart sub(ShoppingCartDTO shoppingCartDTO);
}
