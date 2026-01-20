package com.sky.controller.admin;

import com.sky.result.Result;
import com.sky.service.ShopService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 店铺营业状态管理（管理端）
 */
@RestController("adminShopController")
@RequestMapping("/admin/shop")
@Api(tags = "店铺营业状态相关接口")
@Slf4j
public class ShopController {

    @Autowired
    private ShopService shopService;

    /**
     * 设置店铺营业状态
     *
     * @param status 1：营业 0：打烊
     * @return
     */
    @PutMapping({"/status/{status}", "/{status}"})
    @ApiOperation("设置店铺营业状态")
    public Result<String> setStatus(@PathVariable Integer status) {
        log.info("设置店铺营业状态为：{}", status);
        shopService.setStatus(status);
        return Result.success();
    }

    /**
     * 查询店铺营业状态（管理端）
     *
     * @return
     */
    @GetMapping("/status")
    @ApiOperation("获取店铺营业状态（管理端）")
    public Result<Integer> getStatus() {
        Integer status = shopService.getStatus();
        return Result.success(status);
    }
}
