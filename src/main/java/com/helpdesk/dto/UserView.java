package com.helpdesk.dto;

import com.helpdesk.enums.Role;

public record UserView(
        Long id,
        String username,
        String fullName,
        Role role,
        Long departmentId,
        String departmentName,
        boolean enabled) {}
