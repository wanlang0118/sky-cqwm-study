package com.sky.mapper;

import com.sky.entity.Orders;
import com.sky.dto.OrdersPageQueryDTO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface OrdersMapper {

    /**
     * 插入订单数据
     * @param orders
     */
    @Insert("insert into orders (number, status, user_id, address_book_id, order_time, checkout_time, pay_method, pay_status, amount, remark, phone, address, consignee, cancel_reason, rejection_reason, cancel_time, estimated_delivery_time, delivery_status, delivery_time, pack_amount, tableware_number, tableware_status) " +
            "values (#{number}, #{status}, #{userId}, #{addressBookId}, #{orderTime}, #{checkoutTime}, #{payMethod}, #{payStatus}, #{amount}, #{remark}, #{phone}, #{address}, #{consignee}, #{cancelReason}, #{rejectionReason}, #{cancelTime}, #{estimatedDeliveryTime}, #{deliveryStatus}, #{deliveryTime}, #{packAmount}, #{tablewareNumber}, #{tablewareStatus})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Orders orders);

    /**
     * 根据订单号和用户id查询订单
     * @param orderNumber
     * @param userId
     * @return
     */
    @Select("select * from orders where number = #{orderNumber} and user_id = #{userId}")
    Orders getByNumberAndUserId(String orderNumber, Long userId);

    /**
     * 修改订单
     * @param orders
     */
    void update(Orders orders);

    /**
     * 根据id查询订单
     * @param id
     * @return
     */
    @Select("select * from orders where id = #{id}")
    Orders getOrdersById(Long id);

    /**
     * 用户端历史订单分页查询
     * @param ordersPageQueryDTO
     * @return
     */
    List<Orders> pageQueryUser(OrdersPageQueryDTO ordersPageQueryDTO);

    /**
     * 商家端订单搜索（条件+分页）
     * @param ordersPageQueryDTO
     * @return
     */
    List<Orders> pageQueryAdmin(OrdersPageQueryDTO ordersPageQueryDTO);

    /**
     * 根据id和用户id查询订单
     * @param id
     * @param userId
     * @return
     */
    @Select("select * from orders where id = #{id} and user_id = #{userId}")
    Orders getByIdAndUserId(Long id, Long userId);

    /**
     * 按状态统计数量
     * @param status
     * @return
     */
    @Select("select count(*) from orders where status = #{status}")
    Integer countByStatus(Integer status);

    /**
     * 根据条件统计订单数量（可选状态、时间区间）
     * @param map
     * @return
     */
    Integer countByMap(Map map);

    /**
     * 根据条件统计订单金额汇总（可选状态、时间区间）
     * @param map
     * @return
     */
    Double sumByMap(Map map);

    /**
     * 查询指定状态且下单时间早于指定时间的订单
     * @param status
     * @param time
     * @return
     */
    List<Orders> listByStatusAndOrderTimeLT(Integer status, java.time.LocalDateTime time);

    /**
     * 查询指定状态的订单
     * @param status
     * @return
     */
    List<Orders> listByStatus(Integer status);

    /**
     * 指定时间区间内已完成订单的日营业额
     * @param begin
     * @param end
     * @return
     */
    List<java.util.Map<String, Object>> sumAmountByDate(java.time.LocalDateTime begin, java.time.LocalDateTime end);

    /**
     * 指定时间区间内每日订单数
     * @param begin
     * @param end
     * @return
     */
    List<java.util.Map<String, Object>> countByDate(java.time.LocalDateTime begin, java.time.LocalDateTime end);

    /**
     * 指定时间区间内按状态的每日订单数
     * @param status
     * @param begin
     * @param end
     * @return
     */
    List<java.util.Map<String, Object>> countByDateAndStatus(Integer status, java.time.LocalDateTime begin, java.time.LocalDateTime end);
}
