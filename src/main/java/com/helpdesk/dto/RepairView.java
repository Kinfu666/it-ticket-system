package com.helpdesk.dto;

public record RepairView(
        Long id,
        Long ticketId,
        String ticketNo,
        String title,
        String engineerName,
        String cause,
        String solution,
        String result,
        String createdDisplay,
        boolean currentAssignment) {}
