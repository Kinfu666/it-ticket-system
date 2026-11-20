package com.helpdesk.service;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.helpdesk.dto.Actor;
import com.helpdesk.dto.AssignmentForm;
import com.helpdesk.dto.RejectionForm;
import com.helpdesk.dto.RepairForm;
import com.helpdesk.dto.VersionForm;
import com.helpdesk.entity.RepairRecord;
import com.helpdesk.entity.Ticket;
import com.helpdesk.entity.User;
import com.helpdesk.enums.Role;
import com.helpdesk.enums.TicketStatus;
import com.helpdesk.exception.BusinessException;
import com.helpdesk.repository.RepairRecordRepository;
import com.helpdesk.repository.TicketRepository;
import com.helpdesk.repository.UserRepository;

@Service
@Transactional
public class TicketWorkflowService {
    private final TicketRepository tickets;
    private final UserRepository users;
    private final RepairRecordRepository repairs;
    private final CurrentUserService currentUsers;

    public TicketWorkflowService(
            TicketRepository tickets,
            UserRepository users,
            RepairRecordRepository repairs,
            CurrentUserService currentUsers) {
        this.tickets = tickets;
        this.users = users;
        this.repairs = repairs;
        this.currentUsers = currentUsers;
    }

    private Ticket load(Long id, Long version) {
        Ticket t = tickets.findById(id).orElseThrow(BusinessException::notFound);
        // 比對使用者看到的版本，攔截重送舊表單；@Version 再攔截交易間的競爭更新。
        if (version == null || !version.equals(t.getVersion())) {
            throw new BusinessException(HttpStatus.CONFLICT, "案件已更新，請重新整理後再操作");
        }
        return t;
    }

    private void expect(Ticket t, TicketStatus... allowed) {
        if (Arrays.stream(allowed).noneMatch(s -> s == t.getStatus())) {
            throw new BusinessException("目前案件狀態不允許此操作");
        }
    }

    private void owner(Actor actor, Ticket t) {
        if (!actor.id().equals(t.getRequester().getId())) {
            throw new AccessDeniedException("只能操作本人申請案件");
        }
    }

    private void engineer(Actor actor, Ticket t) {
        if (t.getEngineer() == null || !actor.id().equals(t.getEngineer().getId())) {
            throw new AccessDeniedException("只能操作目前分派給自己的案件");
        }
    }

    private void update(Ticket t, TicketStatus status) {
        t.setStatus(status);
        t.setUpdatedAt(Instant.now());
        tickets.flush();
    }

    public void assign(Long id, AssignmentForm form) {
        currentUsers.require(Role.ADMIN);
        Ticket t = load(id, form.getVersion());
        expect(t, TicketStatus.PENDING, TicketStatus.ASSIGNED, TicketStatus.IN_PROGRESS);
        User engineer =
                users.findById(form.getEngineerId()).orElseThrow(BusinessException::notFound);
        if (!engineer.isEnabled() || engineer.getRole() != Role.ENGINEER) {
            throw new BusinessException("只能分派給啟用中的工程師");
        }
        t.setEngineer(engineer);
        update(t, TicketStatus.ASSIGNED);
    }

    public void start(Long id, VersionForm form) {
        Actor actor = currentUsers.require(Role.ENGINEER);
        Ticket t = load(id, form.getVersion());
        engineer(actor, t);
        expect(t, TicketStatus.ASSIGNED);
        update(t, TicketStatus.IN_PROGRESS);
    }

    public void resolve(Long id, RepairForm form) {
        Actor actor = currentUsers.require(Role.ENGINEER);
        Ticket t = load(id, form.getVersion());
        engineer(actor, t);
        expect(t, TicketStatus.IN_PROGRESS);
        for (String value :
                List.of(
                        Objects.toString(form.getCause(), ""),
                        Objects.toString(form.getSolution(), ""),
                        Objects.toString(form.getResult(), ""))) {
            if (value.isBlank() || value.length() > 2000) {
                throw new BusinessException("故障原因、處理方式與維修結果皆為必填，最多 2000 字元");
            }
        }
        // 先寫狀態再寫維修紀錄，任一步失敗都由同一交易 rollback，避免只完成一半。
        update(t, TicketStatus.RESOLVED);
        RepairRecord record = new RepairRecord();
        record.setTicket(t);
        record.setEngineer(users.findById(actor.id()).orElseThrow(BusinessException::notFound));
        record.setCause(form.getCause());
        record.setSolution(form.getSolution());
        record.setResult(form.getResult());
        repairs.saveAndFlush(record);
    }

    public void confirm(Long id, VersionForm form) {
        Actor actor = currentUsers.require(Role.EMPLOYEE);
        Ticket t = load(id, form.getVersion());
        owner(actor, t);
        expect(t, TicketStatus.RESOLVED);
        t.setClosedAt(Instant.now());
        update(t, TicketStatus.CLOSED);
    }

    public void reject(Long id, RejectionForm form) {
        Actor actor = currentUsers.require(Role.EMPLOYEE);
        Ticket t = load(id, form.getVersion());
        owner(actor, t);
        expect(t, TicketStatus.RESOLVED);
        if (form.getReason() == null
                || form.getReason().isBlank()
                || form.getReason().length() > 2000) {
            throw new BusinessException("請填寫退回原因，最多 2000 字元");
        }
        t.setReturnReason(form.getReason());
        update(t, TicketStatus.IN_PROGRESS);
    }

    public void cancel(Long id, VersionForm form) {
        Actor actor = currentUsers.require(Role.EMPLOYEE);
        Ticket t = load(id, form.getVersion());
        owner(actor, t);
        expect(t, TicketStatus.PENDING);
        update(t, TicketStatus.CANCELLED);
    }
}
