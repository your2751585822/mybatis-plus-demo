package org.example.mybatisplusdemo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.mybatisplusdemo.entity.User;
import org.example.mybatisplusdemo.mapper.UserMapper;
import org.example.mybatisplusdemo.service.UserService;
import org.springframework.stereotype.Service;

@Service  // 重要：添加 @Service 注解
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Override
    public User findByName(String name) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getName, name);
        return this.getOne(wrapper);
    }
}