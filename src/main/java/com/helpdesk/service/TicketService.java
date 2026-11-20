package com.helpdesk.service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.helpdesk.dto.Actor;
import com.helpdesk.dto.MasterView;
import com.helpdesk.dto.RepairView;
import com.helpdesk.dto.TicketForm;
import com.helpdesk.dto.TicketQuery;
import com.helpdesk.dto.TicketView;
import com.helpdesk.entity.Category;
import com.helpdesk.entity.RepairRecord;
import com.helpdesk.entity.Ticket;
import com.helpdesk.enums.Role;
import com.helpdesk.exception.BusinessException;
import com.helpdesk.repository.CategoryRepository;
import com.helpdesk.repository.RepairRecordRepository;
import com.helpdesk.repository.TicketRepository;
import com.helpdesk.repository.UserRepository;

@Service
@Transactional(readOnly = true)
public class TicketService {
    public static final ZoneId DISPLAY_ZONE = ZoneId.of("Asia/Taipei");
    private static final DateTimeFormatter TIME =
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss").withZone(DISPLAY_ZONE);

    private final TicketRepository tickets;
    private final CategoryRepository categories;
    private final UserRepository users;
    private final RepairRecordRepository repairs;
    private final CurrentUserService currentUsers;

    public TicketService(
            TicketRepository tickets,
            CategoryRepository categories,
            UserRepository users,
            RepairRecordRepository repairs,
            CurrentUserService currentUsers) {
        this.tickets = tickets;
        this.categories = categories;
        this.users = users;
        this.repairs = repairs;
        this.currentUsers = currentUsers;
    }

    private static String display(Instant time) {
        return time == null ? "—" : TIME.format(time);
    }

    public TicketView view(Ticket t) {
        return new TicketView(
                t.getId(),
                t.getTicketNo(),
                t.getTitle(),
                t.getDescription(),
                t.getLocation(),
                t.getCategory().getId(),
                t.getCategory().getName(),
                t.getPriority(),
                t.getStatus(),
                t.getRequester().getId(),
                t.getRequester().getFullName(),
                t.getEngineer() == null ? null : t.getEngineer().getId(),
                t.getEngineer() == null ? "尚未分派" : t.getEngineer().getFullName(),
                display(t.getCreatedAt()),
                display(t.getUpdatedAt()),
                display(t.getClosedAt()),
                t.getReturnReason(),
                t.getVersion());
    }

    public static Specification<Ticket> scope(Actor actor) {
        // 先限制角色可見範圍，再疊加篩選條件，避免查詢時遺漏案件所有權。
        return (root, query, cb) ->
                switch (actor.role()) {
                    case ADMIN -> cb.conjunction();
                    case EMPLOYEE -> cb.equal(root.get("requester").get("id"), actor.id());
                    case ENGINEER -> cb.equal(root.get("engineer").get("id"), actor.id());
                };
    }

    public Page<TicketView> search(TicketQuery form) {
        Actor actor = currentUsers.current();
        Specification<Ticket> spec = scope(actor);
        if (form.getKeyword() != null && !form.getKeyword().isBlank()) {
            // 將使用者輸入的萬用字元當成一般文字，避免意外擴大 LIKE 搜尋範圍。
            String escaped =
                    form.getKeyword()
                            .trim()
                            .replace("\\", "\\\\")
                            .replace("%", "\\%")
                            .replace("_", "\\_");
            spec =
                    spec.and(
                            (root, q, cb) ->
                                    cb.or(
                                            cb.like(root.get("title"), "%" + escaped + "%", '\\'),
                                            cb.like(
                                                    root.get("ticketNo"),
                                                    "%" + escaped + "%",
                                                    '\\')));
        }
        if (form.getStatus() != null) {
            spec = spec.and((r, q, cb) -> cb.equal(r.get("status"), form.getStatus()));
        }
        if (form.getCategoryId() != null) {
            spec =
                    spec.and(
                            (r, q, cb) ->
                                    cb.equal(r.get("category").get("id"), form.getCategoryId()));
        }
        if (form.getEngineerId() != null) {
            if (actor.role() != Role.ADMIN) {
                throw new AccessDeniedException("只有管理員可依工程師查詢");
            }
            spec =
                    spec.and(
                            (r, q, cb) ->
                                    cb.equal(r.get("engineer").get("id"), form.getEngineerId()));
        }
        // 日期以台北時區解讀；結束日轉為次日零時，使用不含上界的區間涵蓋整天。
        if (form.getFrom() != null) {
            spec =
                    spec.and(
                            (r, q, cb) ->
                                    cb.greaterThanOrEqualTo(
                                            r.get("createdAt"),
                                            form.getFrom().atStartOfDay(DISPLAY_ZONE).toInstant()));
        }
        if (form.getTo() != null) {
            spec =
                    spec.and(
                            (r, q, cb) ->
                                    cb.lessThan(
                                            r.get("createdAt"),
                                            form.getTo()
                                                    .plusDays(1)
                                                    .atStartOfDay(DISPLAY_ZONE)
                                                    .toInstant()));
        }
        return tickets.findAll(
                        spec,
                        PageRequest.of(
                                form.getPage(),
                                form.getSize(),
                                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))))
                .map(this::view);
    }

    private Ticket readable(Long id) {
        Actor actor = currentUsers.current();
        Ticket t = tickets.findById(id).orElseThrow(BusinessException::notFound);
        if ((actor.role() == Role.EMPLOYEE && !actor.id().equals(t.getRequester().getId()))
                || (actor.role() == Role.ENGINEER
                        && (t.getEngineer() == null
                                || !actor.id().equals(t.getEngineer().getId())))) {
            throw new AccessDeniedException("您沒有此案件的存取權限");
        }
        return t;
    }

    public TicketView detail(Long id) {
        return view(readable(id));
    }

    private RepairView repairView(RepairRecord r, Actor actor) {
        var t = r.getTicket();
        return new RepairView(
                r.getId(),
                t.getId(),
                t.getTicketNo(),
                t.getTitle(),
                r.getEngineer().getFullName(),
                r.getCause(),
                r.getSolution(),
                r.getResult(),
                display(r.getCreatedAt()),
                t.getEngineer() != null && actor.id().equals(t.getEngineer().getId()));
    }

    public List<RepairView> records(Long id) {
        readable(id);
        Actor actor = currentUsers.current();
        return repairs.findByTicketIdOrderByCreatedAtAscIdAsc(id).stream()
                .map(r -> repairView(r, actor))
                .toList();
    }

    public List<RepairView> ownRepairs() {
        Actor actor = currentUsers.require(Role.ENGINEER);
        return repairs.findByEngineerIdOrderByCreatedAtDescIdDesc(actor.id()).stream()
                .map(r -> repairView(r, actor))
                .toList();
    }

    public List<MasterView> categoryOptions(boolean enabledOnly) {
        currentUsers.current();
        return (enabledOnly
                        ? categories.findByEnabledTrueOrderByNameAsc()
                        : categories.findAllByOrderByNameAsc())
                .stream()
                        .map(
                                c ->
                                        new MasterView(
                                                c.getId(),
                                                c.getName(),
                                                c.getDescription(),
                                                c.isEnabled()))
                        .toList();
    }

    @Transactional
    public Long create(TicketForm form) {
        Actor actor = currentUsers.require(Role.EMPLOYEE);
        Category category =
                categories.findById(form.getCategoryId()).orElseThrow(BusinessException::notFound);
        if (!category.isEnabled()) {
            throw new BusinessException("此故障分類已停用，請重新選擇");
        }
        Ticket t = new Ticket();
        t.setTicketNo("HD-" + UUID.randomUUID());
        t.setTitle(form.getTitle());
        t.setDescription(form.getDescription());
        t.setLocation(form.getLocation());
        t.setCategory(category);
        t.setPriority(form.getPriority());
        t.setRequester(users.findById(actor.id()).orElseThrow(BusinessException::notFound));
        return tickets.saveAndFlush(t).getId();
    }
}
