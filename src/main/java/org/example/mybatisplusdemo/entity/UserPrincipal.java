package org.example.mybatisplusdemo.entity;

import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

@Data
public class UserPrincipal implements UserDetails {
    private Long id;
    private String username;
    private String password;
    private String email;
    private Collection<? extends GrantedAuthority> authorities;

    public static UserPrincipal create(User user) {
        UserPrincipal principal = new UserPrincipal();
        principal.setId(user.getId());
        principal.setUsername(user.getName());
        principal.setPassword(user.getPassword());  // 需要在User实体中添加password字段
        principal.setEmail(user.getEmail());
        principal.setAuthorities(Collections.emptyList());
        return principal;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}