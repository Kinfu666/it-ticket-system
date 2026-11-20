package com.helpdesk.enums;

public enum TicketStatus {
    PENDING("待分派"),
    ASSIGNED("待處理"),
    IN_PROGRESS("處理中"),
    RESOLVED("待確認"),
    CLOSED("已結案"),
    CANCELLED("已取消");
    private final String label;

    TicketStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
