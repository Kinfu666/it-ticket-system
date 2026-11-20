package com.helpdesk;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.helpdesk.dto.TicketView;
import com.helpdesk.entity.RepairRecord;
import com.helpdesk.entity.Ticket;
import com.helpdesk.enums.TicketStatus;
import com.helpdesk.repository.CategoryRepository;
import com.helpdesk.repository.RepairRecordRepository;
import com.helpdesk.repository.TicketRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TicketQueryTest extends SqliteTestSupport {
    private final MockMvc mvc;
    private final TestFixtures fixtures;
    private final TicketRepository tickets;
    private final CategoryRepository categories;
    private final RepairRecordRepository repairs;
    private TestFixtures.Fixture f;

    @Autowired
    TicketQueryTest(
            MockMvc mvc,
            TestFixtures fixtures,
            TicketRepository tickets,
            CategoryRepository categories,
            RepairRecordRepository repairs) {
        this.mvc = mvc;
        this.fixtures = fixtures;
        this.tickets = tickets;
        this.categories = categories;
        this.repairs = repairs;
    }

    @BeforeEach
    void setup() {
        f = fixtures.create();
    }

    @Test
    void createIgnoresForgedRequesterAndStatusAndEscapesHtml() throws Exception {
        mvc.perform(
                        post("/tickets")
                                .with(user("fixture_employee").roles("EMPLOYEE"))
                                .with(csrf())
                                .param("title", " <script>alert(1)</script> ")
                                .param("description", "故障")
                                .param("location", " 二樓 ")
                                .param("priority", "HIGH")
                                .param("categoryId", f.category().getId().toString())
                                .param("requesterId", f.otherEmployee().getId().toString())
                                .param("status", "CLOSED"))
                .andExpect(status().is3xxRedirection());
        Ticket t = tickets.findAll().getFirst();
        assertThat(t.getRequester().getId()).isEqualTo(f.employee().getId());
        assertThat(t.getStatus()).isEqualTo(TicketStatus.PENDING);
        assertThat(t.getLocation()).isEqualTo("二樓");
        assertThat(t.getTicketNo()).startsWith("HD-").hasSize(39);
        mvc.perform(get("/tickets/" + t.getId()).with(user("fixture_employee").roles("EMPLOYEE")))
                .andExpect(status().isOk())
                .andExpect(
                        content().string(org.hamcrest.Matchers.containsString("&lt;script&gt;")));
    }

    @Test
    void roleScopesCannotBeBypassedThroughUrlOrEngineerParameter() throws Exception {
        Ticket mine = fixtures.ticket(f, f.employee(), f.engineer(), TicketStatus.ASSIGNED);
        Ticket other =
                fixtures.ticket(f, f.otherEmployee(), f.otherEngineer(), TicketStatus.ASSIGNED);
        for (String[] identity :
                new String[][] {
                    {"fixture_employee", "EMPLOYEE"}, {"fixture_engineer", "ENGINEER"}
                }) {
            mvc.perform(get("/tickets/" + mine.getId()).with(user(identity[0]).roles(identity[1])))
                    .andExpect(status().isOk());
            mvc.perform(get("/tickets/" + other.getId()).with(user(identity[0]).roles(identity[1])))
                    .andExpect(status().isForbidden());
            var result =
                    mvc.perform(get("/tickets").with(user(identity[0]).roles(identity[1])))
                            .andExpect(status().isOk())
                            .andReturn();
            Page<?> page = (Page<?>) result.getModelAndView().getModel().get("items");
            assertThat(page.getTotalElements()).isEqualTo(1);
        }
        mvc.perform(
                        get("/tickets")
                                .param("engineerId", f.otherEngineer().getId().toString())
                                .with(user("fixture_employee").roles("EMPLOYEE")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/tickets/999999").with(user("fixture_admin").roles("ADMIN")))
                .andExpect(status().isNotFound());
    }

    @Test
    void keywordCategoryDateRangeAndStablePagination() throws Exception {
        for (int i = 0; i < 12; i++) {
            Ticket t = fixtures.ticket(f, f.employee(), null, TicketStatus.PENDING);
            t.setTitle("日期篩選 " + i);
            t.setCreatedAt(Instant.parse("2026-01-01T16:00:00Z"));
            tickets.saveAndFlush(t);
        }
        Ticket before = fixtures.ticket(f, f.employee(), null, TicketStatus.PENDING);
        before.setCreatedAt(Instant.parse("2026-01-01T15:59:59.999999999Z"));
        tickets.saveAndFlush(before);
        Ticket after = fixtures.ticket(f, f.employee(), null, TicketStatus.PENDING);
        after.setCreatedAt(Instant.parse("2026-01-02T16:00:00Z"));
        tickets.saveAndFlush(after);
        var result =
                mvc.perform(
                                get("/tickets")
                                        .with(user("fixture_employee").roles("EMPLOYEE"))
                                        .param("keyword", "日期篩選")
                                        .param("status", "PENDING")
                                        .param("categoryId", f.category().getId().toString())
                                        .param("from", "2026-01-02")
                                        .param("to", "2026-01-02"))
                        .andExpect(status().isOk())
                        .andReturn();
        @SuppressWarnings("unchecked")
        Page<TicketView> page = (Page<TicketView>) result.getModelAndView().getModel().get("items");
        assertThat(page.getTotalElements()).isEqualTo(12);
        assertThat(page.getContent()).hasSize(10);
        assertThat(page.getContent().stream().map(TicketView::id).toList())
                .isSortedAccordingTo(java.util.Comparator.reverseOrder());
        var next =
                mvc.perform(
                                get("/tickets")
                                        .with(user("fixture_employee").roles("EMPLOYEE"))
                                        .param("keyword", "日期篩選")
                                        .param("page", "1"))
                        .andExpect(status().isOk())
                        .andReturn();
        assertThat(((Page<?>) next.getModelAndView().getModel().get("items")).getContent())
                .hasSize(2);
        var boundary =
                mvc.perform(
                                get("/tickets")
                                        .with(user("fixture_employee").roles("EMPLOYEE"))
                                        .param("from", "2026-01-02")
                                        .param("to", "2026-01-02"))
                        .andExpect(status().isOk())
                        .andReturn();
        assertThat(
                        ((Page<?>) boundary.getModelAndView().getModel().get("items"))
                                .getTotalElements())
                .isEqualTo(12);
    }

    @Test
    void invalidFormsAndQueriesDoNotWrite() throws Exception {
        mvc.perform(get("/tickets/new").with(user("fixture_employee").roles("EMPLOYEE")))
                .andExpect(status().isOk());
        mvc.perform(
                        post("/tickets")
                                .with(user("fixture_employee").roles("EMPLOYEE"))
                                .with(csrf())
                                .param("title", " ")
                                .param("description", " ")
                                .param("priority", "BAD"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasErrors("form"));
        f.category().setEnabled(false);
        categories.flush();
        mvc.perform(
                        post("/tickets")
                                .with(user("fixture_employee").roles("EMPLOYEE"))
                                .with(csrf())
                                .param("title", "標題")
                                .param("description", "故障")
                                .param("location", "位置")
                                .param("priority", "LOW")
                                .param("categoryId", f.category().getId().toString()))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasErrors("form"));
        for (String[] invalid :
                new String[][] {
                    {"size", "0"}, {"page", "-1"}, {"status", "BAD"}, {"from", "not-date"}
                }) {
            mvc.perform(
                            get("/tickets")
                                    .with(user("fixture_employee").roles("EMPLOYEE"))
                                    .param(invalid[0], invalid[1]))
                    .andExpect(status().isOk())
                    .andExpect(model().attributeHasErrors("query"));
        }
        assertThat(tickets.count()).isZero();
    }

    @Test
    void pastEngineerOnlyHasOwnReadOnlyRepairHistory() throws Exception {
        Ticket t = fixtures.ticket(f, f.employee(), f.otherEngineer(), TicketStatus.IN_PROGRESS);
        RepairRecord r = new RepairRecord();
        r.setTicket(t);
        r.setEngineer(f.engineer());
        r.setCause("故障原因");
        r.setSolution("處理方式");
        r.setResult("維修結果");
        repairs.saveAndFlush(r);
        mvc.perform(get("/tickets/" + t.getId()).with(user("fixture_engineer").roles("ENGINEER")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/tickets/repairs").with(user("fixture_engineer").roles("ENGINEER")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("維修結果")));
        mvc.perform(get("/tickets/repairs").with(user("fixture_employee").roles("EMPLOYEE")))
                .andExpect(status().isForbidden());
    }
}
