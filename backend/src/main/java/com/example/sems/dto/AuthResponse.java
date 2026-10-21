package com.example.sems.dto;

public class AuthResponse {
    public String token;
    public UserDto user;

    public AuthResponse(String token, UserDto user) {
        this.token = token;
        this.user = user;
    }
}
