package com.helpdesk.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public class VersionForm {
    @NotNull(message = "缺少案件版本，請重新整理")
    @PositiveOrZero(message = "案件版本不合法")
    private Long version;

    public VersionForm() {}

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
