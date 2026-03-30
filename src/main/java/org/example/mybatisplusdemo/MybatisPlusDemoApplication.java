package org.example.mybatisplusdemo;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;  // 新增导入
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

@SpringBootApplication
@MapperScan("org.example.mybatisplusdemo.mapper")
@EnableCaching  // 新增注解，开启缓存支持
public class MybatisPlusDemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(MybatisPlusDemoApplication.class, args);
    }

    @Bean
    public ApplicationRunner diagnostic(ApplicationContext ctx) {
        return args -> {
            System.out.println("\n========== 诊断信息 ==========");

            // 1. 检查所有注册的 Controller
            System.out.println("注册的 Controller:");
            String[] controllerNames = ctx.getBeanNamesForAnnotation(RestController.class);
            if (controllerNames.length == 0) {
                System.out.println("  ❌ 没有找到任何 Controller！");
            } else {
                for (String name : controllerNames) {
                    System.out.println("  ✅ " + name + " : " + ctx.getBean(name).getClass().getName());
                }
            }

            // 2. 检查所有请求映射
            System.out.println("\n所有请求映射:");
            RequestMappingHandlerMapping mapping = ctx.getBean(RequestMappingHandlerMapping.class);
            if (mapping.getHandlerMethods().isEmpty()) {
                System.out.println("  ❌ 没有找到任何请求映射！");
            } else {
                mapping.getHandlerMethods().forEach((key, value) -> {
                    System.out.println("  ✅ " + key + " -> " + value.getMethod().getName());
                });
            }

            System.out.println("===============================\n");
        };
    }
}