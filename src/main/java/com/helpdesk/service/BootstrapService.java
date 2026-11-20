package com.helpdesk.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.helpdesk.entity.Department;
import com.helpdesk.entity.User;
import com.helpdesk.enums.Role;
import com.helpdesk.repository.DepartmentRepository;
import com.helpdesk.repository.UserRepository;

@Service
public class BootstrapService {
    private final UserRepository users;
    private final DepartmentRepository departments;
    private final PasswordEncoder encoder;

    public BootstrapService(
            UserRepository users, DepartmentRepository departments, PasswordEncoder encoder) {
        this.users = users;
        this.departments = departments;
        this.encoder = encoder;
    }

    @Transactional
    public void initialize(String username, String password) {
        if (users.count() > 0) {
            return;
        }
        if (username == null
                || username.isBlank()
                || username.trim().length() < 3
                || username.trim().length() > 50) {
            throw new IllegalStateException("首次啟動須設定 HELPDESK_ADMIN_USERNAME（3 至 50 字元）");
        }
        PasswordService.validate(password);
        Department department = new Department();
        department.setName("資訊管理部");
        departments.save(department);
        User admin = new User();
        admin.setUsername(username.trim());
        admin.setFullName("系統管理員");
        admin.setPassword(encoder.encode(password));
        admin.setRole(Role.ADMIN);
        admin.setDepartment(department);
        users.save(admin);
    }
}
