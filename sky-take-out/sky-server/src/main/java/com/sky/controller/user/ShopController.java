package com.sky.controller.user;

import com.sky.result.Result;
import com.sky.service.ShopService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 店铺营业状态查询（用户端）
 */
@RestController("userShopController")
@RequestMapping("/user/shop")
@Api(tags = "用户端-店铺营业状态接口")
public class ShopController {

    @Autowired
    private ShopService shopService;

    /**
     * 查询店铺营业状态（用户端）
     *
     * @return
     */
    @GetMapping("/status")
    @ApiOperation("获取店铺营业状态（用户端）")
    public Result<Integer> getStatus() {
        Integer status = shopService.getStatus();
        return Result.success(status);
    }
}
