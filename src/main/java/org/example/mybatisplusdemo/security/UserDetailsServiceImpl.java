package org.example.mybatisplusdemo.security;

import org.example.mybatisplusdemo.entity.User;
import org.example.mybatisplusdemo.entity.UserPrincipal;
import org.example.mybatisplusdemo.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {
    @Autowired
    private UserMapper userMapper;

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userMapper.selectByName(username);  // 需要添加这个方法

        if (user == null) {
            throw new UsernameNotFoundException("User Not Found with username: " + username);
        }

        return UserPrincipal.create(user);
    }
}