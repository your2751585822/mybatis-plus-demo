package org.example.mybatisplusdemo.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.mybatisplusdemo.common.Result;
import org.example.mybatisplusdemo.entity.User;
import org.example.mybatisplusdemo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j  // 添加日志支持
@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserService userService;

    // 测试方法
    @GetMapping("/ping")
    public Result<String> ping() {
        log.info("调用ping方法");
        return Result.success("pong");
    }

    // 查询所有用户
    @GetMapping
    public Result<List<User>> findAll() {
        log.info("========== 调用 findAll 方法 ==========");
        try {
            List<User> list = userService.list();
            return Result.success("查询成功", list);
        } catch (Exception e) {
            log.error("查询失败", e);
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    // 根据ID查询
    @GetMapping("/{id}")
    public Result<User> findById(@PathVariable Long id) {
        log.info("根据ID查询用户: {}", id);
        try {
            User user = userService.getById(id);
            if (user != null) {
                return Result.success("查询成功", user);
            } else {
                return Result.error("用户不存在，ID: " + id);
            }
        } catch (Exception e) {
            log.error("查询失败", e);
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    // 分页查询
    @GetMapping("/page")
    public Result<Page<User>> page(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size) {
        log.info("分页查询: current={}, size={}", current, size);
        try {
            Page<User> page = new Page<>(current, size);
            Page<User> result = userService.page(page);
            return Result.success("分页查询成功", result);
        } catch (Exception e) {
            log.error("分页查询失败", e);
            return Result.error("分页查询失败：" + e.getMessage());
        }
    }

    // 条件查询
    @GetMapping("/search")
    public Result<List<User>> search(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer age) {
        log.info("条件查询: name={}, age={}", name, age);
        try {
            LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
            if (name != null && !name.isEmpty()) {
                wrapper.like(User::getName, name);
            }
            if (age != null) {
                wrapper.eq(User::getAge, age);
            }
            List<User> list = userService.list(wrapper);
            return Result.success("条件查询成功", list);
        } catch (Exception e) {
            log.error("条件查询失败", e);
            return Result.error("条件查询失败：" + e.getMessage());
        }
    }

    // 新增用户
    @PostMapping
    public Result<Boolean> add(@RequestBody User user) {
        log.info("新增用户: {}", user);
        try {
            // 参数校验
            if (user.getName() == null || user.getName().trim().isEmpty()) {
                return Result.error("用户姓名不能为空");
            }
            if (user.getAge() != null && (user.getAge() < 1 || user.getAge() > 150)) {
                return Result.error("年龄必须在1-150之间");
            }

            boolean success = userService.save(user);
            if (success) {
                log.info("新增用户成功，ID: {}", user.getId());
                return Result.success("新增成功", true);
            } else {
                return Result.error("新增失败");
            }
        } catch (Exception e) {
            log.error("新增用户失败", e);
            return Result.error("新增失败：" + e.getMessage());
        }
    }

    // 更新用户
    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody User user) {
        log.info("更新用户: id={}, user={}", id, user);
        try {
            // 检查用户是否存在
            User existingUser = userService.getById(id);
            if (existingUser == null) {
                return Result.error("用户不存在，ID: " + id);
            }

            user.setId(id);
            boolean success = userService.updateById(user);
            if (success) {
                log.info("更新用户成功: {}", id);
                return Result.success("更新成功", true);
            } else {
                return Result.error("更新失败");
            }
        } catch (Exception e) {
            log.error("更新用户失败", e);
            return Result.error("更新失败：" + e.getMessage());
        }
    }

    // 删除用户
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        log.info("删除用户: {}", id);
        try {
            // 检查用户是否存在
            User existingUser = userService.getById(id);
            if (existingUser == null) {
                return Result.error("用户不存在，ID: " + id);
            }

            boolean success = userService.removeById(id);
            if (success) {
                log.info("删除用户成功: {}", id);
                return Result.success("删除成功", true);
            } else {
                return Result.error("删除失败");
            }
        } catch (Exception e) {
            log.error("删除用户失败", e);
            return Result.error("删除失败：" + e.getMessage());
        }
    }

    // 批量删除
    @DeleteMapping("/batch")
    public Result<Boolean> deleteBatch(@RequestBody List<Long> ids) {
        log.info("批量删除用户: {}", ids);
        try {
            if (ids == null || ids.isEmpty()) {
                return Result.error("请选择要删除的用户");
            }

            boolean success = userService.removeByIds(ids);
            if (success) {
                log.info("批量删除成功: {}", ids);
                return Result.success("批量删除成功", true);
            } else {
                return Result.error("批量删除失败");
            }
        } catch (Exception e) {
            log.error("批量删除失败", e);
            return Result.error("批量删除失败：" + e.getMessage());
        }
    }
}