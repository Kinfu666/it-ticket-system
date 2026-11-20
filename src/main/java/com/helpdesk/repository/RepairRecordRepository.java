package com.helpdesk.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.helpdesk.entity.RepairRecord;

public interface RepairRecordRepository extends JpaRepository<RepairRecord, Long> {
    List<RepairRecord> findByTicketIdOrderByCreatedAtAscIdAsc(Long ticketId);

    List<RepairRecord> findByEngineerIdOrderByCreatedAtDescIdDesc(Long engineerId);
}
