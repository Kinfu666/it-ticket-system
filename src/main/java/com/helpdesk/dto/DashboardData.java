package com.helpdesk.dto;

import java.util.List;

public record DashboardData(
        long total,
        long active,
        long awaitingConfirmation,
        List<StatusCount> statuses,
        List<TicketView> recent,
        List<CategoryCount> categories,
        long maxCategory) {}
