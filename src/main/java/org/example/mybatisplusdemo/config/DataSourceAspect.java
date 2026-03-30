package org.example.mybatisplusdemo.config;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Aspect
@Component
@Order(1)
public class DataSourceAspect {

    @Pointcut("execution(* org.example.mybatisplusdemo.service.*.*(..))")
    public void servicePointcut() {}

    @Before("servicePointcut()")
    public void before(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        // 如果方法有 @Master 注解，强制走主库
        if (method.isAnnotationPresent(Master.class)) {
            DataSourceContextHolder.setDataSourceType("master");
            System.out.println("【数据源切换】" + method.getName() + " → 主库（@Master注解）");
            return;
        }

        String methodName = method.getName();

        // 读方法：走从库
        if (methodName.startsWith("get") ||
                methodName.startsWith("find") ||
                methodName.startsWith("select") ||
                methodName.startsWith("query") ||
                methodName.startsWith("list") ||
                methodName.startsWith("page")) {
            DataSourceContextHolder.setDataSourceType("slave");
            System.out.println("【数据源切换】" + methodName + " → 从库（读操作）");
        }
        // 写方法：走主库
        else {
            DataSourceContextHolder.setDataSourceType("master");
            System.out.println("【数据源切换】" + methodName + " → 主库（写操作）");
        }
    }

    @After("servicePointcut()")
    public void after(JoinPoint joinPoint) {
        DataSourceContextHolder.clear();
    }
}