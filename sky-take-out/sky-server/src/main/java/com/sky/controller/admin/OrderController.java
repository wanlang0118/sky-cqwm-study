package com.sky.controller.admin;

import com.sky.dto.OrdersPageQueryDTO;
import com.sky.dto.OrdersConfirmDTO;
import com.sky.dto.OrdersRejectionDTO;
import com.sky.dto.OrdersCancelDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.OrdersService;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/order")
@Api(tags = "管理端-订单管理")
@Slf4j
public class OrderController {

    @Autowired
    private OrdersService ordersService;

    /**
     * 订单搜索（分页/条件）
     * @param page
     * @param pageSize
     * @param number
     * @param phone
     * @param status
     * @param beginTime
     * @param endTime
     * @return
     */
    @GetMapping("/conditionSearch")
    @ApiOperation("订单搜索")
    public Result<PageResult> conditionSearch(Integer page,
                                              Integer pageSize,
                                              String number,
                                              String phone,
                                              Integer status,
                                              String beginTime,
                                              String endTime) {
        OrdersPageQueryDTO dto = new OrdersPageQueryDTO();
        dto.setPage(page == null ? 1 : page);
        dto.setPageSize(pageSize == null ? 10 : pageSize);
        dto.setNumber(number);
        dto.setPhone(phone);
        dto.setStatus(status);
        dto.setBeginTime(beginTime == null || beginTime.isEmpty() ? null : java.time.LocalDateTime.parse(beginTime));
        dto.setEndTime(endTime == null || endTime.isEmpty() ? null : java.time.LocalDateTime.parse(endTime));
        PageResult pageResult = ordersService.conditionSearch(dto);
        return Result.success(pageResult);
    }

    /**
    * 各状态订单数量统计
    */
    @GetMapping("/statistics")
    @ApiOperation("各状态订单数量统计")
    public Result<OrderStatisticsVO> statistics(){
        OrderStatisticsVO vo = ordersService.statistics();
        return Result.success(vo);
    }

    /**
     * 接单
     */
    @PutMapping("/confirm")
    @ApiOperation("接单")
    public Result<Void> confirm(@RequestBody OrdersConfirmDTO dto){
        ordersService.confirm(dto.getId());
        return Result.success();
    }

    /**
     * 拒单
     */
    @PutMapping("/rejection")
    @ApiOperation("拒单")
    public Result<Void> rejection(@RequestBody OrdersRejectionDTO dto){
        ordersService.rejection(dto);
        return Result.success();
    }

    /**
     * 取消订单
     */
    @PutMapping("/cancel")
    @ApiOperation("取消订单")
    public Result<Void> cancel(@RequestBody OrdersCancelDTO dto){
        ordersService.cancelByAdmin(dto);
        return Result.success();
    }

    /**
     * 派送订单
     */
    @PutMapping("/delivery/{id}")
    @ApiOperation("派送订单")
    public Result<Void> delivery(@PathVariable Long id){
        ordersService.delivery(id);
        return Result.success();
    }

    /**
     * 完成订单
     */
    @PutMapping("/complete/{id}")
    @ApiOperation("完成订单")
    public Result<Void> complete(@PathVariable Long id){
        ordersService.complete(id);
        return Result.success();
    }

    /**
     * 订单详情
     * @param id
     * @return
     */
    @GetMapping("/details/{id}")
    @ApiOperation("查询订单详情")
    public Result<OrderVO> details(@PathVariable Long id){
        OrderVO vo = ordersService.getOrderDetailAdmin(id);
        return Result.success(vo);
    }
}
