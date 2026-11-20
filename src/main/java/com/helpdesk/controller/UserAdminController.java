package com.helpdesk.controller;

import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.helpdesk.dto.UserForm;
import com.helpdesk.enums.Role;
import com.helpdesk.exception.BusinessException;
import com.helpdesk.service.DirectoryService;

@Controller
@RequestMapping("/admin/users")
public class UserAdminController {
    private final DirectoryService directory;

    public UserAdminController(DirectoryService directory) {
        this.directory = directory;
    }

    @GetMapping
    public String list(@RequestParam(required = false) Long departmentId, Model model) {
        model.addAttribute("items", directory.users(departmentId));
        model.addAttribute("departments", directory.masters("departments"));
        model.addAttribute("departmentId", departmentId);
        return "admin/users";
    }

    private String page(Long id, Model model) {
        model.addAttribute("id", id);
        model.addAttribute("roles", Role.values());
        model.addAttribute("departments", directory.masters("departments"));
        model.addAttribute(
                "action", id == null ? "/admin/users/new" : "/admin/users/" + id + "/edit");
        return "admin/user-form";
    }

    @GetMapping("/new")
    public String create(Model model) {
        model.addAttribute("form", new UserForm());
        return page(null, model);
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Long id, Model model) {
        model.addAttribute("form", directory.userForm(id));
        return page(id, model);
    }

    @PostMapping({"/new", "/{id}/edit"})
    public String save(
            @PathVariable(required = false) Long id,
            @Valid @ModelAttribute("form") UserForm form,
            BindingResult errors,
            Model model,
            RedirectAttributes flash) {
        if (!errors.hasErrors()) {
            try {
                directory.saveUser(id, form);
            } catch (BusinessException ex) {
                errors.reject("user", ex.getMessage());
            }
        }
        if (errors.hasErrors()) {
            return page(id, model);
        }
        flash.addFlashAttribute("success", "帳號已儲存");
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/enabled")
    public String enabled(
            @PathVariable Long id, @RequestParam boolean enabled, RedirectAttributes flash) {
        directory.setUserEnabled(id, enabled);
        flash.addFlashAttribute("success", "帳號狀態已更新");
        return "redirect:/admin/users";
    }
}
