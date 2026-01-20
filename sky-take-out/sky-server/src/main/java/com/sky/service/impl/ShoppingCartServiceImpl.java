package com.sky.service.impl;

import com.sky.context.BaseContext;
import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.ShoppingCart;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.mapper.ShoppingCartMapper;
import com.sky.service.ShoppingCartService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 购物车服务实现
 */
@Service
@Slf4j
public class ShoppingCartServiceImpl implements ShoppingCartService {

    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private SetmealMapper setmealMapper;

    /**
     * 添加到购物车（菜品或套餐），存在则数量+1
     *
     * @param shoppingCartDTO
     * @return
     */
    @Override
    public ShoppingCart add(ShoppingCartDTO shoppingCartDTO) {
        Long userId = BaseContext.getCurrentId();
        // 按需求去掉登录强校验，若上下文无用户则使用默认用户占位，避免报“未登录”
        if (userId == null) {
            userId = 1L;
            log.info("未登录");
        }

        ShoppingCart sc = new ShoppingCart();
        sc.setUserId(userId);
        sc.setDishId(shoppingCartDTO.getDishId());
        sc.setSetmealId(shoppingCartDTO.getSetmealId());
        sc.setDishFlavor(shoppingCartDTO.getDishFlavor());

        // 查询是否已存在相同条目
        List<ShoppingCart> list = shoppingCartMapper.list(sc);
        if (list != null && !list.isEmpty()) {
            ShoppingCart exists = list.get(0);
            exists.setNumber(exists.getNumber() + 1);
            shoppingCartMapper.updateNumberById(exists);
            return exists;
        }

        // 新增
        sc.setNumber(1);
        sc.setCreateTime(LocalDateTime.now());

        if (shoppingCartDTO.getDishId() != null) {
            Dish dish = dishMapper.getById(shoppingCartDTO.getDishId());
            if (dish == null) {
                throw new ShoppingCartBusinessException("菜品不存在");
            }
            sc.setName(dish.getName());
            sc.setImage(dish.getImage());
            sc.setAmount(dish.getPrice() == null ? BigDecimal.ZERO : dish.getPrice());
        } else if (shoppingCartDTO.getSetmealId() != null) {
            Setmeal setmeal = setmealMapper.getById(shoppingCartDTO.getSetmealId());
            if (setmeal == null) {
                throw new ShoppingCartBusinessException("套餐不存在");
            }
            sc.setName(setmeal.getName());
            sc.setImage(setmeal.getImage());
            sc.setAmount(setmeal.getPrice() == null ? BigDecimal.ZERO : setmeal.getPrice());
        } else {
            throw new ShoppingCartBusinessException("未指定菜品或套餐");
        }

        shoppingCartMapper.insert(sc);
        return sc;
    }
    /**
     * 查看购物车
     * @return
     */
    @Override
    public List<ShoppingCart> showShoppingCart() {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            userId = 1L;
        }
        return shoppingCartMapper.list(ShoppingCart.builder()
                .userId(userId)
                .build());
    }

    /**
     * 清空购物车
     */
    @Override
    public void cleanShoppingCart() {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            userId = 1L;
        }
        shoppingCartMapper.deleteByUserId(userId);
    }

    /**
     * 购物车减一/删除
     * @param shoppingCartDTO
     * @return 减后条目（若删除则返回null）
     */
    @Override
    public ShoppingCart sub(ShoppingCartDTO shoppingCartDTO) {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            userId = 1L;
        }

        ShoppingCart query = new ShoppingCart();
        query.setUserId(userId);
        query.setDishId(shoppingCartDTO.getDishId());
        query.setSetmealId(shoppingCartDTO.getSetmealId());
        query.setDishFlavor(shoppingCartDTO.getDishFlavor());

        List<ShoppingCart> list = shoppingCartMapper.list(query);
        if (list == null || list.isEmpty()) {
            return null;
        }
        ShoppingCart cart = list.get(0);
        if (cart.getNumber() != null && cart.getNumber() > 1) {
            cart.setNumber(cart.getNumber() - 1);
            shoppingCartMapper.updateNumberById(cart);
            return cart;
        } else {
            shoppingCartMapper.deleteById(cart.getId());
            return null;
        }
    }
}
