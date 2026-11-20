package com.helpdesk.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import com.helpdesk.enums.TicketStatus;

public class TicketQuery {
    @Size(max = 100, message = "關鍵字最多 100 字元")
    private String keyword;

    private TicketStatus status;

    @Positive(message = "分類編號不合法")
    private Long categoryId;

    @Positive(message = "工程師編號不合法")
    private Long engineerId;

    @org.springframework.format.annotation.DateTimeFormat(
            iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
    private java.time.LocalDate from;

    @org.springframework.format.annotation.DateTimeFormat(
            iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
    private java.time.LocalDate to;

    @Min(value = 0, message = "頁碼不可為負數")
    @Max(value = 1000000, message = "頁碼過大")
    private int page = 0;

    @Min(value = 1, message = "每頁至少 1 筆")
    @Max(value = 100, message = "每頁最多 100 筆")
    private int size = 10;

    public TicketQuery() {}

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public TicketStatus getStatus() {
        return status;
    }

    public void setStatus(TicketStatus status) {
        this.status = status;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public Long getEngineerId() {
        return engineerId;
    }

    public void setEngineerId(Long engineerId) {
        this.engineerId = engineerId;
    }

    public java.time.LocalDate getFrom() {
        return from;
    }

    public void setFrom(java.time.LocalDate from) {
        this.from = from;
    }

    public java.time.LocalDate getTo() {
        return to;
    }

    public void setTo(java.time.LocalDate to) {
        this.to = to;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    @AssertTrue(message = "起始日期不可晚於結束日期")
    public boolean isDateRangeValid() {
        return from == null || to == null || !from.isAfter(to);
    }
}
