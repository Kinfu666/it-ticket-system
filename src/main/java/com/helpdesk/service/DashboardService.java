package com.helpdesk.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.helpdesk.dto.Actor;
import com.helpdesk.dto.CategoryCount;
import com.helpdesk.dto.DashboardData;
import com.helpdesk.dto.StatusCount;
import com.helpdesk.entity.Ticket;
import com.helpdesk.enums.Role;
import com.helpdesk.enums.TicketStatus;
import com.helpdesk.repository.TicketRepository;

@Service
@Transactional(readOnly = true)
public class DashboardService {
    private final TicketRepository tickets;
    private final CurrentUserService currentUsers;
    private final TicketService ticketService;

    public DashboardService(
            TicketRepository tickets,
            CurrentUserService currentUsers,
            TicketService ticketService) {
        this.tickets = tickets;
        this.currentUsers = currentUsers;
        this.ticketService = ticketService;
    }

    public DashboardData summary() {
        Actor actor = currentUsers.current();
        Specification<Ticket> scope = TicketService.scope(actor);
        // 總數、各狀態與近期案件使用相同資料範圍，避免統計洩漏其他角色的案件。
        List<StatusCount> counts = new ArrayList<>();
        for (TicketStatus status : TicketStatus.values()) {
            counts.add(
                    new StatusCount(
                            status,
                            tickets.count(
                                    scope.and((r, q, cb) -> cb.equal(r.get("status"), status)))));
        }
        long total = counts.stream().mapToLong(StatusCount::total).sum();
        long terminal =
                counts.stream()
                        .filter(
                                c ->
                                        c.status() == TicketStatus.CLOSED
                                                || c.status() == TicketStatus.CANCELLED)
                        .mapToLong(StatusCount::total)
                        .sum();
        long awaiting =
                counts.stream()
                        .filter(c -> c.status() == TicketStatus.RESOLVED)
                        .findFirst()
                        .orElseThrow()
                        .total();
        var recent =
                tickets.findAll(
                                scope,
                                PageRequest.of(
                                        0,
                                        5,
                                        Sort.by(
                                                Sort.Order.desc("createdAt"),
                                                Sort.Order.desc("id"))))
                        .map(ticketService::view)
                        .getContent();
        List<CategoryCount> categories =
                actor.role() == Role.ADMIN ? tickets.countByCategory() : List.of();
        long max = categories.stream().mapToLong(CategoryCount::total).max().orElse(0);
        return new DashboardData(
                total,
                total - terminal,
                awaiting,
                List.copyOf(counts),
                recent,
                categories,
                Math.max(1, max));
    }

    public DashboardData reports() {
        currentUsers.require(Role.ADMIN);
        return summary();
    }
}
