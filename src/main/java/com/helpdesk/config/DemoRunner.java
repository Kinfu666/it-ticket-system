package com.helpdesk.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.helpdesk.service.DemoService;

@Component
@Profile("demo")
public class DemoRunner implements ApplicationRunner {
    private final DemoService demo;

    public DemoRunner(DemoService demo) {
        this.demo = demo;
    }

    @Override
    public void run(ApplicationArguments args) {
        demo.initialize();
    }
}
