package com.helpdesk.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class MasterForm {
    @NotBlank(message = "請輸入名稱")
    @Size(max = 100, message = "名稱最多 100 字元")
    private String name;

    @Size(max = 2000, message = "說明最多 2000 字元")
    private String description;

    private boolean enabled = true;

    public MasterForm() {}

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
