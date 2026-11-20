package com.helpdesk;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.helpdesk.repository.CategoryRepository;
import com.helpdesk.repository.DepartmentRepository;
import com.helpdesk.repository.RepairRecordRepository;
import com.helpdesk.repository.TicketRepository;
import com.helpdesk.repository.UserRepository;
import com.helpdesk.service.DemoService;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles({"test", "demo"})
class DemoInitializationTest {
    private static final Path DATABASE = createDatabase();

    private final DemoService demo;
    private final UserRepository users;
    private final DepartmentRepository departments;
    private final CategoryRepository categories;
    private final TicketRepository tickets;
    private final RepairRecordRepository repairs;
    private final PasswordEncoder encoder;

    private static Path createDatabase() {
        try {
            Files.createDirectories(Path.of("target/test-databases"));
            return Files.createTempFile(Path.of("target/test-databases"), "demo-", ".db")
                    .toAbsolutePath();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("helpdesk.database", DATABASE::toString);
    }

    @Autowired
    DemoInitializationTest(
            DemoService demo,
            UserRepository users,
            DepartmentRepository departments,
            CategoryRepository categories,
            TicketRepository tickets,
            RepairRecordRepository repairs,
            PasswordEncoder encoder) {
        this.demo = demo;
        this.users = users;
        this.departments = departments;
        this.categories = categories;
        this.tickets = tickets;
        this.repairs = repairs;
        this.encoder = encoder;
    }

    @Test
    void explicitDemoProfileSeedsAllStatesAndNeverDuplicatesOnRestart() {
        assertThat(users.count()).isEqualTo(5);
        assertThat(departments.count()).isEqualTo(4);
        assertThat(categories.count()).isEqualTo(5);
        assertThat(tickets.count()).isEqualTo(12);
        long repairCount = repairs.count();
        assertThat(repairCount).isEqualTo(5);
        assertThat(tickets.findAll().stream().map(t -> t.getStatus()).distinct().count())
                .isEqualTo(6);
        for (String username : new String[] {"admin", "engineer", "employee"}) {
            assertThat(
                            encoder.matches(
                                    "Demo1234!",
                                    users.findByUsername(username).orElseThrow().getPassword()))
                    .isTrue();
        }
        var renamed = users.findByUsername("employee").orElseThrow();
        renamed.setUsername("renamed_employee");
        users.saveAndFlush(renamed);
        demo.initialize();
        assertThat(users.count()).isEqualTo(5);
        assertThat(tickets.count()).isEqualTo(12);
        assertThat(repairs.count()).isEqualTo(repairCount);
        assertThat(users.findByUsername("renamed_employee")).isPresent();
        assertThat(users.findByUsername("employee")).isEmpty();
    }
}
