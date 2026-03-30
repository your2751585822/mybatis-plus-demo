package org.example.mybatisplusdemo.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.mybatisplusdemo.common.Result;
import org.example.mybatisplusdemo.entity.Order;
import org.example.mybatisplusdemo.service.OrderService;
import org.example.mybatisplusdemo.util.RedisLockUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private RedisLockUtil redisLockUtil;  // 注入分布式锁工具

    // 查询所有订单 - 缓存10分钟
    @GetMapping
    @Cacheable(value = "orders", key = "'all'")
    public Result<List<Order>> findAll() {
        log.info("从数据库查询所有订单...");
        return Result.success("查询成功", orderService.list());
    }

    // 根据ID查询订单 - 缓存单个订单
    @GetMapping("/{id}")
    @Cacheable(value = "orders", key = "#id")
    public Result<Order> findById(@PathVariable Long id) {
        log.info("从数据库查询订单: {}", id);
        Order order = orderService.getById(id);
        return order != null ? Result.success("查询成功", order) : Result.error("订单不存在");
    }

    @GetMapping("/user/{userId}")
    @Cacheable(value = "orders", key = "'user:' + #userId")
    public Result<List<Order>> findByUserId(@PathVariable Long userId) {
        log.info("从数据库查询用户{}的订单", userId);
        return Result.success("查询成功", orderService.getOrdersByUserId(userId));
    }

    @GetMapping("/page")
    public Result<Page<Order>> page(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size) {
        Page<Order> page = new Page<>(current, size);
        return Result.success("分页查询成功", orderService.page(page));
    }

    @GetMapping("/search")
    public Result<List<Order>> search(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String orderNo) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        if (userId != null) wrapper.eq(Order::getUserId, userId);
        if (status != null) wrapper.eq(Order::getStatus, status);
        if (orderNo != null && !orderNo.isEmpty()) wrapper.like(Order::getOrderNo, orderNo);
        wrapper.orderByDesc(Order::getCreateTime);
        return Result.success("查询成功", orderService.list(wrapper));
    }

    @GetMapping("/stats/user/{userId}")
    public Result<Map<String, Object>> getUserOrderStats(@PathVariable Long userId) {
        return Result.success("统计成功", orderService.getUserOrderStats(userId));
    }

    @GetMapping("/stats/all")
    public Result<List<Map<String, Object>>> getAllUsersOrderStats() {
        return Result.success("统计成功", orderService.getAllUsersOrderStats());
    }

    // 新增订单 - 加分布式锁 + 清除缓存
    @PostMapping
    @CacheEvict(value = "orders", allEntries = true)
    public Result<Boolean> add(@RequestBody Order order) {
        // 生成唯一请求ID
        String requestId = UUID.randomUUID().toString();
        String lockKey = "lock:order:user:" + order.getUserId();

        // 尝试获取锁（10秒自动释放）
        boolean locked = redisLockUtil.tryLock(lockKey, requestId, 10);

        if (!locked) {
            log.warn("用户 {} 重复提交订单，已被拦截", order.getUserId());
            return Result.error("请勿重复提交订单");
        }

        try {
            log.info("用户 {} 开始创建订单", order.getUserId());

            if (order.getOrderNo() == null || order.getOrderNo().isEmpty()) {
                order.setOrderNo("ORD" + System.currentTimeMillis());
            }
            if (order.getStatus() == null) {
                order.setStatus(Order.STATUS_PENDING);
            }

            boolean result = orderService.save(order);

            if (result) {
                log.info("用户 {} 创建订单成功", order.getUserId());
                return Result.success("新增成功", true);
            } else {
                return Result.error("新增失败");
            }
        } finally {
            // 释放锁
            redisLockUtil.unlock(lockKey, requestId);
        }
    }

    @PutMapping("/{id}/status")
    @CacheEvict(value = "orders", key = "#id")
    public Result<Boolean> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        // 加锁防止并发修改
        String requestId = UUID.randomUUID().toString();
        String lockKey = "lock:order:" + id;

        boolean locked = redisLockUtil.tryLock(lockKey, requestId, 10);
        if (!locked) {
            return Result.error("订单正在被操作，请稍后再试");
        }

        try {
            log.info("更新订单{}状态，清除该订单缓存", id);
            return orderService.updateOrderStatus(id, status) ?
                    Result.success("更新成功", true) : Result.error("更新失败");
        } finally {
            redisLockUtil.unlock(lockKey, requestId);
        }
    }

    // 更新订单 - 加锁 + 更新缓存
    @PutMapping("/{id}")
    @CachePut(value = "orders", key = "#id")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody Order order) {
        // 加锁防止并发修改
        String requestId = UUID.randomUUID().toString();
        String lockKey = "lock:order:" + id;

        boolean locked = redisLockUtil.tryLock(lockKey, requestId, 10);
        if (!locked) {
            return Result.error("订单正在被修改，请稍后再试");
        }

        try {
            order.setId(id);
            log.info("更新订单{}，更新缓存", id);
            return orderService.updateById(order) ? Result.success("更新成功", true) : Result.error("更新失败");
        } finally {
            redisLockUtil.unlock(lockKey, requestId);
        }
    }

    // 删除订单 - 加锁 + 清除缓存
    @DeleteMapping("/{id}")
    @CacheEvict(value = "orders", key = "#id")
    public Result<Boolean> delete(@PathVariable Long id) {
        // 加锁防止并发删除/修改
        String requestId = UUID.randomUUID().toString();
        String lockKey = "lock:order:" + id;

        boolean locked = redisLockUtil.tryLock(lockKey, requestId, 10);
        if (!locked) {
            return Result.error("订单正在被操作，请稍后再试");
        }

        try {
            log.info("删除订单{}，清除该订单缓存", id);
            return orderService.removeById(id) ? Result.success("删除成功", true) : Result.error("删除失败");
        } finally {
            redisLockUtil.unlock(lockKey, requestId);
        }
    }

    // 批量删除 - 清除所有订单缓存
    @DeleteMapping("/batch")
    @CacheEvict(value = "orders", allEntries = true)
    public Result<Boolean> deleteBatch(@RequestBody List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Result.error("请选择要删除的订单");
        }

        // 批量删除加锁（防止多个请求同时删除）
        String requestId = UUID.randomUUID().toString();
        String lockKey = "lock:order:batch";

        boolean locked = redisLockUtil.tryLock(lockKey, requestId, 30);
        if (!locked) {
            return Result.error("批量操作正在执行，请稍后再试");
        }

        try {
            log.info("批量删除订单，清除所有订单缓存");
            return orderService.removeByIds(ids) ? Result.success("批量删除成功", true) : Result.error("批量删除失败");
        } finally {
            redisLockUtil.unlock(lockKey, requestId);
        }
    }
}