package org.example.mybatisplusdemo.controller;

import org.example.mybatisplusdemo.common.Result;
import org.example.mybatisplusdemo.entity.Order;
import org.example.mybatisplusdemo.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/order-user")
public class OrderUserController {

    @Autowired
    private OrderService orderService;

    // 1. 根据订单ID查询订单及其用户信息
    @GetMapping("/order/{orderId}")
    public Result<Order> getOrderWithUser(@PathVariable Long orderId) {
        log.info("查询订单及其用户信息: orderId={}", orderId);

        Order order = orderService.getOrderWithUser(orderId);
        if (order == null) {
            return Result.error("订单不存在");
        }

        return Result.success("查询成功", order);
    }

    // 2. 查询所有订单及其用户信息
    @GetMapping("/orders")
    public Result<List<Order>> getAllOrdersWithUser() {
        log.info("查询所有订单及其用户信息");

        List<Order> orders = orderService.getAllOrdersWithUser();
        return Result.success("查询成功", orders);
    }

    // 3. 根据订单号查询订单及其用户
    @GetMapping("/order-no/{orderNo}")
    public Result<Order> getOrderWithUserByOrderNo(@PathVariable String orderNo) {
        log.info("根据订单号查询订单及其用户: orderNo={}", orderNo);

        Order order = orderService.getOrderWithUserByOrderNo(orderNo);
        if (order == null) {
            return Result.error("订单不存在");
        }

        return Result.success("查询成功", order);
    }

    // 4. 根据用户ID查询该用户的所有订单（包含用户信息）
    @GetMapping("/user/{userId}/orders")
    public Result<List<Order>> getUserOrdersWithUserInfo(@PathVariable Long userId) {
        log.info("查询用户的订单（包含用户信息）: userId={}", userId);

        List<Order> orders = orderService.getOrdersByUserId(userId);

        // 为每个订单设置用户信息
        orders.forEach(order -> {
            // 这里可以重用 getOrderWithUser 的逻辑，但为了性能，可以批量查询
            if (order.getUser() == null) {
                order.setUser(orderService.getOrderWithUser(order.getId()).getUser());
            }
        });

        return Result.success("查询成功", orders);
    }

    // 5. 获取订单详情（包含完整的用户信息）
    @GetMapping("/detail/{orderId}")
    public Result<Order> getOrderDetail(@PathVariable Long orderId) {
        log.info("获取订单详情: orderId={}", orderId);

        Order order = orderService.getOrderWithUser(orderId);
        if (order == null) {
            return Result.error("订单不存在");
        }

        // 可以在这里添加更多详情，比如订单商品等

        return Result.success("查询成功", order);
    }

    // 6. 批量查询订单及其用户
    @PostMapping("/orders/batch")
    public Result<List<Order>> getOrdersWithUserBatch(@RequestBody List<Long> orderIds) {
        log.info("批量查询订单及其用户: orderIds={}", orderIds);

        if (orderIds == null || orderIds.isEmpty()) {
            return Result.error("请提供订单ID列表");
        }

        List<Order> orders = orderService.listByIds(orderIds);

        // 批量设置用户信息
        orders.forEach(order -> {
            if (order.getUser() == null) {
                order.setUser(orderService.getOrderWithUser(order.getId()).getUser());
            }
        });

        return Result.success("查询成功", orders);
    }
}