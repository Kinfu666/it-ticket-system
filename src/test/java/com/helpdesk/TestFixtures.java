package com.helpdesk;

import org.springframework.boot.test.context.TestComponent;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import com.helpdesk.entity.Category;
import com.helpdesk.entity.Department;
import com.helpdesk.entity.Ticket;
import com.helpdesk.entity.User;
import com.helpdesk.enums.Priority;
import com.helpdesk.enums.Role;
import com.helpdesk.enums.TicketStatus;
import com.helpdesk.repository.CategoryRepository;
import com.helpdesk.repository.DepartmentRepository;
import com.helpdesk.repository.TicketRepository;
import com.helpdesk.repository.UserRepository;

@TestComponent
public class TestFixtures {
    private final DepartmentRepository departments;
    private final CategoryRepository categories;
    private final UserRepository users;
    private final TicketRepository tickets;
    private final PasswordEncoder encoder;

    public TestFixtures(
            DepartmentRepository departments,
            CategoryRepository categories,
            UserRepository users,
            TicketRepository tickets,
            PasswordEncoder encoder) {
        this.departments = departments;
        this.categories = categories;
        this.users = users;
        this.tickets = tickets;
        this.encoder = encoder;
    }

    public record Fixture(
            Department department,
            Category category,
            User admin,
            User employee,
            User otherEmployee,
            User engineer,
            User otherEngineer) {}

    private User user(String name, Role role, Department department) {
        User u = new User();
        u.setUsername(name);
        u.setFullName(name);
        u.setRole(role);
        u.setDepartment(department);
        u.setPassword(encoder.encode("Test12345!"));
        return users.save(u);
    }

    @Transactional
    public Fixture create() {
        Department d = new Department();
        d.setName("整合測試部門");
        departments.save(d);
        Category c = new Category();
        c.setName("整合測試分類");
        categories.save(c);
        return new Fixture(
                d,
                c,
                user("fixture_admin", Role.ADMIN, d),
                user("fixture_employee", Role.EMPLOYEE, d),
                user("fixture_other_employee", Role.EMPLOYEE, d),
                user("fixture_engineer", Role.ENGINEER, d),
                user("fixture_other_engineer", Role.ENGINEER, d));
    }

    @Transactional
    public Ticket ticket(Fixture f, User requester, User engineer, TicketStatus status) {
        Ticket t = new Ticket();
        t.setTicketNo("HD-" + java.util.UUID.randomUUID());
        t.setTitle("測試電腦故障");
        t.setDescription("無法開機");
        t.setLocation("一樓");
        t.setPriority(Priority.MEDIUM);
        t.setCategory(f.category());
        t.setRequester(requester);
        t.setEngineer(engineer);
        t.setStatus(status);
        return tickets.saveAndFlush(t);
    }
}
