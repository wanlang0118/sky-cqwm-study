package com.sky.service;

import com.sky.dto.OrdersPaymentDTO;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.dto.OrdersRejectionDTO;
import com.sky.dto.OrdersCancelDTO;
import com.sky.result.PageResult;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;

/**
 * 订单相关服务
 */
public interface OrdersService {

    /**
     * 用户下单
     * @param ordersSubmitDTO
     * @return
     */
    OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO);

    /**
     * 订单支付
     * @param ordersPaymentDTO
     * @return
     */
    OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception;

    /**
     * 支付成功，修改订单状态
     * @param outTradeNo
     */
    void paySuccess(String outTradeNo);

    /**
     * 历史订单分页查询（用户端）
     * @param ordersPageQueryDTO
     * @return
     */
    PageResult historyOrders(OrdersPageQueryDTO ordersPageQueryDTO);

    /**
     * 订单详情（用户端）
     * @param id
     * @return
     */
    OrderVO getOrderDetail(Long id);

    /**
     * 取消订单（用户端）
     * @param id
     */
    void cancel(Long id);

    /**
     * 再来一单（将原订单明细重新加入购物车）
     * @param id
     */
    void repetition(Long id);

    /**
     * 催单（用户端）
     * @param id
     */
    void reminder(Long id);

    /**
     * 商家端订单搜索（条件+分页）
     * @param ordersPageQueryDTO
     * @return
     */
    PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO);

    /**
     * 各状态订单数量统计（商家端）
     * @return
     */
    com.sky.vo.OrderStatisticsVO statistics();

    /**
     * 接单（商家端）
     * @param id
     */
    void confirm(Long id);

    /**
     * 拒单（商家端）
     * @param ordersRejectionDTO
     */
    void rejection(OrdersRejectionDTO ordersRejectionDTO);

    /**
     * 取消订单（商家端）
     * @param ordersCancelDTO
     */
    void cancelByAdmin(OrdersCancelDTO ordersCancelDTO);

    /**
     * 派送订单（商家端）
     * @param id
     */
    void delivery(Long id);

    /**
     * 完成订单（商家端）
     * @param id
     */
    void complete(Long id);

    /**
     * 营业额统计（商家端）
     * @param begin
     * @param end
     * @return
     */
    com.sky.vo.TurnoverReportVO turnoverStatistics(java.time.LocalDate begin, java.time.LocalDate end);

    /**
     * 用户统计（商家端）
     * @param begin
     * @param end
     * @return
     */
    com.sky.vo.UserReportVO userStatistics(java.time.LocalDate begin, java.time.LocalDate end);

    /**
     * 订单统计（商家端）
     * @param begin
     * @param end
     * @return
     */
    com.sky.vo.OrderReportVO ordersStatistics(java.time.LocalDate begin, java.time.LocalDate end);

    /**
     * 销量排名Top10（商家端）
     * @param begin
     * @param end
     * @return
     */
    com.sky.vo.SalesTop10ReportVO top10(java.time.LocalDate begin, java.time.LocalDate end);

    /**
     * 订单详情（商家端）
     * @param id
     * @return
     */
    com.sky.vo.OrderVO getOrderDetailAdmin(Long id);
}
