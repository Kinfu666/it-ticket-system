package com.helpdesk.config;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.helpdesk.service.CurrentUserService;

// 只加入 Security filter chain，避免 servlet 自動註冊而執行兩次。
public class LiveAccountFilter extends OncePerRequestFilter {
    private final CurrentUserService currentUsers;

    public LiveAccountFilter(CurrentUserService currentUsers) {
        this.currentUsers = currentUsers;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null
                && auth.isAuthenticated()
                && !(auth instanceof AnonymousAuthenticationToken)) {
            try {
                currentUsers.current();
            } catch (AccessDeniedException ex) {
                var session = request.getSession(false);
                if (session != null) {
                    session.invalidate();
                }
                SecurityContextHolder.clearContext();
                response.sendRedirect(request.getContextPath() + "/login?changed");
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
