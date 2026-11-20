package com.helpdesk.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RejectionForm extends VersionForm {
    @NotBlank(message = "請填寫退回原因")
    @Size(max = 2000, message = "退回原因最多 2000 字元")
    private String reason;

    public RejectionForm() {}

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
