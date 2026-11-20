package com.helpdesk.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.helpdesk.service.BootstrapService;

@Component
@Profile("!demo & !test")
public class BootstrapRunner implements ApplicationRunner {
    private final BootstrapService bootstrap;
    private final String username;
    private final String password;

    public BootstrapRunner(
            BootstrapService bootstrap,
            @Value("${HELPDESK_ADMIN_USERNAME:}") String username,
            @Value("${HELPDESK_ADMIN_PASSWORD:}") String password) {
        this.bootstrap = bootstrap;
        this.username = username;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        bootstrap.initialize(username, password);
    }
}
