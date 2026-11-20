package com.helpdesk.dto;

import com.helpdesk.enums.Role;

public record Actor(Long id, String username, String fullName, Role role, String departmentName) {}
