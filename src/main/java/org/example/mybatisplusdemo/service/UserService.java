package org.example.mybatisplusdemo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.example.mybatisplusdemo.entity.User;

// 继承 MyBatis-Plus 的 IService 接口
public interface UserService extends IService<User> {
    // 可以在这里添加自定义方法
    User findByName(String name);
}