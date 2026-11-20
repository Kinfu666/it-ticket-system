package com.helpdesk.dto;

import com.helpdesk.enums.TicketStatus;

public record StatusCount(TicketStatus status, long total) {}
