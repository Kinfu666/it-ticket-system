package com.helpdesk.dto;

import com.helpdesk.enums.Priority;
import com.helpdesk.enums.TicketStatus;

public record TicketView(
        Long id,
        String ticketNo,
        String title,
        String description,
        String location,
        Long categoryId,
        String categoryName,
        Priority priority,
        TicketStatus status,
        Long requesterId,
        String requesterName,
        Long engineerId,
        String engineerName,
        String createdDisplay,
        String updatedDisplay,
        String closedDisplay,
        String returnReason,
        Long version) {}
