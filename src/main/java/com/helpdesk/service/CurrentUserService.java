package com.helpdesk.service;

import java.util.Arrays;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.helpdesk.dto.Actor;
import com.helpdesk.entity.User;
import com.helpdesk.enums.Role;
import com.helpdesk.repository.UserRepository;

@Service
@Transactional(readOnly = true)
public class CurrentUserService {
    private final UserRepository users;

    public CurrentUserService(UserRepository users) {
        this.users = users;
    }

    public Actor current() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("請先登入");
        }
        User user =
                users.findByUsername(auth.getName())
                        .orElseThrow(() -> new AccessDeniedException("帳號不存在"));
        // 每次重新讀取帳號，不能讓 Session 保留的舊角色繼續生效。
        if (!user.isEnabled()
                || auth.getAuthorities().stream()
                        .noneMatch(a -> a.getAuthority().equals("ROLE_" + user.getRole().name()))) {
            throw new AccessDeniedException("帳號狀態已變更，請重新登入");
        }
        return new Actor(
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getRole(),
                user.getDepartment().getName());
    }

    public Actor require(Role... roles) {
        Actor actor = current();
        if (Arrays.stream(roles).noneMatch(r -> r == actor.role())) {
            throw new AccessDeniedException("您沒有此操作權限");
        }
        return actor;
    }
}
