package org.example.mybatisplusdemo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.example.mybatisplusdemo.entity.Order;
import java.util.List;
import java.util.Map;

public interface OrderService extends IService<Order> {
    List<Order> getOrdersByUserId(Long userId);
    Map<String, Object> getUserOrderStats(Long userId);
    List<Map<String, Object>> getAllUsersOrderStats();
    boolean updateOrderStatus(Long orderId, Integer status);

    // 新增：根据订单ID查询订单及其用户信息
    Order getOrderWithUser(Long orderId);

    // 新增：查询所有订单及其用户信息
    List<Order> getAllOrdersWithUser();

    // 新增：根据订单号查询订单及其用户
    Order getOrderWithUserByOrderNo(String orderNo);
}