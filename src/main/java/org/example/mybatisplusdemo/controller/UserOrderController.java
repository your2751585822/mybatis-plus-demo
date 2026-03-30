package org.example.mybatisplusdemo.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.mybatisplusdemo.common.Result;
import org.example.mybatisplusdemo.entity.Order;
import org.example.mybatisplusdemo.entity.User;
import org.example.mybatisplusdemo.service.OrderService;
import org.example.mybatisplusdemo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/user-orders")
public class UserOrderController {

    @Autowired
    private UserService userService;

    @Autowired
    private OrderService orderService;

    // 修复：查询用户及其订单
    @GetMapping("/user/{userId}")
    public Result<User> getUserWithOrders(@PathVariable Long userId) {
        log.info("查询用户及其订单: userId={}", userId);

        // 1. 查询用户
        User user = userService.getById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }

        // 2. 查询该用户的所有订单
        List<Order> orders = getOrdersByUserId(userId);
        log.info("找到 {} 条订单", orders.size());

        // 3. 设置订单到用户对象
        user.setOrders(orders);

        return Result.success("查询成功", user);
    }

    // 修复：查询所有用户及其订单
    @GetMapping("/all")
    public Result<List<User>> getAllUsersWithOrders() {
        log.info("查询所有用户及其订单");

        // 1. 查询所有用户
        List<User> users = userService.list();
        log.info("找到 {} 个用户", users.size());

        // 2. 为每个用户查询订单
        for (User user : users) {
            List<Order> orders = getOrdersByUserId(user.getId());
            user.setOrders(orders);
            log.info("用户 {} 有 {} 条订单", user.getName(), orders.size());
        }

        return Result.success("查询成功", users);
    }

    // 修复：查询用户及其订单统计
    @GetMapping("/stats")
    public Result<List<Map<String, Object>>> getUsersWithOrderStats() {
        log.info("查询用户及其订单统计");

        List<User> users = userService.list();
        List<Map<String, Object>> result = new ArrayList<>();

        for (User user : users) {
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("user_id", user.getId());
            userInfo.put("user_name", user.getName());
            userInfo.put("user_age", user.getAge());

            // 获取订单统计
            Map<String, Object> orderStats = getUserOrderStats(user.getId());
            userInfo.put("order_stats", orderStats);

            // 也可以同时返回订单列表
            List<Order> orders = getOrdersByUserId(user.getId());
            userInfo.put("orders", orders);

            result.add(userInfo);
        }

        return Result.success("查询成功", result);
    }

    // 根据订单状态筛选用户
    @GetMapping("/by-order-status")
    public Result<List<User>> getUsersByOrderStatus(@RequestParam Integer status) {
        log.info("查询有指定订单状态的用户: status={}", status);

        // 1. 查询指定状态的订单
        LambdaQueryWrapper<Order> orderWrapper = new LambdaQueryWrapper<>();
        orderWrapper.eq(Order::getStatus, status);
        List<Order> orders = orderService.list(orderWrapper);
        log.info("找到 {} 条状态为 {} 的订单", orders.size(), status);

        if (orders.isEmpty()) {
            return Result.success("没有符合条件的用户", new ArrayList<>());
        }

        // 2. 获取有这些订单的用户ID
        Set<Long> userIds = new HashSet<>();
        for (Order order : orders) {
            userIds.add(order.getUserId());
        }

        // 3. 查询这些用户
        List<User> users = userService.listByIds(userIds);

        // 4. 按用户分组订单
        Map<Long, List<Order>> orderMap = new HashMap<>();
        for (Order order : orders) {
            Long userId = order.getUserId();
            if (!orderMap.containsKey(userId)) {
                orderMap.put(userId, new ArrayList<>());
            }
            orderMap.get(userId).add(order);
        }

        // 5. 设置订单
        for (User user : users) {
            user.setOrders(orderMap.get(user.getId()));
        }

        return Result.success("查询成功", users);
    }

    // 查询订单金额大于指定值的用户
    @GetMapping("/by-amount")
    public Result<List<User>> getUsersByOrderAmount(@RequestParam Double minAmount) {
        log.info("查询订单总金额大于 {} 的用户", minAmount);

        // 1. 查询所有订单
        List<Order> allOrders = orderService.list();

        // 2. 按用户分组统计金额
        Map<Long, List<Order>> userOrdersMap = new HashMap<>();
        Map<Long, Double> userTotalAmount = new HashMap<>();

        for (Order order : allOrders) {
            Long userId = order.getUserId();

            // 分组订单
            if (!userOrdersMap.containsKey(userId)) {
                userOrdersMap.put(userId, new ArrayList<>());
            }
            userOrdersMap.get(userId).add(order);

            // 累计金额
            double amount = order.getTotalAmount().doubleValue();
            userTotalAmount.put(userId, userTotalAmount.getOrDefault(userId, 0.0) + amount);
        }

        // 3. 筛选出总金额大于minAmount的用户
        List<Long> userIds = new ArrayList<>();
        for (Map.Entry<Long, Double> entry : userTotalAmount.entrySet()) {
            if (entry.getValue() > minAmount) {
                userIds.add(entry.getKey());
            }
        }

        if (userIds.isEmpty()) {
            return Result.success("没有符合条件的用户", new ArrayList<>());
        }

        // 4. 查询用户
        List<User> users = userService.listByIds(userIds);

        // 5. 设置订单
        for (User user : users) {
            user.setOrders(userOrdersMap.get(user.getId()));
        }

        log.info("找到 {} 个符合条件的用户", users.size());
        return Result.success("查询成功", users);
    }

    // 获取统计看板
    @GetMapping("/dashboard")
    public Result<Map<String, Object>> getDashboard() {
        log.info("获取订单统计看板");

        Map<String, Object> dashboard = new HashMap<>();

        // 1. 用户统计
        long totalUsers = userService.count();
        dashboard.put("total_users", totalUsers);

        // 2. 订单统计
        List<Order> allOrders = orderService.list();
        dashboard.put("total_orders", allOrders.size());

        BigDecimal totalAmount = BigDecimal.ZERO;
        for (Order order : allOrders) {
            totalAmount = totalAmount.add(order.getTotalAmount());
        }
        dashboard.put("total_amount", totalAmount);

        // 3. 各状态订单数
        Map<Integer, Long> statusCount = new HashMap<>();
        for (Order order : allOrders) {
            Integer status = order.getStatus();
            statusCount.put(status, statusCount.getOrDefault(status, 0L) + 1);
        }
        dashboard.put("order_status_count", statusCount);

        // 4. 每个用户的订单数和金额
        Map<Long, Integer> userOrderCount = new HashMap<>();
        Map<Long, BigDecimal> userOrderAmount = new HashMap<>();

        for (Order order : allOrders) {
            Long userId = order.getUserId();
            userOrderCount.put(userId, userOrderCount.getOrDefault(userId, 0) + 1);
            userOrderAmount.put(userId,
                    userOrderAmount.getOrDefault(userId, BigDecimal.ZERO).add(order.getTotalAmount()));
        }

        // 5. 消费前5的用户
        List<Map<String, Object>> topUsers = new ArrayList<>();
        List<Long> sortedUserIds = new ArrayList<>(userOrderAmount.keySet());
        sortedUserIds.sort((id1, id2) ->
                userOrderAmount.get(id2).compareTo(userOrderAmount.get(id1)));

        int limit = Math.min(5, sortedUserIds.size());
        for (int i = 0; i < limit; i++) {
            Long userId = sortedUserIds.get(i);
            Map<String, Object> userInfo = new HashMap<>();

            User user = userService.getById(userId);
            userInfo.put("user_id", userId);
            userInfo.put("user_name", user != null ? user.getName() : "未知");
            userInfo.put("order_count", userOrderCount.get(userId));
            userInfo.put("total_amount", userOrderAmount.get(userId));

            topUsers.add(userInfo);
        }
        dashboard.put("top_users", topUsers);

        // 6. 最近5个订单
        List<Order> recentOrders = allOrders.stream()
                .sorted((o1, o2) -> {
                    if (o1.getCreateTime() == null) return 1;
                    if (o2.getCreateTime() == null) return -1;
                    return o2.getCreateTime().compareTo(o1.getCreateTime());
                })
                .limit(5)
                .collect(Collectors.toList());
        dashboard.put("recent_orders", recentOrders);

        log.info("统计看板生成完成");
        return Result.success("获取成功", dashboard);
    }

    // 辅助方法：根据用户ID查询订单
    private List<Order> getOrdersByUserId(Long userId) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getUserId, userId)
                .orderByDesc(Order::getCreateTime);
        return orderService.list(wrapper);
    }

    // 辅助方法：获取用户订单统计
    private Map<String, Object> getUserOrderStats(Long userId) {
        Map<String, Object> stats = new HashMap<>();
        List<Order> orders = getOrdersByUserId(userId);

        stats.put("total_orders", orders.size());

        if (!orders.isEmpty()) {
            BigDecimal totalAmount = BigDecimal.ZERO;
            for (Order order : orders) {
                totalAmount = totalAmount.add(order.getTotalAmount());
            }
            stats.put("total_amount", totalAmount);
            stats.put("avg_amount", totalAmount.divide(BigDecimal.valueOf(orders.size()), 2, BigDecimal.ROUND_HALF_UP));

            // 各状态订单数
            Map<Integer, Long> statusCount = new HashMap<>();
            for (Order order : orders) {
                Integer status = order.getStatus();
                statusCount.put(status, statusCount.getOrDefault(status, 0L) + 1);
            }
            stats.put("status_count", statusCount);
        } else {
            stats.put("total_amount", BigDecimal.ZERO);
            stats.put("avg_amount", BigDecimal.ZERO);
            stats.put("status_count", new HashMap<>());
        }

        return stats;
    }
}