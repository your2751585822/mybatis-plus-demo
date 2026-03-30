package org.example.mybatisplusdemo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.example.mybatisplusdemo.entity.Order;
import java.util.List;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    @Select("SELECT * FROM orders WHERE user_id = #{userId} AND deleted = 0 ORDER BY create_time DESC")
    List<Order> selectByUserId(Long userId);

    @Select("SELECT COUNT(*), SUM(total_amount) FROM orders WHERE user_id = #{userId} AND deleted = 0")
    List<Object> selectUserOrderStats(Long userId);
}