package com.helpdesk.controller;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.helpdesk.dto.TicketForm;
import com.helpdesk.dto.TicketQuery;
import com.helpdesk.enums.Priority;
import com.helpdesk.enums.Role;
import com.helpdesk.enums.TicketStatus;
import com.helpdesk.exception.BusinessException;
import com.helpdesk.service.CurrentUserService;
import com.helpdesk.service.DirectoryService;
import com.helpdesk.service.TicketService;

@Controller
@RequestMapping("/tickets")
public class TicketController {
    private final TicketService tickets;
    private final CurrentUserService currentUsers;
    private final DirectoryService directory;

    public TicketController(
            TicketService tickets, CurrentUserService currentUsers, DirectoryService directory) {
        this.tickets = tickets;
        this.currentUsers = currentUsers;
        this.directory = directory;
    }

    @GetMapping
    public String list(
            @Valid @ModelAttribute("query") TicketQuery query, BindingResult errors, Model model) {
        model.addAttribute("items", errors.hasErrors() ? Page.empty() : tickets.search(query));
        model.addAttribute("categories", tickets.categoryOptions(false));
        model.addAttribute("statuses", TicketStatus.values());
        return "tickets/list";
    }

    private String formPage(Model model) {
        model.addAttribute("categories", tickets.categoryOptions(true));
        model.addAttribute("priorities", Priority.values());
        return "tickets/new";
    }

    @GetMapping("/new")
    public String create(Model model) {
        currentUsers.require(Role.EMPLOYEE);
        model.addAttribute("form", new TicketForm());
        return formPage(model);
    }

    @PostMapping
    public String save(
            @Valid @ModelAttribute("form") TicketForm form,
            BindingResult errors,
            Model model,
            RedirectAttributes flash) {
        if (!errors.hasErrors()) {
            try {
                Long id = tickets.create(form);
                flash.addFlashAttribute("success", "報修申請已送出");
                return "redirect:/tickets/" + id;
            } catch (BusinessException ex) {
                errors.reject("ticket", ex.getMessage());
            }
        }
        return formPage(model);
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        if (currentUsers.current().role() == Role.ADMIN) {
            model.addAttribute("engineers", directory.engineers());
        }
        model.addAttribute("ticket", tickets.detail(id));
        model.addAttribute("records", tickets.records(id));
        return "tickets/detail";
    }

    @GetMapping("/repairs")
    public String repairs(Model model) {
        model.addAttribute("records", tickets.ownRepairs());
        return "tickets/repairs";
    }
}
