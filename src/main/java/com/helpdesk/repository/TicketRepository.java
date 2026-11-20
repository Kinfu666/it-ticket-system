package com.helpdesk.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.helpdesk.entity.Ticket;
import com.helpdesk.enums.TicketStatus;

public interface TicketRepository
        extends JpaRepository<Ticket, Long>, JpaSpecificationExecutor<Ticket> {
    boolean existsByTicketNo(String ticketNo);

    @org.springframework.data.jpa.repository.Query(
            "select new com.helpdesk.dto.CategoryCount(c.name,count(t.id)) from Category c left join Ticket t on t.category.id=c.id group by c.id,c.name order by count(t.id) desc,c.name asc")
    List<com.helpdesk.dto.CategoryCount> countByCategory();

    long countByEngineerIdAndStatusNotIn(Long engineerId, Collection<TicketStatus> statuses);
}
