package com.helpdesk.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import com.helpdesk.dto.PasswordForm;
import com.helpdesk.exception.BusinessException;
import com.helpdesk.service.PasswordService;

@Controller
public class AuthController {
    private final PasswordService passwords;

    public AuthController(PasswordService passwords) {
        this.passwords = passwords;
    }

    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }

    @GetMapping("/profile/password")
    public String password(Model model) {
        model.addAttribute("form", new PasswordForm());
        return "auth/password";
    }

    @PostMapping("/profile/password")
    public String change(
            @Valid @ModelAttribute("form") PasswordForm form,
            BindingResult errors,
            HttpServletRequest request) {
        if (!errors.hasErrors()) {
            try {
                passwords.change(form);
            } catch (BusinessException ex) {
                errors.reject("password", ex.getMessage());
            }
        }
        if (errors.hasErrors()) {
            return "auth/password";
        }
        request.getSession().invalidate();
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
        return "redirect:/login?passwordChanged";
    }
}
