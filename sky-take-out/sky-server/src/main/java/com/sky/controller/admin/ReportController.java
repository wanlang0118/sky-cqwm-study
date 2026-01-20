package com.sky.controller.admin;

import com.sky.result.Result;
import com.sky.service.OrdersService;
import com.sky.service.WorkspaceService;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/admin/report")
@Api(tags = "管理端-报表统计")
@Slf4j
public class ReportController {

    @Autowired
    private OrdersService ordersService;
    @Autowired
    private WorkspaceService workspaceService;

    /**
     * 营业额统计
     */
    @GetMapping("/turnoverStatistics")
    @ApiOperation("营业额统计")
    public Result<TurnoverReportVO> turnoverStatistics(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end) {
        log.info("营业额统计，begin={}, end={}", begin, end);
        TurnoverReportVO vo = ordersService.turnoverStatistics(begin, end);
        return Result.success(vo);
    }

    /**
     * 用户统计
     */
    @GetMapping("/userStatistics")
    @ApiOperation("用户统计")
    public Result<UserReportVO> userStatistics(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end) {
        log.info("用户统计，begin={}, end={}", begin, end);
        UserReportVO vo = ordersService.userStatistics(begin, end);
        return Result.success(vo);
    }

    /**
     * 订单统计
     */
    @GetMapping("/ordersStatistics")
    @ApiOperation("订单统计")
    public Result<OrderReportVO> ordersStatistics(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end) {
        log.info("订单统计，begin={}, end={}", begin, end);
        OrderReportVO vo = ordersService.ordersStatistics(begin, end);
        return Result.success(vo);
    }

    /**
     * 销量排名Top10
     */
    @GetMapping("/top10")
    @ApiOperation("销量排名Top10")
    public Result<SalesTop10ReportVO> top10(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end) {
        log.info("销量Top10，begin={}, end={}", begin, end);
        SalesTop10ReportVO vo = ordersService.top10(begin, end);
        return Result.success(vo);
    }

    /**
     * 导出近30天运营数据报表（Excel）
     */
    @GetMapping("/export")
    @ApiOperation("导出运营数据Excel报表")
    public void export(javax.servlet.http.HttpServletResponse response) {
        workspaceService.exportBusinessData(response);
    }
}
