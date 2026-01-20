package com.sky.mapper;

import com.sky.entity.OrderDetail;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface OrderDetailMapper {

    void insertBatch(List<OrderDetail> details);

    /**
     * 根据订单id查询明细
     * @param orderId
     * @return
     */
    List<OrderDetail> listByOrderId(Long orderId);

    /**
     * 销量Top10（按商品名聚合，包含菜品与套餐）
     * @param begin
     * @param end
     * @return
     */
    java.util.List<java.util.Map<String, Object>> top10Sales(java.time.LocalDateTime begin, java.time.LocalDateTime end);
}
