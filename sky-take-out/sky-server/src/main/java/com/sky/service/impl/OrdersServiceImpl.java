package com.sky.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.sky.context.BaseContext;
import com.sky.dto.OrdersPaymentDTO;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.dto.OrdersRejectionDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.dto.OrdersCancelDTO;
import com.sky.entity.AddressBook;
import com.sky.entity.OrderDetail;
import com.sky.entity.Orders;
import com.sky.entity.ShoppingCart;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.*;
import com.sky.service.OrdersService;
import com.sky.result.PageResult;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;
import com.sky.websocket.WebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Map;
import java.util.HashMap;

@Service
@Slf4j
public class OrdersServiceImpl implements OrdersService {

    @Autowired
    private OrdersMapper ordersMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private AddressBookMapper addressBookMapper;
    @Autowired
    private WebSocketServer webSocketServer;
    @Autowired
    private UserMapper userMapper;

    /**
     * 用户下单
     */
    @Override
    @Transactional
    public OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO) {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            userId = 1L;
        }

        // 校验购物车
        List<ShoppingCart> cartList = shoppingCartMapper.list(ShoppingCart.builder().userId(userId).build());
        if (cartList == null || cartList.isEmpty()) {
            throw new ShoppingCartBusinessException("购物车为空，无法下单");
        }

        // 校验地址
        AddressBook address = addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());
        if (address == null) {
            throw new ShoppingCartBusinessException("地址不存在，无法下单");
        }

        LocalDateTime now = LocalDateTime.now();
        String orderNumber = UUID.randomUUID().toString().replace("-", "");

        // 计算金额（购物车合计 + 打包费）
        BigDecimal total = BigDecimal.ZERO;
        for (ShoppingCart sc : cartList) {
            BigDecimal line = sc.getAmount() == null ? BigDecimal.ZERO : sc.getAmount();
            if (sc.getNumber() != null) {
                line = line.multiply(new BigDecimal(sc.getNumber()));
            }
            total = total.add(line);
        }
        BigDecimal packAmount = ordersSubmitDTO.getPackAmount() == null ? BigDecimal.ZERO : new BigDecimal(ordersSubmitDTO.getPackAmount());
        total = total.add(packAmount);

        // 构建订单
        Orders orders = new Orders();
        orders.setNumber(orderNumber);
        orders.setStatus(Orders.TO_BE_CONFIRMED);
        orders.setUserId(userId);
        orders.setAddressBookId(ordersSubmitDTO.getAddressBookId());
        orders.setOrderTime(now);
        orders.setCheckoutTime(now);
        orders.setPayMethod(ordersSubmitDTO.getPayMethod());
        orders.setPayStatus(Orders.PAID);
        orders.setAmount(total);
        orders.setRemark(ordersSubmitDTO.getRemark());
        orders.setPhone(address.getPhone());
        orders.setAddress(formatAddress(address));
        orders.setConsignee(address.getConsignee());
        orders.setEstimatedDeliveryTime(ordersSubmitDTO.getEstimatedDeliveryTime());
        orders.setDeliveryStatus(ordersSubmitDTO.getDeliveryStatus());
        orders.setPackAmount(packAmount.intValue());
        orders.setTablewareNumber(ordersSubmitDTO.getTablewareNumber() == null ? 0 : ordersSubmitDTO.getTablewareNumber());
        orders.setTablewareStatus(ordersSubmitDTO.getTablewareStatus());

        ordersMapper.insert(orders);

        // 构建订单明细
        List<OrderDetail> details = new ArrayList<>();
        for (ShoppingCart sc : cartList) {
            OrderDetail detail = new OrderDetail();
            detail.setOrderId(orders.getId());
            detail.setName(sc.getName());
            detail.setImage(sc.getImage());
            detail.setDishId(sc.getDishId());
            detail.setSetmealId(sc.getSetmealId());
            detail.setDishFlavor(sc.getDishFlavor());
            detail.setNumber(sc.getNumber());
            detail.setAmount(sc.getAmount());
            details.add(detail);
        }
        orderDetailMapper.insertBatch(details);

        // 清空购物车
        shoppingCartMapper.deleteByUserId(userId);

        // 返回VO
        return OrderSubmitVO.builder()
                .id(orders.getId())
                .orderNumber(orders.getNumber())
                .orderAmount(orders.getAmount())
                .orderTime(orders.getOrderTime())
                .build();
    }

    /**
     * 订单支付（跳过微信预下单，直接返回空json，并调用paySuccess）
     */
    @Override
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            userId = 1L;
        }

        // 模拟微信预下单：直接返回空json
        JSONObject jsonObject = new JSONObject();

        if (jsonObject.getString("code") != null && "ORDERPAID".equals(jsonObject.getString("code"))) {
            throw new OrderBusinessException("该订单已支付");
        }

        OrderPaymentVO vo = new OrderPaymentVO();
        vo.setPackageStr(jsonObject.getString("package"));

        return vo;
    }

    /**
     * 订单详情（商家端）
     */
    @Override
    public OrderVO getOrderDetailAdmin(Long id) {
        Orders orders = ordersMapper.getOrdersById(id);
        if (orders == null) {
            throw new OrderBusinessException("订单不存在");
        }
        List<OrderDetail> details = orderDetailMapper.listByOrderId(orders.getId());
        return buildOrderVO(orders, details);
    }

    /**
     * 营业额统计（商家端）
     */
    @Override
    public TurnoverReportVO turnoverStatistics(LocalDate begin, LocalDate end) {
        // 生成日期序列
        List<LocalDate> dates = new ArrayList<>();
        LocalDate cursor = begin;
        while (!cursor.isAfter(end)) {
            dates.add(cursor);
            cursor = cursor.plusDays(1);
        }

        // 查询区间内已完成订单的日营业额
        List<Map<String, Object>> dbList = ordersMapper.sumAmountByDate(begin.atStartOfDay(), end.plusDays(1).atStartOfDay());
        Map<LocalDate, BigDecimal> turnoverMap = new HashMap<>();
        if (dbList != null) {
            for (Map<String, Object> row : dbList) {
                Object dateObj = row.get("orderDate");
                Object amountObj = row.get("turnover");
                if (dateObj != null) {
                    LocalDate d = LocalDate.parse(dateObj.toString());
                    BigDecimal amt = amountObj == null ? BigDecimal.ZERO : new BigDecimal(amountObj.toString());
                    turnoverMap.put(d, amt);
                }
            }
        }

        List<String> dateList = new ArrayList<>();
        List<String> turnoverList = new ArrayList<>();
        for (LocalDate d : dates) {
            dateList.add(d.toString());
            BigDecimal amt = turnoverMap.getOrDefault(d, BigDecimal.ZERO);
            turnoverList.add(amt.toString());
        }

        return TurnoverReportVO.builder()
                .dateList(String.join(",", dateList))
                .turnoverList(String.join(",", turnoverList))
                .build();
    }

    /**
     * 订单统计（商家端）
     */
    @Override
    public OrderReportVO ordersStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dates = new ArrayList<>();
        LocalDate cursor = begin;
        while (!cursor.isAfter(end)) {
            dates.add(cursor);
            cursor = cursor.plusDays(1);
        }

        // 全部订单每日数
        List<Map<String, Object>> allCounts = ordersMapper.countByDate(begin.atStartOfDay(), end.plusDays(1).atStartOfDay());
        Map<LocalDate, Integer> allMap = new HashMap<>();
        if (allCounts != null) {
            for (Map<String, Object> row : allCounts) {
                Object dateObj = row.get("orderDate");
                Object cntObj = row.get("cnt");
                if (dateObj != null) {
                    LocalDate d = LocalDate.parse(dateObj.toString());
                    Integer cnt = cntObj == null ? 0 : Integer.parseInt(cntObj.toString());
                    allMap.put(d, cnt);
                }
            }
        }

        // 有效订单（已完成）每日数
        List<Map<String, Object>> validCounts = ordersMapper.countByDateAndStatus(Orders.COMPLETED, begin.atStartOfDay(), end.plusDays(1).atStartOfDay());
        Map<LocalDate, Integer> validMap = new HashMap<>();
        if (validCounts != null) {
            for (Map<String, Object> row : validCounts) {
                Object dateObj = row.get("orderDate");
                Object cntObj = row.get("cnt");
                if (dateObj != null) {
                    LocalDate d = LocalDate.parse(dateObj.toString());
                    Integer cnt = cntObj == null ? 0 : Integer.parseInt(cntObj.toString());
                    validMap.put(d, cnt);
                }
            }
        }

        List<String> dateList = new ArrayList<>();
        List<String> orderCountList = new ArrayList<>();
        List<String> validOrderCountList = new ArrayList<>();
        int totalOrderCount = 0;
        int validOrderCount = 0;

        for (LocalDate d : dates) {
            int all = allMap.getOrDefault(d, 0);
            int valid = validMap.getOrDefault(d, 0);
            totalOrderCount += all;
            validOrderCount += valid;

            dateList.add(d.toString());
            orderCountList.add(String.valueOf(all));
            validOrderCountList.add(String.valueOf(valid));
        }

        double completionRate = 0.0;
        if (totalOrderCount > 0) {
            completionRate = BigDecimal.valueOf(validOrderCount)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(totalOrderCount), 2, RoundingMode.HALF_UP)
                    .doubleValue();
        }

        return OrderReportVO.builder()
                .dateList(String.join(",", dateList))
                .orderCountList(String.join(",", orderCountList))
                .validOrderCountList(String.join(",", validOrderCountList))
                .totalOrderCount(totalOrderCount)
                .validOrderCount(validOrderCount)
                .orderCompletionRate(completionRate)
                .build();
    }

    /**
     * 销量排名Top10（商家端）
     */
    @Override
    public SalesTop10ReportVO top10(LocalDate begin, LocalDate end) {
        List<Map<String, Object>> list = orderDetailMapper.top10Sales(begin.atStartOfDay(), end.plusDays(1).atStartOfDay());
        List<String> nameList = new ArrayList<>();
        List<String> numberList = new ArrayList<>();
        if (list != null) {
            for (Map<String, Object> row : list) {
                Object nameObj = row.get("name");
                Object totalObj = row.get("total");
                nameList.add(nameObj == null ? "" : nameObj.toString());
                numberList.add(totalObj == null ? "0" : totalObj.toString());
            }
        }
        return SalesTop10ReportVO.builder()
                .nameList(String.join(",", nameList))
                .numberList(String.join(",", numberList))
                .build();
    }

    /**
     * 用户统计（商家端）
     */
    @Override
    public UserReportVO userStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dates = new ArrayList<>();
        LocalDate cursor = begin;
        while (!cursor.isAfter(end)) {
            dates.add(cursor);
            cursor = cursor.plusDays(1);
        }

        // 查询每日新增用户
        List<Map<String, Object>> newUsers = userMapper.countNewByDate(begin.atStartOfDay(), end.plusDays(1).atStartOfDay());
        Map<LocalDate, Integer> newUserMap = new HashMap<>();
        if (newUsers != null) {
            for (Map<String, Object> row : newUsers) {
                Object dateObj = row.get("cdate");
                Object cntObj = row.get("cnt");
                if (dateObj != null) {
                    LocalDate d = LocalDate.parse(dateObj.toString());
                    Integer cnt = cntObj == null ? 0 : Integer.parseInt(cntObj.toString());
                    newUserMap.put(d, cnt);
                }
            }
        }

        // 累计用户：先计算 begin 当天0点之前的存量，再累加每日新增
        int beforeCount = userMapper.countBefore(begin.atStartOfDay());
        List<String> dateList = new ArrayList<>();
        List<String> newUserList = new ArrayList<>();
        List<String> totalUserList = new ArrayList<>();

        int runningTotal = beforeCount;
        for (LocalDate d : dates) {
            int add = newUserMap.getOrDefault(d, 0);
            runningTotal += add;
            dateList.add(d.toString());
            newUserList.add(String.valueOf(add));
            totalUserList.add(String.valueOf(runningTotal));
        }

        return UserReportVO.builder()
                .dateList(String.join(",", dateList))
                .newUserList(String.join(",", newUserList))
                .totalUserList(String.join(",", totalUserList))
                .build();
    }

    /**
     * 支付成功，修改订单状态
     */
    @Override
    public void paySuccess(String outTradeNo) {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            userId = 1L;
        }

        Orders ordersDB = ordersMapper.getByNumberAndUserId(outTradeNo, userId);
        if (ordersDB == null) {
            throw new OrderBusinessException("订单不存在");
        }

        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        ordersMapper.update(orders);

        // 来单提醒：type=1
        try {
            webSocketServer.broadcast(1, ordersDB.getId(), "您有新的订单，请及时处理");
        } catch (Exception e) {
            log.error("推送来单提醒失败, orderId={}", ordersDB.getId(), e);
        }
    }

    /**
     * 历史订单分页查询（用户端）
     */
    @Override
    public PageResult historyOrders(OrdersPageQueryDTO ordersPageQueryDTO) {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            userId = 1L;
        }
        ordersPageQueryDTO.setUserId(userId);

        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());
        Page<Orders> page = (Page<Orders>) ordersMapper.pageQueryUser(ordersPageQueryDTO);

        List<OrderVO> records = new ArrayList<>();
        for (Orders orders : page.getResult()) {
            List<OrderDetail> details = orderDetailMapper.listByOrderId(orders.getId());
            OrderVO vo = new OrderVO();
            vo.setId(orders.getId());
            vo.setNumber(orders.getNumber());
            vo.setStatus(orders.getStatus());
            vo.setUserId(orders.getUserId());
            vo.setAddressBookId(orders.getAddressBookId());
            vo.setOrderTime(orders.getOrderTime());
            vo.setCheckoutTime(orders.getCheckoutTime());
            vo.setPayMethod(orders.getPayMethod());
            vo.setPayStatus(orders.getPayStatus());
            vo.setAmount(orders.getAmount());
            vo.setRemark(orders.getRemark());
            vo.setPhone(orders.getPhone());
            vo.setAddress(orders.getAddress());
            vo.setConsignee(orders.getConsignee());
            vo.setCancelReason(orders.getCancelReason());
            vo.setRejectionReason(orders.getRejectionReason());
            vo.setCancelTime(orders.getCancelTime());
            vo.setEstimatedDeliveryTime(orders.getEstimatedDeliveryTime());
            vo.setDeliveryStatus(orders.getDeliveryStatus());
            vo.setDeliveryTime(orders.getDeliveryTime());
            vo.setPackAmount(orders.getPackAmount());
            vo.setTablewareNumber(orders.getTablewareNumber());
            vo.setTablewareStatus(orders.getTablewareStatus());
            vo.setOrderDetailList(details);
            vo.setOrderDishes(buildOrderDishes(details));
            records.add(vo);
        }

        return new PageResult(page.getTotal(), records);
    }

    private String buildOrderDishes(List<OrderDetail> details) {
        if (details == null || details.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < details.size(); i++) {
            OrderDetail d = details.get(i);
            sb.append(d.getName());
            if (d.getNumber() != null) {
                sb.append(" x").append(d.getNumber());
            }
            if (i != details.size() - 1) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }

    private OrderVO buildOrderVO(Orders orders, List<OrderDetail> details) {
        OrderVO vo = new OrderVO();
        vo.setId(orders.getId());
        vo.setNumber(orders.getNumber());
        vo.setStatus(orders.getStatus());
        vo.setUserId(orders.getUserId());
        vo.setAddressBookId(orders.getAddressBookId());
        vo.setOrderTime(orders.getOrderTime());
        vo.setCheckoutTime(orders.getCheckoutTime());
        vo.setPayMethod(orders.getPayMethod());
        vo.setPayStatus(orders.getPayStatus());
        vo.setAmount(orders.getAmount());
        vo.setRemark(orders.getRemark());
        vo.setPhone(orders.getPhone());
        vo.setAddress(orders.getAddress());
        vo.setConsignee(orders.getConsignee());
        vo.setCancelReason(orders.getCancelReason());
        vo.setRejectionReason(orders.getRejectionReason());
        vo.setCancelTime(orders.getCancelTime());
        vo.setEstimatedDeliveryTime(orders.getEstimatedDeliveryTime());
        vo.setDeliveryStatus(orders.getDeliveryStatus());
        vo.setDeliveryTime(orders.getDeliveryTime());
        vo.setPackAmount(orders.getPackAmount());
        vo.setTablewareNumber(orders.getTablewareNumber());
        vo.setTablewareStatus(orders.getTablewareStatus());
        vo.setOrderDetailList(details);
        vo.setOrderDishes(buildOrderDishes(details));
        return vo;
    }

    /**
     * 订单详情（用户端）
     */
    @Override
    public OrderVO getOrderDetail(Long id) {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            userId = 1L;
        }

        Orders orders = ordersMapper.getByIdAndUserId(id, userId);
        if (orders == null) {
            throw new OrderBusinessException("订单不存在");
        }
        List<OrderDetail> details = orderDetailMapper.listByOrderId(orders.getId());
        return buildOrderVO(orders, details);
    }

    /**
     * 取消订单（用户端）
     */
    @Override
    @Transactional
    public void cancel(Long id) {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            userId = 1L;
        }
        Orders orders = ordersMapper.getByIdAndUserId(id, userId);
        if (orders == null) {
            throw new OrderBusinessException("订单不存在");
        }
        if (Orders.COMPLETED.equals(orders.getStatus()) || Orders.CANCELLED.equals(orders.getStatus())) {
            throw new OrderBusinessException("订单已完成或已取消");
        }

        Orders toUpdate = Orders.builder()
                .id(orders.getId())
                .status(Orders.CANCELLED)
                .payStatus(Orders.REFUND)
                .cancelTime(LocalDateTime.now())
                .cancelReason("用户取消")
                .build();
        ordersMapper.update(toUpdate);
    }

    /**
     * 再来一单（将原订单明细重新加入购物车）
     */
    @Override
    @Transactional
    public void repetition(Long id) {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            userId = 1L;
        }
        Orders orders = ordersMapper.getByIdAndUserId(id, userId);
        if (orders == null) {
            throw new OrderBusinessException("订单不存在");
        }

        List<OrderDetail> details = orderDetailMapper.listByOrderId(orders.getId());
        if (details == null || details.isEmpty()) {
            throw new OrderBusinessException("订单明细为空");
        }

        for (OrderDetail detail : details) {
            ShoppingCart cart = new ShoppingCart();
            cart.setUserId(userId);
            cart.setDishId(detail.getDishId());
            cart.setSetmealId(detail.getSetmealId());
            cart.setName(detail.getName());
            cart.setImage(detail.getImage());
            cart.setDishFlavor(detail.getDishFlavor());
            cart.setNumber(detail.getNumber());
            cart.setAmount(detail.getAmount());
            cart.setCreateTime(LocalDateTime.now());
            shoppingCartMapper.insert(cart);
        }
    }

    /**
     * 催单（用户端）
     */
    @Override
    public void reminder(Long id) {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            userId = 1L;
        }
        Orders orders = ordersMapper.getByIdAndUserId(id, userId);
        if (orders == null) {
            throw new OrderBusinessException("订单不存在");
        }
        // 仅在未完成或未取消的情况下提醒
        if (Orders.CANCELLED.equals(orders.getStatus()) || Orders.COMPLETED.equals(orders.getStatus())) {
            throw new OrderBusinessException("订单已完成或已取消，无法催单");
        }
        try {
            webSocketServer.broadcast(2, orders.getId(), "客户催单，请尽快处理");
        } catch (Exception e) {
            log.error("推送催单失败, orderId={}", orders.getId(), e);
        }
    }

    /**
     * 商家端订单搜索（条件+分页）
     */
    @Override
    public PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO) {
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());
        Page<Orders> page = (Page<Orders>) ordersMapper.pageQueryAdmin(ordersPageQueryDTO);
        return new PageResult(page.getTotal(), page.getResult());
    }

    /**
     * 拒单（商家端）
     */
    @Override
    @Transactional
    public void rejection(OrdersRejectionDTO ordersRejectionDTO) {
        Orders orders = ordersMapper.getOrdersById(ordersRejectionDTO.getId());
        if (orders == null) {
            throw new OrderBusinessException("订单不存在");
        }
        if (!Orders.TO_BE_CONFIRMED.equals(orders.getStatus())) {
            throw new OrderBusinessException("当前状态不可拒单");
        }
        Orders toUpdate = Orders.builder()
                .id(ordersRejectionDTO.getId())
                .status(Orders.CANCELLED)
                .rejectionReason(ordersRejectionDTO.getRejectionReason())
                .cancelTime(LocalDateTime.now())
                .payStatus(Orders.REFUND)
                .build();
        ordersMapper.update(toUpdate);
    }

    /**
     * 接单（商家端）
     */
    @Override
    @Transactional
    public void confirm(Long id) {
        Orders orders = ordersMapper.getOrdersById(id);
        if (orders == null) {
            throw new OrderBusinessException("订单不存在");
        }
        if (!Orders.TO_BE_CONFIRMED.equals(orders.getStatus())) {
            throw new OrderBusinessException("当前状态不可接单");
        }
        Orders toUpdate = Orders.builder()
                .id(id)
                .status(Orders.CONFIRMED)
                .build();
        ordersMapper.update(toUpdate);
    }

    /**
     * 取消订单（商家端）
     */
    @Override
    @Transactional
    public void cancelByAdmin(OrdersCancelDTO ordersCancelDTO) {
        Orders orders = ordersMapper.getOrdersById(ordersCancelDTO.getId());
        if (orders == null) {
            throw new OrderBusinessException("订单不存在");
        }
        if (Orders.CANCELLED.equals(orders.getStatus()) || Orders.COMPLETED.equals(orders.getStatus())) {
            throw new OrderBusinessException("订单已完成或已取消");
        }
        Orders toUpdate = Orders.builder()
                .id(ordersCancelDTO.getId())
                .status(Orders.CANCELLED)
                .cancelReason(ordersCancelDTO.getCancelReason())
                .cancelTime(LocalDateTime.now())
                .payStatus(Orders.REFUND)
                .build();
        ordersMapper.update(toUpdate);
    }

    /**
     * 派送订单（商家端）
     */
    @Override
    @Transactional
    public void delivery(Long id) {
        Orders orders = ordersMapper.getOrdersById(id);
        if (orders == null) {
            throw new OrderBusinessException("订单不存在");
        }
        if (!Orders.CONFIRMED.equals(orders.getStatus())) {
            throw new OrderBusinessException("当前状态不可派送");
        }
        Orders toUpdate = Orders.builder()
                .id(id)
                .status(Orders.DELIVERY_IN_PROGRESS)
                .deliveryTime(LocalDateTime.now())
                .build();
        ordersMapper.update(toUpdate);
    }

    /**
     * 完成订单（商家端）
     */
    @Override
    @Transactional
    public void complete(Long id) {
        Orders orders = ordersMapper.getOrdersById(id);
        if (orders == null) {
            throw new OrderBusinessException("订单不存在");
        }
        if (!Orders.DELIVERY_IN_PROGRESS.equals(orders.getStatus())) {
            throw new OrderBusinessException("当前状态不可完成");
        }
        Orders toUpdate = Orders.builder()
                .id(id)
                .status(Orders.COMPLETED)
                .deliveryTime(orders.getDeliveryTime() == null ? LocalDateTime.now() : orders.getDeliveryTime())
                .build();
        ordersMapper.update(toUpdate);
    }

    /**
     * 各状态订单数量统计（商家端）
     */
    @Override
    public OrderStatisticsVO statistics() {
        Integer toBeConfirmed = ordersMapper.countByStatus(Orders.TO_BE_CONFIRMED);
        Integer confirmed = ordersMapper.countByStatus(Orders.CONFIRMED);
        Integer deliveryInProgress = ordersMapper.countByStatus(Orders.DELIVERY_IN_PROGRESS);
        OrderStatisticsVO vo = new OrderStatisticsVO();
        vo.setToBeConfirmed(toBeConfirmed);
        vo.setConfirmed(confirmed);
        vo.setDeliveryInProgress(deliveryInProgress);
        return vo;
    }

    private String formatAddress(AddressBook address) {
        StringBuilder sb = new StringBuilder();
        if (address.getProvinceName() != null) sb.append(address.getProvinceName());
        if (address.getCityName() != null) sb.append(address.getCityName());
        if (address.getDistrictName() != null) sb.append(address.getDistrictName());
        if (address.getDetail() != null) sb.append(address.getDetail());
        return sb.toString();
    }
}
