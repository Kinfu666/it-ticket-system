package com.helpdesk.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.helpdesk.enums.Priority;

public class TicketForm {
    @NotBlank(message = "請輸入標題")
    @Size(max = 100, message = "標題最多 100 字元")
    private String title;

    @NotNull(message = "請選擇故障分類")
    private Long categoryId;

    @NotBlank(message = "請輸入位置")
    @Size(max = 100, message = "位置最多 100 字元")
    private String location;

    @NotBlank(message = "請輸入故障描述")
    @Size(max = 2000, message = "故障描述最多 2000 字元")
    private String description;

    @NotNull(message = "請選擇緊急程度")
    private Priority priority = Priority.MEDIUM;

    public TicketForm() {}

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }
}
