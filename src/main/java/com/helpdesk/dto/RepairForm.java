package com.helpdesk.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RepairForm extends VersionForm {
    @NotBlank(message = "請填寫故障原因")
    @Size(max = 2000, message = "欄位最多 2000 字元")
    private String cause;

    @NotBlank(message = "請填寫處理方式")
    @Size(max = 2000, message = "欄位最多 2000 字元")
    private String solution;

    @NotBlank(message = "請填寫維修結果")
    @Size(max = 2000, message = "欄位最多 2000 字元")
    private String result;

    public RepairForm() {}

    public String getCause() {
        return cause;
    }

    public void setCause(String cause) {
        this.cause = cause;
    }

    public String getSolution() {
        return solution;
    }

    public void setSolution(String solution) {
        this.solution = solution;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }
}
