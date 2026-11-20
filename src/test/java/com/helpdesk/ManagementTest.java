package com.helpdesk;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.helpdesk.entity.User;
import com.helpdesk.enums.Role;
import com.helpdesk.enums.TicketStatus;
import com.helpdesk.repository.CategoryRepository;
import com.helpdesk.repository.DepartmentRepository;
import com.helpdesk.repository.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ManagementTest extends SqliteTestSupport {
    private final MockMvc mvc;
    private final TestFixtures fixtures;
    private final UserRepository users;
    private final DepartmentRepository departments;
    private final CategoryRepository categories;
    private TestFixtures.Fixture f;

    @Autowired
    ManagementTest(
            MockMvc mvc,
            TestFixtures fixtures,
            UserRepository users,
            DepartmentRepository departments,
            CategoryRepository categories) {
        this.mvc = mvc;
        this.fixtures = fixtures;
        this.users = users;
        this.departments = departments;
        this.categories = categories;
    }

    @BeforeEach
    void setup() {
        f = fixtures.create();
    }

    @Test
    void allManagementPagesAndFormsRender() throws Exception {
        for (String path :
                new String[] {
                    "/admin/users",
                    "/admin/users/new",
                    "/admin/users/" + f.employee().getId() + "/edit",
                    "/admin/departments",
                    "/admin/categories",
                    "/admin/departments/new",
                    "/admin/categories/new",
                    "/admin/departments/" + f.department().getId() + "/edit",
                    "/admin/categories/" + f.category().getId() + "/edit"
                }) {
            mvc.perform(get(path).with(user("fixture_admin").roles("ADMIN")))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void createUserHashesPasswordAndRejectsDuplicateOrInvalid() throws Exception {
        mvc.perform(
                        post("/admin/users/new")
                                .with(user("fixture_admin").roles("ADMIN"))
                                .with(csrf())
                                .param("username", "  new_user  ")
                                .param("fullName", "  王小明  ")
                                .param("password", "New12345!")
                                .param("role", "EMPLOYEE")
                                .param("departmentId", f.department().getId().toString())
                                .param("enabled", "true"))
                .andExpect(redirectedUrl("/admin/users"));
        User created = users.findByUsername("new_user").orElseThrow();
        assertThat(created.getFullName()).isEqualTo("王小明");
        assertThat(created.getPassword()).startsWith("$2").isNotEqualTo("New12345!");
        mvc.perform(
                        post("/admin/users/new")
                                .with(user("fixture_admin").roles("ADMIN"))
                                .with(csrf())
                                .param("username", "fixture_employee")
                                .param("fullName", "姓名")
                                .param("password", "New12345!")
                                .param("role", "EMPLOYEE")
                                .param("departmentId", f.department().getId().toString()))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasErrors("form"));
        mvc.perform(
                        post("/admin/users/new")
                                .with(user("fixture_admin").roles("ADMIN"))
                                .with(csrf())
                                .param("username", " ")
                                .param("fullName", " ")
                                .param("role", "INVALID"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasErrors("form"));
    }

    @Test
    void lastAdminAndBusyEngineerCannotBeDisabledOrChangeRole() throws Exception {
        mvc.perform(
                        post("/admin/users/" + f.admin().getId() + "/enabled")
                                .with(user("fixture_admin").roles("ADMIN"))
                                .with(csrf())
                                .param("enabled", "false"))
                .andExpect(status().isBadRequest());
        assertThat(f.admin().isEnabled()).isTrue();
        fixtures.ticket(f, f.employee(), f.engineer(), TicketStatus.ASSIGNED);
        mvc.perform(
                        post("/admin/users/" + f.engineer().getId() + "/enabled")
                                .with(user("fixture_admin").roles("ADMIN"))
                                .with(csrf())
                                .param("enabled", "false"))
                .andExpect(status().isBadRequest());
        mvc.perform(
                        post("/admin/users/" + f.engineer().getId() + "/edit")
                                .with(user("fixture_admin").roles("ADMIN"))
                                .with(csrf())
                                .param("username", f.engineer().getUsername())
                                .param("fullName", "工程師")
                                .param("role", "EMPLOYEE")
                                .param("departmentId", f.department().getId().toString())
                                .param("enabled", "true"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasErrors("form"));
        assertThat(f.engineer().getRole()).isEqualTo(Role.ENGINEER);
    }

    @Test
    void mastersCreateUpdateDisableAndDepartmentCannotReceiveNewUsers() throws Exception {
        mvc.perform(
                        post("/admin/categories/new")
                                .with(user("fixture_admin").roles("ADMIN"))
                                .with(csrf())
                                .param("name", " 印表機 ")
                                .param("description", " 說明 ")
                                .param("enabled", "true"))
                .andExpect(redirectedUrl("/admin/categories"));
        assertThat(categories.existsByName("印表機")).isTrue();
        mvc.perform(
                        post("/admin/categories/" + f.category().getId() + "/edit")
                                .with(user("fixture_admin").roles("ADMIN"))
                                .with(csrf())
                                .param("name", "修改分類")
                                .param("enabled", "true"))
                .andExpect(redirectedUrl("/admin/categories"));
        mvc.perform(
                        post("/admin/departments/" + f.department().getId() + "/enabled")
                                .with(user("fixture_admin").roles("ADMIN"))
                                .with(csrf())
                                .param("enabled", "false"))
                .andExpect(redirectedUrl("/admin/departments"));
        assertThat(departments.findById(f.department().getId()).orElseThrow().isEnabled())
                .isFalse();
        assertThat(users.findByUsername("fixture_employee")).isPresent();
        mvc.perform(
                        post("/admin/users/new")
                                .with(user("fixture_admin").roles("ADMIN"))
                                .with(csrf())
                                .param("username", "blocked_user")
                                .param("fullName", "姓名")
                                .param("password", "Test12345!")
                                .param("role", "EMPLOYEE")
                                .param("departmentId", f.department().getId().toString()))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasErrors("form"));
        assertThat(users.findByUsername("blocked_user")).isEmpty();
    }

    @Test
    void employeeCannotForgeAdminPost() throws Exception {
        long count = departments.count();
        mvc.perform(
                        post("/admin/departments/new")
                                .with(user("fixture_employee").roles("EMPLOYEE"))
                                .with(csrf())
                                .param("name", "越權部門")
                                .param("enabled", "true"))
                .andExpect(status().isForbidden());
        assertThat(departments.count()).isEqualTo(count);
    }
}
