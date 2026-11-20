package com.helpdesk;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.helpdesk.entity.Department;
import com.helpdesk.entity.User;
import com.helpdesk.enums.Role;
import com.helpdesk.repository.DepartmentRepository;
import com.helpdesk.repository.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SecurityTest extends SqliteTestSupport {
    private final MockMvc mvc;
    private final UserRepository users;
    private final DepartmentRepository departments;
    private final PasswordEncoder encoder;

    @Autowired
    SecurityTest(
            MockMvc mvc,
            UserRepository users,
            DepartmentRepository departments,
            PasswordEncoder encoder) {
        this.mvc = mvc;
        this.users = users;
        this.departments = departments;
        this.encoder = encoder;
    }

    @BeforeEach
    void fixture() {
        Department department = new Department();
        department.setName("安全測試部門");
        departments.save(department);
        for (Role role : Role.values()) {
            User user = new User();
            user.setUsername(role.name().toLowerCase());
            user.setFullName(role.getLabel());
            user.setRole(role);
            user.setPassword(encoder.encode("Test12345!"));
            user.setDepartment(department);
            users.save(user);
        }
        users.flush();
    }

    @Test
    void allRolesLoginAndDashboard() throws Exception {
        for (Role role : Role.values()) {
            var session =
                    (MockHttpSession)
                            mvc.perform(
                                            formLogin()
                                                    .user(role.name().toLowerCase())
                                                    .password("Test12345!"))
                                    .andExpect(authenticated().withRoles(role.name()))
                                    .andReturn()
                                    .getRequest()
                                    .getSession(false);
            mvc.perform(get("/dashboard").session(session)).andExpect(status().isOk());
        }
    }

    @Test
    void wrongPasswordDisabledAndAnonymousAreRejected() throws Exception {
        mvc.perform(formLogin().user("employee").password("wrong")).andExpect(unauthenticated());
        var employee = users.findByUsername("employee").orElseThrow();
        employee.setEnabled(false);
        users.flush();
        mvc.perform(formLogin().user("employee").password("Test12345!"))
                .andExpect(unauthenticated());
        mvc.perform(get("/tickets")).andExpect(status().is3xxRedirection());
    }

    @Test
    void roleAuthorizationAndCsrf() throws Exception {
        mvc.perform(get("/admin/users").with(user("employee").roles("EMPLOYEE")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/tickets/new").with(user("engineer").roles("ENGINEER")))
                .andExpect(status().isForbidden());
        mvc.perform(post("/profile/password").with(user("employee").roles("EMPLOYEE")))
                .andExpect(status().isForbidden());
    }

    @Test
    void logoutInvalidatesSession() throws Exception {
        var session =
                (MockHttpSession)
                        mvc.perform(formLogin().user("employee").password("Test12345!"))
                                .andReturn()
                                .getRequest()
                                .getSession(false);
        mvc.perform(post("/logout").session(session).with(csrf()))
                .andExpect(redirectedUrl("/login?logout"))
                .andExpect(unauthenticated());
        assertThat(session.isInvalid()).isTrue();
        mvc.perform(get("/dashboard")).andExpect(status().is3xxRedirection());
    }

    @Test
    void disabledOrChangedRoleRevokesExistingSession() throws Exception {
        var session =
                (MockHttpSession)
                        mvc.perform(formLogin().user("admin").password("Test12345!"))
                                .andReturn()
                                .getRequest()
                                .getSession(false);
        var admin = users.findByUsername("admin").orElseThrow();
        admin.setRole(Role.EMPLOYEE);
        users.flush();
        mvc.perform(get("/dashboard").session(session)).andExpect(redirectedUrl("/login?changed"));
        assertThat(session.isInvalid()).isTrue();
        var employeeSession =
                (MockHttpSession)
                        mvc.perform(formLogin().user("employee").password("Test12345!"))
                                .andReturn()
                                .getRequest()
                                .getSession(false);
        var employee = users.findByUsername("employee").orElseThrow();
        employee.setEnabled(false);
        users.flush();
        mvc.perform(
                        post("/profile/password")
                                .session(employeeSession)
                                .with(csrf())
                                .param("oldPassword", "Test12345!")
                                .param("newPassword", "Other12345!"))
                .andExpect(redirectedUrl("/login?changed"));
        assertThat(encoder.matches("Test12345!", employee.getPassword())).isTrue();
    }

    @Test
    void passwordRequiresOldPasswordAndPreservesSpaces() throws Exception {
        mvc.perform(
                        post("/profile/password")
                                .with(user("employee").roles("EMPLOYEE"))
                                .with(csrf())
                                .param("oldPassword", "wrong")
                                .param("newPassword", "New12345!"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasErrors("form"));
        mvc.perform(
                        post("/profile/password")
                                .with(user("employee").roles("EMPLOYEE"))
                                .with(csrf())
                                .param("oldPassword", "Test12345!")
                                .param("newPassword", "  abcdefgh  "))
                .andExpect(redirectedUrl("/login?passwordChanged"));
        assertThat(
                        encoder.matches(
                                "  abcdefgh  ",
                                users.findByUsername("employee").orElseThrow().getPassword()))
                .isTrue();
    }

    @Test
    void unicodePasswordCannotExceedBcryptByteLimit() throws Exception {
        mvc.perform(
                        post("/profile/password")
                                .with(user("employee").roles("EMPLOYEE"))
                                .with(csrf())
                                .param("oldPassword", "Test12345!")
                                .param("newPassword", "密".repeat(25)))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasErrors("form"));
        assertThat(
                        encoder.matches(
                                "Test12345!",
                                users.findByUsername("employee").orElseThrow().getPassword()))
                .isTrue();
    }
}
