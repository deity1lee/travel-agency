package com.epam.edp.demo.controller;

import com.epam.edp.demo.entity.auth.Role;

public record UserContext(
        String userId,
        String email,
        Role role
){}
