package com.sky.mapper;

import com.sky.entity.User;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper {

    /**
     * 根据openid查询用户
     * @param openid
     * @return
     */
    @Select("select * from user where openid = #{openid}")
    User getByOpenid(String openid);

    /**
     * 新增用户
     * @param user
     */
    @Insert("insert into user (openid, create_time) values (#{openid}, #{createTime})")
    void insert(User user);

    /**
     * 统计时间区间内按日新增用户数
     * @param begin
     * @param end
     * @return
     */
    @Select("select date(create_time) as cdate, count(*) as cnt from user where create_time >= #{begin} and create_time < #{end} group by date(create_time)")
    java.util.List<java.util.Map<String, Object>> countNewByDate(java.time.LocalDateTime begin, java.time.LocalDateTime end);

    /**
     * 统计截止某日（不含当日0点）之前的累计用户数
     * @param before
     * @return
     */
    @Select("select count(*) from user where create_time < #{before}")
    Integer countBefore(java.time.LocalDateTime before);

    /**
     * 按条件统计用户数量（可选时间区间）
     * @param map
     * @return
     */
    @Select("<script>" +
            "select count(*) from user" +
            "<where>" +
            "  <if test=\"begin != null\"> and create_time &gt;= #{begin} </if>" +
            "  <if test=\"end != null\"> and create_time &lt; #{end} </if>" +
            "</where>" +
            "</script>")
    Integer countByMap(java.util.Map<String, Object> map);
}
