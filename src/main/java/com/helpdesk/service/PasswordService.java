package com.helpdesk.service;

import java.nio.charset.StandardCharsets;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.helpdesk.dto.PasswordForm;
import com.helpdesk.exception.BusinessException;
import com.helpdesk.repository.UserRepository;

@Service
public class PasswordService {
    private final UserRepository users;
    private final CurrentUserService currentUsers;
    private final PasswordEncoder encoder;

    public PasswordService(
            UserRepository users, CurrentUserService currentUsers, PasswordEncoder encoder) {
        this.users = users;
        this.currentUsers = currentUsers;
        this.encoder = encoder;
    }

    @Transactional
    public void change(PasswordForm form) {
        var actor = currentUsers.current();
        var user = users.findById(actor.id()).orElseThrow(BusinessException::notFound);
        if (!encoder.matches(form.getOldPassword(), user.getPassword())) {
            throw new BusinessException("舊密碼不正確");
        }
        validate(form.getNewPassword());
        user.setPassword(encoder.encode(form.getNewPassword()));
    }

    public static void validate(String password) {
        if (password == null
                || password.length() < 8
                || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new BusinessException("密碼至少 8 字元，且不得超過 72 UTF-8 位元組");
        }
    }
}
