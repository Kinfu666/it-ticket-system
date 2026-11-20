package com.helpdesk.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.helpdesk.enums.Role;

public class UserForm {
    @NotBlank(message = "請輸入帳號")
    @Size(min = 3, max = 50, message = "帳號須為 3 至 50 字元")
    private String username;

    @Size(max = 72, message = "密碼過長")
    private String password;

    @NotBlank(message = "請輸入姓名")
    @Size(max = 100, message = "姓名最多 100 字元")
    private String fullName;

    @NotNull(message = "請選擇角色")
    private Role role;

    @NotNull(message = "請選擇部門")
    private Long departmentId;

    private boolean enabled = true;

    public UserForm() {}

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
