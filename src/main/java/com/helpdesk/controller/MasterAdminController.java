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

import com.helpdesk.dto.MasterForm;
import com.helpdesk.exception.BusinessException;
import com.helpdesk.service.DirectoryService;

@Controller
@RequestMapping("/admin")
public class MasterAdminController {
    private final DirectoryService directory;

    public MasterAdminController(DirectoryService directory) {
        this.directory = directory;
    }

    private void attributes(String kind, Model model) {
        model.addAttribute("kind", kind);
        model.addAttribute("label", kind.equals("departments") ? "部門" : "故障分類");
    }

    @GetMapping("/{kind:departments|categories}")
    public String list(@PathVariable String kind, Model model) {
        attributes(kind, model);
        model.addAttribute("items", directory.masters(kind));
        return "admin/masters";
    }

    private String page(String kind, Long id, Model model) {
        attributes(kind, model);
        model.addAttribute("id", id);
        model.addAttribute("action", "/admin/" + kind + (id == null ? "/new" : "/" + id + "/edit"));
        return "admin/master-form";
    }

    @GetMapping("/{kind:departments|categories}/new")
    public String create(@PathVariable String kind, Model model) {
        model.addAttribute("form", new MasterForm());
        return page(kind, null, model);
    }

    @GetMapping("/{kind:departments|categories}/{id}/edit")
    public String edit(@PathVariable String kind, @PathVariable Long id, Model model) {
        model.addAttribute("form", directory.masterForm(kind, id));
        return page(kind, id, model);
    }

    @PostMapping({"/{kind:departments|categories}/new", "/{kind:departments|categories}/{id}/edit"})
    public String save(
            @PathVariable String kind,
            @PathVariable(required = false) Long id,
            @Valid @ModelAttribute("form") MasterForm form,
            BindingResult errors,
            Model model,
            RedirectAttributes flash) {
        if (!errors.hasErrors()) {
            try {
                directory.saveMaster(kind, id, form);
            } catch (BusinessException ex) {
                errors.reject("master", ex.getMessage());
            }
        }
        if (errors.hasErrors()) {
            return page(kind, id, model);
        }
        flash.addFlashAttribute("success", "資料已儲存");
        return "redirect:/admin/" + kind;
    }

    @PostMapping("/{kind:departments|categories}/{id}/enabled")
    public String enabled(
            @PathVariable String kind,
            @PathVariable Long id,
            @RequestParam boolean enabled,
            RedirectAttributes flash) {
        directory.setMasterEnabled(kind, id, enabled);
        flash.addFlashAttribute("success", "狀態已更新");
        return "redirect:/admin/" + kind;
    }
}
