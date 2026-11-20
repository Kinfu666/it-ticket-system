package com.helpdesk.service;

import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.helpdesk.dto.MasterForm;
import com.helpdesk.dto.MasterView;
import com.helpdesk.dto.UserForm;
import com.helpdesk.dto.UserView;
import com.helpdesk.entity.Category;
import com.helpdesk.entity.Department;
import com.helpdesk.entity.User;
import com.helpdesk.enums.Role;
import com.helpdesk.enums.TicketStatus;
import com.helpdesk.exception.BusinessException;
import com.helpdesk.repository.CategoryRepository;
import com.helpdesk.repository.DepartmentRepository;
import com.helpdesk.repository.TicketRepository;
import com.helpdesk.repository.UserRepository;

@Service
@Transactional(readOnly = true)
public class DirectoryService {
    private final UserRepository users;
    private final DepartmentRepository departments;
    private final CategoryRepository categories;
    private final TicketRepository tickets;
    private final CurrentUserService currentUsers;
    private final PasswordEncoder encoder;

    public DirectoryService(
            UserRepository users,
            DepartmentRepository departments,
            CategoryRepository categories,
            TicketRepository tickets,
            CurrentUserService currentUsers,
            PasswordEncoder encoder) {
        this.users = users;
        this.departments = departments;
        this.categories = categories;
        this.tickets = tickets;
        this.currentUsers = currentUsers;
        this.encoder = encoder;
    }

    private UserView view(User u) {
        return new UserView(
                u.getId(),
                u.getUsername(),
                u.getFullName(),
                u.getRole(),
                u.getDepartment().getId(),
                u.getDepartment().getName(),
                u.isEnabled());
    }

    public List<UserView> users(Long departmentId) {
        currentUsers.require(Role.ADMIN);
        return (departmentId == null
                        ? users.findAllByOrderByIdAsc()
                        : users.findByDepartmentIdOrderByFullNameAsc(departmentId))
                .stream().map(this::view).toList();
    }

    public List<UserView> engineers() {
        currentUsers.require(Role.ADMIN);
        return users.findByRoleAndEnabledTrueOrderByFullNameAsc(Role.ENGINEER).stream()
                .map(this::view)
                .toList();
    }

    public UserForm userForm(Long id) {
        currentUsers.require(Role.ADMIN);
        User u = users.findById(id).orElseThrow(BusinessException::notFound);
        UserForm form = new UserForm();
        form.setUsername(u.getUsername());
        form.setFullName(u.getFullName());
        form.setRole(u.getRole());
        form.setDepartmentId(u.getDepartment().getId());
        form.setEnabled(u.isEnabled());
        return form;
    }

    // 角色變更與停用共用同一組規則，避免不同入口漏掉歷史案件保護。
    private void guardAccountChange(User user, Role role, boolean enabled) {
        if (user.getRole() == Role.ADMIN
                && user.isEnabled()
                && (role != Role.ADMIN || !enabled)
                && users.countByRoleAndEnabledTrue(Role.ADMIN) <= 1) {
            throw new BusinessException("不得停用或變更最後一位有效管理員");
        }
        if (user.getRole() == Role.ENGINEER
                && (role != Role.ENGINEER || !enabled)
                && tickets.countByEngineerIdAndStatusNotIn(
                                user.getId(), List.of(TicketStatus.CLOSED, TicketStatus.CANCELLED))
                        > 0) {
            throw new BusinessException("此工程師仍有未完成案件，請先重新分派；待確認案件須先由申請人確認或退回");
        }
    }

    @Transactional
    public void saveUser(Long id, UserForm form) {
        currentUsers.require(Role.ADMIN);
        User user =
                id == null
                        ? new User()
                        : users.findById(id).orElseThrow(BusinessException::notFound);
        if (id == null
                ? users.existsByUsername(form.getUsername())
                : users.existsByUsernameAndIdNot(form.getUsername(), id)) {
            throw new BusinessException("帳號已存在");
        }
        Department department =
                departments
                        .findById(form.getDepartmentId())
                        .orElseThrow(BusinessException::notFound);
        // 停用部門保留既有關聯，但不能再指派其他帳號進入該部門。
        if (!department.isEnabled()
                && (id == null || !department.getId().equals(user.getDepartment().getId()))) {
            throw new BusinessException("不可將帳號指派至停用部門");
        }
        if (id != null) {
            guardAccountChange(user, form.getRole(), form.isEnabled());
        }
        if (id == null || (form.getPassword() != null && !form.getPassword().isEmpty())) {
            PasswordService.validate(form.getPassword());
            user.setPassword(encoder.encode(form.getPassword()));
        }
        user.setUsername(form.getUsername());
        user.setFullName(form.getFullName());
        user.setRole(form.getRole());
        user.setDepartment(department);
        user.setEnabled(form.isEnabled());
        users.saveAndFlush(user);
    }

    @Transactional
    public void setUserEnabled(Long id, boolean enabled) {
        currentUsers.require(Role.ADMIN);
        User user = users.findById(id).orElseThrow(BusinessException::notFound);
        guardAccountChange(user, user.getRole(), enabled);
        user.setEnabled(enabled);
        users.flush();
    }

    public List<MasterView> masters(String kind) {
        currentUsers.require(Role.ADMIN);
        if (kind.equals("departments")) {
            return departments.findAllByOrderByNameAsc().stream()
                    .map(d -> new MasterView(d.getId(), d.getName(), null, d.isEnabled()))
                    .toList();
        }
        return categories.findAllByOrderByNameAsc().stream()
                .map(c -> new MasterView(c.getId(), c.getName(), c.getDescription(), c.isEnabled()))
                .toList();
    }

    public MasterForm masterForm(String kind, Long id) {
        var item =
                masters(kind).stream()
                        .filter(v -> v.id().equals(id))
                        .findFirst()
                        .orElseThrow(BusinessException::notFound);
        MasterForm form = new MasterForm();
        form.setName(item.name());
        form.setDescription(item.description());
        form.setEnabled(item.enabled());
        return form;
    }

    @Transactional
    public void saveMaster(String kind, Long id, MasterForm form) {
        currentUsers.require(Role.ADMIN);
        if (kind.equals("departments")) {
            if (id == null
                    ? departments.existsByName(form.getName())
                    : departments.existsByNameAndIdNot(form.getName(), id)) {
                throw new BusinessException("部門名稱已存在");
            }
            Department d =
                    id == null
                            ? new Department()
                            : departments.findById(id).orElseThrow(BusinessException::notFound);
            d.setName(form.getName());
            d.setEnabled(form.isEnabled());
            departments.saveAndFlush(d);
        } else {
            if (id == null
                    ? categories.existsByName(form.getName())
                    : categories.existsByNameAndIdNot(form.getName(), id)) {
                throw new BusinessException("分類名稱已存在");
            }
            Category c =
                    id == null
                            ? new Category()
                            : categories.findById(id).orElseThrow(BusinessException::notFound);
            c.setName(form.getName());
            c.setDescription(form.getDescription());
            c.setEnabled(form.isEnabled());
            categories.saveAndFlush(c);
        }
    }

    @Transactional
    public void setMasterEnabled(String kind, Long id, boolean enabled) {
        currentUsers.require(Role.ADMIN);
        if (kind.equals("departments")) {
            departments.findById(id).orElseThrow(BusinessException::notFound).setEnabled(enabled);
        } else {
            categories.findById(id).orElseThrow(BusinessException::notFound).setEnabled(enabled);
        }
    }
}
