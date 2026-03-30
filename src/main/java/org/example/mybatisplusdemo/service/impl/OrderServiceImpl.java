package org.example.mybatisplusdemo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.mybatisplusdemo.entity.Order;
import org.example.mybatisplusdemo.entity.User;
import org.example.mybatisplusdemo.mapper.OrderMapper;
import org.example.mybatisplusdemo.service.OrderService;
import org.example.mybatisplusdemo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {

    @Autowired
    private UserService userService;  // 注入UserService

    @Override
    public List<Order> getOrdersByUserId(Long userId) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getUserId, userId)
                .orderByDesc(Order::getCreateTime);
        return this.list(wrapper);
    }

    @Override
    public Map<String, Object> getUserOrderStats(Long userId) {
        Map<String, Object> stats = new HashMap<>();
        List<Order> orders = getOrdersByUserId(userId);

        stats.put("total_orders", orders.size());

        if (!orders.isEmpty()) {
            BigDecimal totalAmount = orders.stream()
                    .map(Order::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            stats.put("total_amount", totalAmount);
            stats.put("avg_amount", totalAmount.divide(BigDecimal.valueOf(orders.size()), 2, BigDecimal.ROUND_HALF_UP));
        } else {
            stats.put("total_amount", BigDecimal.ZERO);
            stats.put("avg_amount", BigDecimal.ZERO);
        }

        Map<Integer, Long> statusCount = orders.stream()
                .collect(Collectors.groupingBy(Order::getStatus, Collectors.counting()));
        stats.put("status_count", statusCount);

        return stats;
    }

    @Override
    public List<Map<String, Object>> getAllUsersOrderStats() {
        return baseMapper.selectList(null).stream()
                .collect(Collectors.groupingBy(
                        Order::getUserId,
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                orders -> {
                                    Map<String, Object> userStats = new HashMap<>();
                                    userStats.put("order_count", orders.size());
                                    userStats.put("total_amount", orders.stream()
                                            .map(Order::getTotalAmount)
                                            .reduce(BigDecimal.ZERO, BigDecimal::add));
                                    return userStats;
                                }
                        )
                ))
                .entrySet().stream()
                .map(entry -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("user_id", entry.getKey());
                    map.putAll(entry.getValue());
                    return map;
                })
                .collect(Collectors.toList());
    }

    @Override
    public boolean updateOrderStatus(Long orderId, Integer status) {
        Order order = new Order();
        order.setId(orderId);
        order.setStatus(status);
        return this.updateById(order);
    }

    // 新增：根据订单ID查询订单及其用户信息
    @Override
    public Order getOrderWithUser(Long orderId) {
        // 1. 查询订单
        Order order = this.getById(orderId);
        if (order == null) {
            return null;
        }

        // 2. 根据订单中的userId查询用户
        User user = userService.getById(order.getUserId());

        // 3. 设置用户信息到订单
        order.setUser(user);

        return order;
    }

    // 新增：查询所有订单及其用户信息
    @Override
    public List<Order> getAllOrdersWithUser() {
        // 1. 查询所有订单
        List<Order> orders = this.list();
        if (orders.isEmpty()) {
            return orders;
        }

        // 2. 获取所有用户ID
        Set<Long> userIds = orders.stream()
                .map(Order::getUserId)
                .collect(Collectors.toSet());

        // 3. 批量查询所有相关用户
        List<User> users = userService.listByIds(userIds);

        // 4. 将用户列表转换为Map，方便查找
        Map<Long, User> userMap = users.stream()
                .collect(Collectors.toMap(User::getId, user -> user));

        // 5. 为每个订单设置用户
        orders.forEach(order ->
                order.setUser(userMap.get(order.getUserId()))
        );

        return orders;
    }

    // 新增：根据订单号查询订单及其用户
    @Override
    public Order getOrderWithUserByOrderNo(String orderNo) {
        // 1. 根据订单号查询订单
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getOrderNo, orderNo);
        Order order = this.getOne(wrapper);

        if (order == null) {
            return null;
        }

        // 2. 查询用户信息
        User user = userService.getById(order.getUserId());
        order.setUser(user);

        return order;
    }
}