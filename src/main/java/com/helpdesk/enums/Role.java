package com.helpdesk.enums;

public enum Role {
    ADMIN("管理員"),
    ENGINEER("工程師"),
    EMPLOYEE("員工");
    private final String label;

    Role(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
