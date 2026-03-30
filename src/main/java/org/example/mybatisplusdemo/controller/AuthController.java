package org.example.mybatisplusdemo.controller;

import org.example.mybatisplusdemo.common.Result;
import org.example.mybatisplusdemo.entity.LoginRequest;
import org.example.mybatisplusdemo.entity.LoginResponse;
import org.example.mybatisplusdemo.entity.User;
import org.example.mybatisplusdemo.entity.UserPrincipal;
import org.example.mybatisplusdemo.mapper.UserMapper;
import org.example.mybatisplusdemo.security.JwtUtils;
import org.example.mybatisplusdemo.util.RateLimiterUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    JwtUtils jwtUtils;

    @Autowired
    UserMapper userMapper;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    private RateLimiterUtil rateLimiterUtil;  // 注入限流工具

    @PostMapping("/register")
    public Result<User> register(@RequestBody User user) {
        logger.info("========== 开始注册用户 ==========");
        logger.info("接收到的用户信息: name={}, age={}, email={}, password={}",
                user.getName(), user.getAge(), user.getEmail(),
                user.getPassword() != null ? "已提供" : "未提供");

        try {
            // 检查用户名是否已存在
            User existingUser = userMapper.selectByName(user.getName());
            if (existingUser != null) {
                return Result.error("用户名已存在");
            }

            // 检查密码是否为空
            if (user.getPassword() == null || user.getPassword().isEmpty()) {
                return Result.error("密码不能为空");
            }

            // 加密密码
            String encodedPassword = passwordEncoder.encode(user.getPassword());
            user.setPassword(encodedPassword);

            // 保存用户
            userMapper.insert(user);
            user.setPassword(null);  // 返回时隐藏密码

            return Result.success("注册成功", user);

        } catch (Exception e) {
            logger.error("注册失败", e);
            return Result.error("注册失败: " + e.getMessage());
        }
    }

    /**
     * 获取客户端真实 IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 如果有多级代理，取第一个 IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody LoginRequest loginRequest, HttpServletRequest httpRequest) {
        logger.info("========== 开始登录 ==========");

        String username = loginRequest.getUsername();
        String ip = getClientIp(httpRequest);

        logger.info("登录请求: username={}, ip={}", username, ip);

        // 限流检查：同一个IP 1分钟内最多尝试5次
        if (rateLimiterUtil.isRateLimited(ip, 5, 60)) {
            logger.warn("IP {} 登录尝试次数超限", ip);
            return Result.error("登录失败次数过多，请1分钟后再试");
        }

        // 按用户名限流：同一个用户名 1分钟内最多尝试3次
        if (rateLimiterUtil.isRateLimited("user:" + username, 3, 60)) {
            logger.warn("用户 {} 登录尝试次数超限", username);
            return Result.error("登录失败次数过多，请1分钟后再试");
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, loginRequest.getPassword()));

            SecurityContextHolder.getContext().setAuthentication(authentication);
            String jwt = jwtUtils.generateJwtToken(authentication);

            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            LoginResponse response = new LoginResponse(jwt, userPrincipal.getId(),
                    userPrincipal.getUsername(), userPrincipal.getEmail());

            // 登录成功，清除限流记录
            rateLimiterUtil.reset(ip);
            rateLimiterUtil.reset("user:" + username);

            logger.info("登录成功: {}", username);
            return Result.success("登录成功", response);

        } catch (Exception e) {
            logger.error("登录失败: {}", e.getMessage());

            // 登录失败，返回剩余次数
            long remaining = rateLimiterUtil.getRemaining(ip, 5);
            return Result.error("用户名或密码错误，还剩 " + remaining + " 次尝试机会");
        }
    }

    @PostMapping("/logout")
    public Result<?> logout() {
        logger.info("用户登出");
        return Result.success("登出成功");
    }

    // 测试端点：查看接收到的数据
    @PostMapping("/test")
    public Result<Map<String, Object>> test(@RequestBody Map<String, Object> payload) {
        logger.info("========== 测试接收数据 ==========");

        Map<String, Object> result = new HashMap<>();
        result.put("received_data", payload);
        result.put("contains_password", payload.containsKey("password"));
        result.put("password_value", payload.get("password"));

        // 打印所有接收到的字段
        payload.forEach((key, value) -> {
            logger.info("字段: {} = {}", key, value);
        });

        return Result.success("测试成功", result);
    }

    // 简化版注册（用于调试）
    @PostMapping("/register-simple")
    public Result<Map<String, String>> registerSimple(@RequestBody Map<String, String> payload) {
        logger.info("========== 简化版注册 ==========");

        Map<String, String> response = new HashMap<>();

        try {
            String name = payload.get("name");
            String password = payload.get("password");
            String age = payload.get("age");
            String email = payload.get("email");

            logger.info("提取的数据:");
            logger.info("  name: {}", name);
            logger.info("  password: {}", password);
            logger.info("  age: {}", age);
            logger.info("  email: {}", email);

            response.put("received_name", name);
            response.put("received_password", password != null ? "已接收" : "未接收");
            response.put("received_age", age);
            response.put("received_email", email);

            return Result.success("简化版注册成功", response);

        } catch (Exception e) {
            logger.error("简化版注册失败", e);
            return Result.error("简化版注册失败: " + e.getMessage());
        }
    }
}