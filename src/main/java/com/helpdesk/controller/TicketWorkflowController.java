package com.helpdesk.controller;

import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.helpdesk.dto.AssignmentForm;
import com.helpdesk.dto.RejectionForm;
import com.helpdesk.dto.RepairForm;
import com.helpdesk.dto.VersionForm;
import com.helpdesk.service.TicketWorkflowService;

@Controller
@RequestMapping("/tickets/{id}")
public class TicketWorkflowController {
    private final TicketWorkflowService workflow;

    public TicketWorkflowController(TicketWorkflowService workflow) {
        this.workflow = workflow;
    }

    private String done(Long id, RedirectAttributes flash, String message) {
        flash.addFlashAttribute("success", message);
        return "redirect:/tickets/" + id;
    }

    @PostMapping("/assign")
    public String assign(
            @PathVariable Long id,
            @Valid @ModelAttribute AssignmentForm form,
            RedirectAttributes flash) {
        workflow.assign(id, form);
        return done(id, flash, "案件已分派");
    }

    @PostMapping("/start")
    public String start(
            @PathVariable Long id,
            @Valid @ModelAttribute VersionForm form,
            RedirectAttributes flash) {
        workflow.start(id, form);
        return done(id, flash, "已開始維修");
    }

    @PostMapping("/resolve")
    public String resolve(
            @PathVariable Long id,
            @Valid @ModelAttribute RepairForm form,
            RedirectAttributes flash) {
        workflow.resolve(id, form);
        return done(id, flash, "維修結果已提交，等待申請人確認");
    }

    @PostMapping("/confirm")
    public String confirm(
            @PathVariable Long id,
            @Valid @ModelAttribute VersionForm form,
            RedirectAttributes flash) {
        workflow.confirm(id, form);
        return done(id, flash, "已確認修復並結案");
    }

    @PostMapping("/reject")
    public String reject(
            @PathVariable Long id,
            @Valid @ModelAttribute RejectionForm form,
            RedirectAttributes flash) {
        workflow.reject(id, form);
        return done(id, flash, "已退回工程師重新處理");
    }

    @PostMapping("/cancel")
    public String cancel(
            @PathVariable Long id,
            @Valid @ModelAttribute VersionForm form,
            RedirectAttributes flash) {
        workflow.cancel(id, form);
        return done(id, flash, "案件已取消");
    }
}
