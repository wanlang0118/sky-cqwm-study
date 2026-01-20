package com.sky.controller.user;

import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.ShoppingCart;
import com.sky.result.Result;
import com.sky.service.ShoppingCartService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户端-购物车接口
 */
@RestController
@RequestMapping("/user/shoppingCart")
@Api(tags = "用户端-购物车接口")
@Slf4j
public class ShoppingCartController {

    @Autowired
    private ShoppingCartService shoppingCartService;

    /**
     * 添加到购物车（菜品或套餐，存在则数量+1）
     * @param shoppingCartDTO
     * @return
     */
    @PostMapping("/add")
    @ApiOperation("添加到购物车")
    public Result<ShoppingCart> add(@RequestBody ShoppingCartDTO shoppingCartDTO){
        log.info("添加到购物车：{}", shoppingCartDTO);
        ShoppingCart shoppingCart = shoppingCartService.add(shoppingCartDTO);
        return Result.success(shoppingCart);
    }

    /**
     * 查看购物车
     * @return
     */
    @GetMapping("/list")
    @ApiOperation("查看购物车")
    public Result<List<ShoppingCart>> list(){
        return Result.success(shoppingCartService.showShoppingCart());
    }

    /**
     * 清空购物车
     */
    @DeleteMapping("/clean")
    @ApiOperation("清空购物车")
    public Result<String> clean(){
        shoppingCartService.cleanShoppingCart();
        return Result.success();
    }

    /**
     * 购物车减一/删除
     * @param shoppingCartDTO
     * @return
     */
    @PostMapping("/sub")
    @ApiOperation("购物车减一/删除")
    public Result<ShoppingCart> sub(@RequestBody ShoppingCartDTO shoppingCartDTO){
        log.info("购物车减一：{}", shoppingCartDTO);
        ShoppingCart cart = shoppingCartService.sub(shoppingCartDTO);
        return Result.success(cart);
    }
}
