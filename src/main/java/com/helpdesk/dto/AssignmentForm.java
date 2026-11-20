package com.helpdesk.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class AssignmentForm extends VersionForm {
    @NotNull(message = "請選擇工程師")
    @Positive(message = "工程師編號不合法")
    private Long engineerId;

    public AssignmentForm() {}

    public Long getEngineerId() {
        return engineerId;
    }

    public void setEngineerId(Long engineerId) {
        this.engineerId = engineerId;
    }
}
