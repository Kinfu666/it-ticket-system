package com.helpdesk.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {
    private final com.helpdesk.service.DashboardService dashboard;

    public HomeController(com.helpdesk.service.DashboardService dashboard) {
        this.dashboard = dashboard;
    }

    @GetMapping("/")
    public String home() {
        return "redirect:/dashboard";
    }

    @GetMapping("/dashboard")
    public String dashboard(org.springframework.ui.Model model) {
        model.addAttribute("data", dashboard.summary());
        return "dashboard/index";
    }
}
