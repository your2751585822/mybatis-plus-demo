package org.example.mybatisplusdemo.entity;

import lombok.Data;

@Data
public class LoginRequest {
    private String username;
    private String password;
}