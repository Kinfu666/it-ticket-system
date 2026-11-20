package com.helpdesk.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.helpdesk.service.DashboardService;

@Controller
public class ReportController {
    private final DashboardService dashboard;

    public ReportController(DashboardService dashboard) {
        this.dashboard = dashboard;
    }

    @GetMapping("/admin/reports")
    public String reports(Model model) {
        model.addAttribute("data", dashboard.reports());
        return "admin/reports";
    }
}
