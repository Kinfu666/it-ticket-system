package com.helpdesk;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.helpdesk.dto.DashboardData;
import com.helpdesk.enums.TicketStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DashboardTest extends SqliteTestSupport {
    private final MockMvc mvc;
    private final TestFixtures fixtures;
    private TestFixtures.Fixture f;

    @Autowired
    DashboardTest(MockMvc mvc, TestFixtures fixtures) {
        this.mvc = mvc;
        this.fixtures = fixtures;
    }

    @BeforeEach
    void setup() {
        f = fixtures.create();
    }

    @Test
    void realDatabaseStatisticsRespectAllRoleScopesAndMenus() throws Exception {
        fixtures.ticket(f, f.employee(), null, TicketStatus.PENDING);
        fixtures.ticket(f, f.employee(), f.engineer(), TicketStatus.RESOLVED);
        fixtures.ticket(f, f.otherEmployee(), f.otherEngineer(), TicketStatus.CLOSED);
        for (String[] identity :
                new String[][] {
                    {"fixture_admin", "ADMIN", "3"},
                    {"fixture_employee", "EMPLOYEE", "2"},
                    {"fixture_engineer", "ENGINEER", "1"}
                }) {
            var result =
                    mvc.perform(get("/dashboard").with(user(identity[0]).roles(identity[1])))
                            .andExpect(status().isOk())
                            .andReturn();
            DashboardData data = (DashboardData) result.getModelAndView().getModel().get("data");
            assertThat(data.total()).isEqualTo(Long.parseLong(identity[2]));
            assertThat(data.awaitingConfirmation()).isEqualTo(1);
            assertThat(data.statuses().stream().mapToLong(s -> s.total()).sum())
                    .isEqualTo(data.total());
            String html =
                    result.getResponse()
                            .getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
            assertThat(html)
                    .contains("/css/app.css", "/vendor/bootstrap/bootstrap.min.css", "/logout");
            if (identity[1].equals("ADMIN")) {
                assertThat(data.categories()).hasSize(1);
                assertThat(data.categories().getFirst().total()).isEqualTo(3);
                assertThat(html).contains("/admin/users", "/admin/reports");
                assertThat(data.active()).isEqualTo(2);
            } else {
                assertThat(data.categories()).isEmpty();
                assertThat(html).doesNotContain("href=\"/admin/");
            }
        }
    }

    @Test
    void reportsAndEmptyDataRenderAndOnlyAdminCanRead() throws Exception {
        mvc.perform(get("/admin/reports").with(user("fixture_admin").roles("ADMIN")))
                .andExpect(status().isOk());
        mvc.perform(get("/admin/reports").with(user("fixture_employee").roles("EMPLOYEE")))
                .andExpect(status().isForbidden());
        var result =
                mvc.perform(get("/dashboard").with(user("fixture_employee").roles("EMPLOYEE")))
                        .andExpect(status().isOk())
                        .andReturn();
        assertThat(((DashboardData) result.getModelAndView().getModel().get("data")).total())
                .isZero();
    }

    @Test
    void loginAndResourcesAreLocalAndAccessibleWithoutLogin() throws Exception {
        String html =
                mvc.perform(get("/login"))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        assertThat(html).doesNotMatch("(?s).*(?:src|href)=\"https?://.*");
        for (String path :
                new String[] {
                    "/css/app.css",
                    "/vendor/bootstrap/bootstrap.min.css",
                    "/vendor/bootstrap/bootstrap.bundle.min.js"
                }) {
            mvc.perform(get(path)).andExpect(status().isOk());
        }
    }
}
