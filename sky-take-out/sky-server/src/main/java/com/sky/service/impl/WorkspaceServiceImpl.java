package com.sky.service.impl;

import com.sky.constant.StatusConstant;
import com.sky.entity.Orders;
import com.sky.mapper.DishMapper;
import com.sky.mapper.OrdersMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.WorkspaceService;
import com.sky.vo.BusinessDataVO;
import com.sky.vo.DishOverViewVO;
import com.sky.vo.OrderOverViewVO;
import com.sky.vo.SetmealOverViewVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.util.CellRangeAddress;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class WorkspaceServiceImpl implements WorkspaceService {

    @Autowired
    private OrdersMapper orderMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private SetmealMapper setmealMapper;

    /**
     * 根据时间段统计营业数据
     * @param begin
     * @param end
     * @return
     */
    @Override
    public BusinessDataVO getBusinessData(LocalDateTime begin, LocalDateTime end) {
        /**
         * 营业额：当日已完成订单的总金额
         * 有效订单：当日已完成订单的数量
         * 订单完成率：有效订单数 / 总订单数
         * 平均客单价：营业额 / 有效订单数
         * 新增用户：当日新增用户的数量
         */

        Map<String, Object> orderCondition = new HashMap<>();
        orderCondition.put("begin", begin);
        orderCondition.put("end", end);

        //查询总订单数
        Integer totalOrderCount = orderMapper.countByMap(orderCondition);

        orderCondition.put("status", Orders.COMPLETED);
        //营业额
        Double turnover = orderMapper.sumByMap(orderCondition);
        turnover = turnover == null? 0.0 : turnover;

        //有效订单数
        Integer validOrderCount = orderMapper.countByMap(orderCondition);

        Double unitPrice = 0.0;

        Double orderCompletionRate = 0.0;
        if(totalOrderCount != 0 && validOrderCount != 0){
            //订单完成率
            orderCompletionRate = validOrderCount.doubleValue() / totalOrderCount;
            //平均客单价
            unitPrice = turnover / validOrderCount;
        }

        //新增用户数
        Map<String, Object> userCondition = new HashMap<>();
        userCondition.put("begin", begin);
        userCondition.put("end", end);
        Integer newUsers = userMapper.countByMap(userCondition);

        return BusinessDataVO.builder()
                .turnover(turnover)
                .validOrderCount(validOrderCount)
                .orderCompletionRate(orderCompletionRate)
                .unitPrice(unitPrice)
                .newUsers(newUsers)
                .build();
    }


    /**
     * 查询订单管理数据
     *
     * @return
     */
    @Override
    public OrderOverViewVO getOrderOverView() {
        Map<String, Object> map = new HashMap<>();
        map.put("begin", LocalDateTime.now().with(LocalTime.MIN));
        map.put("end", LocalDateTime.now().with(LocalTime.MAX));
        map.put("status", Orders.TO_BE_CONFIRMED);

        //待接单
        Integer waitingOrders = orderMapper.countByMap(map);

        //待派送
        map.put("status", Orders.CONFIRMED);
        Integer deliveredOrders = orderMapper.countByMap(map);

        //已完成
        map.put("status", Orders.COMPLETED);
        Integer completedOrders = orderMapper.countByMap(map);

        //已取消
        map.put("status", Orders.CANCELLED);
        Integer cancelledOrders = orderMapper.countByMap(map);

        //全部订单
        map.put("status", null);
        Integer allOrders = orderMapper.countByMap(map);

        return OrderOverViewVO.builder()
                .waitingOrders(waitingOrders)
                .deliveredOrders(deliveredOrders)
                .completedOrders(completedOrders)
                .cancelledOrders(cancelledOrders)
                .allOrders(allOrders)
                .build();
    }

    /**
     * 查询菜品总览
     *
     * @return
     */
    @Override
    public DishOverViewVO getDishOverView() {
        Map<String, Object> map = new HashMap<>();
        map.put("status", StatusConstant.ENABLE);
        Integer sold = dishMapper.countByMap(map);

        map.put("status", StatusConstant.DISABLE);
        Integer discontinued = dishMapper.countByMap(map);

        return DishOverViewVO.builder()
                .sold(sold)
                .discontinued(discontinued)
                .build();
    }

    /**
     * 查询套餐总览
     *
     * @return
     */
    @Override
    public SetmealOverViewVO getSetmealOverView() {
        Map<String, Object> map = new HashMap<>();
        map.put("status", StatusConstant.ENABLE);
        Integer sold = setmealMapper.countByMap(map);

        map.put("status", StatusConstant.DISABLE);
        Integer discontinued = setmealMapper.countByMap(map);

        return SetmealOverViewVO.builder()
                .sold(sold)
                .discontinued(discontinued)
                .build();
    }

    /**
     * 导出近30天运营数据Excel
     */
    @Override
    public void exportBusinessData(HttpServletResponse response) {
        LocalDate endDate = LocalDate.now().minusDays(1);
        LocalDate beginDate = endDate.minusDays(29);

        // 汇总近30天概览
        BusinessDataVO summary = this.getBusinessData(beginDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay());

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("运营数据");
        sheet.setDefaultColumnWidth(15);

        CellStyle centerStyle = workbook.createCellStyle();
        centerStyle.setAlignment(HorizontalAlignment.CENTER);

        // 标题
        Row titleRow = sheet.createRow(0);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("运营数据报表");
        titleCell.setCellStyle(centerStyle);
        sheet.addMergedRegion(new CellRangeAddress(0,0,0,5));

        // 时间范围
        Row rangeRow = sheet.createRow(1);
        rangeRow.createCell(0).setCellValue("时间范围");
        rangeRow.createCell(1).setCellValue(beginDate + " 至 " + endDate);
        sheet.addMergedRegion(new CellRangeAddress(1,1,1,5));

        // 概览表头
        Row overviewHeader = sheet.createRow(3);
        overviewHeader.createCell(0).setCellValue("营业额");
        overviewHeader.createCell(1).setCellValue("有效订单数");
        overviewHeader.createCell(2).setCellValue("订单完成率(%)");
        overviewHeader.createCell(3).setCellValue("平均客单价");
        overviewHeader.createCell(4).setCellValue("新增用户数");

        Row overviewData = sheet.createRow(4);
        overviewData.createCell(0).setCellValue(formatDouble(summary.getTurnover()));
        overviewData.createCell(1).setCellValue(summary.getValidOrderCount() == null ? 0 : summary.getValidOrderCount());
        overviewData.createCell(2).setCellValue(formatDouble(percent(summary.getOrderCompletionRate())));
        overviewData.createCell(3).setCellValue(formatDouble(summary.getUnitPrice()));
        overviewData.createCell(4).setCellValue(summary.getNewUsers() == null ? 0 : summary.getNewUsers());

        // 明细表头
        Row detailTitle = sheet.createRow(6);
        Cell detailTitleCell = detailTitle.createCell(0);
        detailTitleCell.setCellValue("明细数据");
        detailTitleCell.setCellStyle(centerStyle);
        sheet.addMergedRegion(new CellRangeAddress(6,6,0,5));

        Row detailHeader = sheet.createRow(7);
        detailHeader.createCell(0).setCellValue("日期");
        detailHeader.createCell(1).setCellValue("营业额");
        detailHeader.createCell(2).setCellValue("有效订单数");
        detailHeader.createCell(3).setCellValue("订单完成率(%)");
        detailHeader.createCell(4).setCellValue("平均客单价");
        detailHeader.createCell(5).setCellValue("新增用户数");

        int rowIndex = 8;
        for (LocalDate date = beginDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            BusinessDataVO dayData = this.getBusinessData(date.atStartOfDay(), date.plusDays(1).atStartOfDay());
            Row row = sheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(date.toString());
            row.createCell(1).setCellValue(formatDouble(dayData.getTurnover()));
            row.createCell(2).setCellValue(dayData.getValidOrderCount() == null ? 0 : dayData.getValidOrderCount());
            row.createCell(3).setCellValue(formatDouble(percent(dayData.getOrderCompletionRate())));
            row.createCell(4).setCellValue(formatDouble(dayData.getUnitPrice()));
            row.createCell(5).setCellValue(dayData.getNewUsers() == null ? 0 : dayData.getNewUsers());
        }

        // 输出
        try (ServletOutputStream out = response.getOutputStream()) {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            String fileName = URLEncoder.encode("运营数据报表.xlsx", "UTF-8");
            response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
            workbook.write(out);
            out.flush();
        } catch (IOException e) {
            log.error("导出运营数据失败", e);
        }
    }

    private String formatDouble(Double value) {
        if (value == null) return "0.00";
        return new BigDecimal(value.toString()).setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private Double percent(Double ratio) {
        if (ratio == null) return 0.0;
        return ratio * 100;
    }
}
