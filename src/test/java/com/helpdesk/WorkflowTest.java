package com.helpdesk;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.helpdesk.entity.Ticket;
import com.helpdesk.entity.User;
import com.helpdesk.enums.TicketStatus;
import com.helpdesk.repository.RepairRecordRepository;
import com.helpdesk.repository.TicketRepository;
import com.helpdesk.repository.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 刻意不包測試交易，讓每次 HTTP 請求實際 commit / rollback，再重新讀取資料庫驗證。
@SpringBootTest
@AutoConfigureMockMvc
class WorkflowTest extends SqliteTestSupport {
    private final MockMvc mvc;
    private final TestFixtures fixtures;
    private final TicketRepository tickets;
    private final RepairRecordRepository repairs;
    private final UserRepository users;
    private final JdbcTemplate jdbc;
    private TestFixtures.Fixture f;

    @Autowired
    WorkflowTest(
            MockMvc mvc,
            TestFixtures fixtures,
            TicketRepository tickets,
            RepairRecordRepository repairs,
            UserRepository users,
            JdbcTemplate jdbc) {
        this.mvc = mvc;
        this.fixtures = fixtures;
        this.tickets = tickets;
        this.repairs = repairs;
        this.users = users;
        this.jdbc = jdbc;
    }

    @BeforeEach
    void setup() {
        f = fixtures.create();
    }

    @AfterEach
    void cleanup() {
        jdbc.execute("DROP TRIGGER IF EXISTS fail_repair_test");
        for (String table :
                new String[] {"repair_records", "tickets", "users", "categories", "departments"}) {
            jdbc.update("delete from " + table);
        }
    }

    private Ticket load(Ticket t) {
        return tickets.findById(t.getId()).orElseThrow();
    }

    private MockHttpServletRequestBuilder action(
            Ticket t, String operation, String username, String role) {
        return post("/tickets/" + t.getId() + "/" + operation)
                .with(user(username).roles(role))
                .with(csrf())
                .param("version", load(t).getVersion().toString());
    }

    private MockHttpServletRequestBuilder resolve(Ticket t, String engineer) {
        return action(t, "resolve", engineer, "ENGINEER")
                .param("cause", "電源異常")
                .param("solution", "更換電源")
                .param("result", "測試正常");
    }

    @Test
    void employeeToAdminToEngineerToConfirmationAndTerminalProtection() throws Exception {
        var created =
                mvc.perform(
                                post("/tickets")
                                        .with(user("fixture_employee").roles("EMPLOYEE"))
                                        .with(csrf())
                                        .param("title", "流程驗收")
                                        .param("description", "無法開機")
                                        .param("location", "一樓")
                                        .param("priority", "HIGH")
                                        .param("categoryId", f.category().getId().toString()))
                        .andExpect(status().is3xxRedirection())
                        .andReturn();
        Long id =
                Long.valueOf(
                        created.getResponse().getRedirectedUrl().substring("/tickets/".length()));
        Ticket t = tickets.findById(id).orElseThrow();
        assertThat(t.getStatus()).isEqualTo(TicketStatus.PENDING);
        mvc.perform(
                        action(t, "assign", "fixture_admin", "ADMIN")
                                .param("engineerId", f.engineer().getId().toString()))
                .andExpect(status().is3xxRedirection());
        assertThat(load(t).getStatus()).isEqualTo(TicketStatus.ASSIGNED);
        mvc.perform(action(t, "start", "fixture_engineer", "ENGINEER"))
                .andExpect(status().is3xxRedirection());
        assertThat(load(t).getStatus()).isEqualTo(TicketStatus.IN_PROGRESS);
        mvc.perform(resolve(t, "fixture_engineer")).andExpect(status().is3xxRedirection());
        assertThat(load(t).getStatus()).isEqualTo(TicketStatus.RESOLVED);
        assertThat(repairs.count()).isEqualTo(1);
        mvc.perform(action(t, "confirm", "fixture_employee", "EMPLOYEE"))
                .andExpect(status().is3xxRedirection());
        Ticket closed = load(t);
        assertThat(closed.getStatus()).isEqualTo(TicketStatus.CLOSED);
        assertThat(closed.getClosedAt()).isNotNull();
        for (String[] op :
                new String[][] {
                    {"assign", "fixture_admin", "ADMIN"},
                    {"start", "fixture_engineer", "ENGINEER"},
                    {"resolve", "fixture_engineer", "ENGINEER"},
                    {"confirm", "fixture_employee", "EMPLOYEE"},
                    {"reject", "fixture_employee", "EMPLOYEE"},
                    {"cancel", "fixture_employee", "EMPLOYEE"}
                }) {
            mvc.perform(
                            action(t, op[0], op[1], op[2])
                                    .param("engineerId", f.engineer().getId().toString())
                                    .param("cause", "原因")
                                    .param("solution", "方式")
                                    .param("result", "結果")
                                    .param("reason", "退回"))
                    .andExpect(status().isBadRequest());
        }
        assertThat(load(t).getVersion()).isEqualTo(closed.getVersion());
        assertThat(repairs.count()).isEqualTo(1);
    }

    @Test
    void rejectionReassignmentAndRepeatedOldSubmissionPreserveHistory() throws Exception {
        Ticket t = fixtures.ticket(f, f.employee(), f.engineer(), TicketStatus.IN_PROGRESS);
        Long original = load(t).getVersion();
        mvc.perform(resolve(t, "fixture_engineer")).andExpect(status().is3xxRedirection());
        mvc.perform(
                        post("/tickets/" + t.getId() + "/resolve")
                                .with(user("fixture_engineer").roles("ENGINEER"))
                                .with(csrf())
                                .param("version", original.toString())
                                .param("cause", "原因")
                                .param("solution", "方式")
                                .param("result", "結果"))
                .andExpect(status().isConflict());
        mvc.perform(
                        action(t, "reject", "fixture_employee", "EMPLOYEE")
                                .param("reason", "  仍然無法開機  "))
                .andExpect(status().is3xxRedirection());
        assertThat(load(t).getStatus()).isEqualTo(TicketStatus.IN_PROGRESS);
        assertThat(load(t).getReturnReason()).isEqualTo("仍然無法開機");
        mvc.perform(
                        post("/tickets/" + t.getId() + "/resolve")
                                .with(user("fixture_engineer").roles("ENGINEER"))
                                .with(csrf())
                                .param("version", original.toString())
                                .param("cause", "舊表單")
                                .param("solution", "方式")
                                .param("result", "結果"))
                .andExpect(status().isConflict());
        mvc.perform(
                        action(t, "assign", "fixture_admin", "ADMIN")
                                .param("engineerId", f.otherEngineer().getId().toString()))
                .andExpect(status().is3xxRedirection());
        assertThat(load(t).getStatus()).isEqualTo(TicketStatus.ASSIGNED);
        assertThat(repairs.count()).isEqualTo(1);
        mvc.perform(action(t, "start", "fixture_engineer", "ENGINEER"))
                .andExpect(status().isForbidden());
        mvc.perform(action(t, "start", "fixture_other_engineer", "ENGINEER"))
                .andExpect(status().is3xxRedirection());
        mvc.perform(resolve(t, "fixture_other_engineer")).andExpect(status().is3xxRedirection());
        assertThat(repairs.count()).isEqualTo(2);
        mvc.perform(action(t, "confirm", "fixture_employee", "EMPLOYEE"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void onlyPendingOwnerMayCancel() throws Exception {
        Ticket pending = fixtures.ticket(f, f.employee(), null, TicketStatus.PENDING);
        mvc.perform(action(pending, "cancel", "fixture_other_employee", "EMPLOYEE"))
                .andExpect(status().isForbidden());
        mvc.perform(action(pending, "cancel", "fixture_employee", "EMPLOYEE"))
                .andExpect(status().is3xxRedirection());
        assertThat(load(pending).getStatus()).isEqualTo(TicketStatus.CANCELLED);
        mvc.perform(action(pending, "cancel", "fixture_employee", "EMPLOYEE"))
                .andExpect(status().isBadRequest());
        Ticket assigned = fixtures.ticket(f, f.employee(), f.engineer(), TicketStatus.ASSIGNED);
        mvc.perform(action(assigned, "cancel", "fixture_employee", "EMPLOYEE"))
                .andExpect(status().isBadRequest());
        assertThat(load(assigned).getStatus()).isEqualTo(TicketStatus.ASSIGNED);
    }

    @Test
    void invalidAssigneeRolesOwnershipAndCsrfDoNotChangeData() throws Exception {
        Ticket t = fixtures.ticket(f, f.employee(), f.engineer(), TicketStatus.IN_PROGRESS);
        mvc.perform(resolve(t, "fixture_other_engineer")).andExpect(status().isForbidden());
        mvc.perform(
                        action(t, "assign", "fixture_employee", "EMPLOYEE")
                                .param("engineerId", f.engineer().getId().toString()))
                .andExpect(status().isForbidden());
        mvc.perform(
                        action(t, "resolve", "fixture_admin", "ADMIN")
                                .param("cause", "原因")
                                .param("solution", "方式")
                                .param("result", "結果"))
                .andExpect(status().isForbidden());
        mvc.perform(
                        action(t, "assign", "fixture_admin", "ADMIN")
                                .param("engineerId", f.employee().getId().toString()))
                .andExpect(status().isBadRequest());
        User inactive = users.findById(f.otherEngineer().getId()).orElseThrow();
        inactive.setEnabled(false);
        users.saveAndFlush(inactive);
        mvc.perform(
                        action(t, "assign", "fixture_admin", "ADMIN")
                                .param("engineerId", inactive.getId().toString()))
                .andExpect(status().isBadRequest());
        mvc.perform(
                        post("/tickets/" + t.getId() + "/resolve")
                                .with(user("fixture_engineer").roles("ENGINEER"))
                                .param("version", "0")
                                .param("cause", "原因")
                                .param("solution", "方式")
                                .param("result", "結果"))
                .andExpect(status().isForbidden());
        assertThat(load(t).getStatus()).isEqualTo(TicketStatus.IN_PROGRESS);
        assertThat(repairs.count()).isZero();
    }

    @Test
    void missingFieldsAndIllegalStateTransitionsAreRejected() throws Exception {
        Ticket t = fixtures.ticket(f, f.employee(), f.engineer(), TicketStatus.IN_PROGRESS);
        mvc.perform(
                        action(t, "resolve", "fixture_engineer", "ENGINEER")
                                .param("cause", " ")
                                .param("solution", " ")
                                .param("result", " "))
                .andExpect(status().isBadRequest());
        mvc.perform(
                        post("/tickets/" + t.getId() + "/resolve")
                                .with(user("fixture_engineer").roles("ENGINEER"))
                                .with(csrf())
                                .param("cause", "原因")
                                .param("solution", "方式")
                                .param("result", "結果"))
                .andExpect(status().isBadRequest());
        mvc.perform(action(t, "start", "fixture_engineer", "ENGINEER"))
                .andExpect(status().isBadRequest());
        mvc.perform(action(t, "confirm", "fixture_employee", "EMPLOYEE"))
                .andExpect(status().isBadRequest());
        mvc.perform(action(t, "reject", "fixture_employee", "EMPLOYEE").param("reason", "退回"))
                .andExpect(status().isBadRequest());
        assertThat(repairs.count()).isZero();
    }

    @Test
    void repairInsertFailureRollsBackAlreadyFlushedStatusAndVersion() throws Exception {
        Ticket t = fixtures.ticket(f, f.employee(), f.engineer(), TicketStatus.IN_PROGRESS);
        Long version = load(t).getVersion();
        jdbc.execute(
                "CREATE TRIGGER fail_repair_test BEFORE INSERT ON repair_records BEGIN SELECT RAISE(ABORT, 'integration rollback'); END");
        mvc.perform(resolve(t, "fixture_engineer")).andExpect(status().isConflict());
        Ticket unchanged = load(t);
        assertThat(unchanged.getStatus()).isEqualTo(TicketStatus.IN_PROGRESS);
        assertThat(unchanged.getVersion()).isEqualTo(version);
        assertThat(repairs.count()).isZero();
    }

    @Test
    void resolvedCannotBeReassignedAndOnlyRequesterCanConfirm() throws Exception {
        Ticket t = fixtures.ticket(f, f.employee(), f.engineer(), TicketStatus.RESOLVED);
        mvc.perform(
                        action(t, "assign", "fixture_admin", "ADMIN")
                                .param("engineerId", f.otherEngineer().getId().toString()))
                .andExpect(status().isBadRequest());
        mvc.perform(action(t, "confirm", "fixture_other_employee", "EMPLOYEE"))
                .andExpect(status().isForbidden());
        mvc.perform(action(t, "confirm", "fixture_admin", "ADMIN"))
                .andExpect(status().isForbidden());
        mvc.perform(action(t, "reject", "fixture_employee", "EMPLOYEE").param("reason", " "))
                .andExpect(status().isBadRequest());
        assertThat(load(t).getStatus()).isEqualTo(TicketStatus.RESOLVED);
    }

    @Test
    void hibernateOptimisticLockDetectsDetachedStaleWrite() {
        Ticket t = fixtures.ticket(f, f.employee(), null, TicketStatus.PENDING);
        Ticket first = load(t);
        Ticket stale = load(t);
        first.setTitle("第一筆修改");
        tickets.saveAndFlush(first);
        stale.setTitle("過期修改");
        assertThatThrownBy(() -> tickets.saveAndFlush(stale))
                .isInstanceOf(org.springframework.dao.OptimisticLockingFailureException.class);
        assertThat(load(t).getTitle()).isEqualTo("第一筆修改");
    }
}
