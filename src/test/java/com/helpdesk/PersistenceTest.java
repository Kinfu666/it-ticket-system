package com.helpdesk;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.time.Instant;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import com.helpdesk.config.InstantTextConverter;
import com.helpdesk.entity.Category;
import com.helpdesk.entity.Department;
import com.helpdesk.entity.RepairRecord;
import com.helpdesk.entity.Ticket;
import com.helpdesk.entity.User;
import com.helpdesk.enums.Priority;
import com.helpdesk.enums.Role;
import com.helpdesk.repository.CategoryRepository;
import com.helpdesk.repository.DepartmentRepository;
import com.helpdesk.repository.RepairRecordRepository;
import com.helpdesk.repository.TicketRepository;
import com.helpdesk.repository.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class PersistenceTest extends SqliteTestSupport {
    private final DepartmentRepository departments;
    private final UserRepository users;
    private final CategoryRepository categories;
    private final TicketRepository tickets;
    private final RepairRecordRepository repairs;
    private final JdbcTemplate jdbc;
    private final EntityManager em;

    @Autowired
    PersistenceTest(
            DepartmentRepository departments,
            UserRepository users,
            CategoryRepository categories,
            TicketRepository tickets,
            RepairRecordRepository repairs,
            JdbcTemplate jdbc,
            EntityManager em) {
        this.departments = departments;
        this.users = users;
        this.categories = categories;
        this.tickets = tickets;
        this.repairs = repairs;
        this.jdbc = jdbc;
        this.em = em;
    }

    @Test
    void fiveTablesCrudAndTimeMapping() {
        Department d = new Department();
        d.setName("測試部門");
        departments.saveAndFlush(d);
        User u = new User();
        u.setUsername("test-user");
        u.setPassword("hash");
        u.setFullName("測試員工");
        u.setRole(Role.EMPLOYEE);
        u.setDepartment(d);
        users.saveAndFlush(u);
        Category c = new Category();
        c.setName("測試分類");
        categories.saveAndFlush(c);
        Ticket t = new Ticket();
        t.setTicketNo("HD-test");
        t.setTitle("故障");
        t.setDescription("描述");
        t.setLocation("一樓");
        t.setCategory(c);
        t.setRequester(u);
        t.setPriority(Priority.MEDIUM);
        t.setCreatedAt(Instant.parse("2026-01-01T00:00:00.1Z"));
        tickets.saveAndFlush(t);
        RepairRecord r = new RepairRecord();
        r.setTicket(t);
        r.setEngineer(u);
        r.setCause("原因");
        r.setSolution("處理");
        r.setResult("結果");
        repairs.saveAndFlush(r);
        em.clear();
        assertThat(tickets.findById(t.getId()).orElseThrow().getCreatedAt())
                .isEqualTo(t.getCreatedAt());
        assertThat(
                        jdbc.queryForObject(
                                "select created_at from tickets where id=?",
                                String.class,
                                t.getId()))
                .isEqualTo("2026-01-01T00:00:00.100000000Z");
        assertThat(repairs.findByTicketIdOrderByCreatedAtAscIdAsc(t.getId())).hasSize(1);
        c = categories.findById(c.getId()).orElseThrow();
        c.setName("修改分類");
        categories.saveAndFlush(c);
        repairs.deleteById(r.getId());
        repairs.flush();
        tickets.deleteById(t.getId());
        tickets.flush();
        users.deleteById(u.getId());
        users.flush();
        categories.delete(c);
        departments.deleteById(d.getId());
        em.flush();
        assertThat(tickets.findById(t.getId())).isEmpty();
    }

    @Test
    void constraintsAndEveryBorrowedConnectionEnableForeignKeys() {
        assertThat(jdbc.queryForObject("PRAGMA foreign_keys", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("PRAGMA busy_timeout", Integer.class)).isEqualTo(5000);
        jdbc.update("insert into departments(name) values ('受引用部門')");
        Long id = jdbc.queryForObject("select id from departments where name='受引用部門'", Long.class);
        assertThatThrownBy(() -> jdbc.update("insert into departments(name) values ('受引用部門')"))
                .isInstanceOf(Exception.class);
        assertThatThrownBy(
                        () -> jdbc.update("insert into categories(name,enabled) values ('非法',2)"))
                .isInstanceOf(Exception.class);
        assertThatThrownBy(
                        () ->
                                jdbc.update(
                                        "insert into users(username,password,full_name,role,department_id,created_at) values ('bad','x','x','ADMIN',999999,'x')"))
                .isInstanceOf(Exception.class);
        jdbc.update(
                "insert into users(username,password,full_name,role,department_id,created_at) values ('valid','x','x','ADMIN',?,'x')",
                id);
        assertThatThrownBy(() -> jdbc.update("delete from departments where id=?", id))
                .isInstanceOf(Exception.class);
    }

    @Test
    void utcTextSortAndRangeAreChronological() {
        InstantTextConverter converter = new InstantTextConverter();
        String a = converter.convertToDatabaseColumn(Instant.parse("2026-01-01T00:00:00Z"));
        String b = converter.convertToDatabaseColumn(Instant.parse("2026-01-01T00:00:00.1Z"));
        assertThat(a).isLessThan(b);
        assertThat(converter.convertToEntityAttribute(b))
                .isEqualTo(Instant.parse("2026-01-01T00:00:00.1Z"));
    }

    @Test
    void sqliteFileSurvivesClosingAndReopeningConnection() throws Exception {
        var path = Files.createTempFile(Path.of("target/test-databases"), "restart-", ".db");
        try (var connection = DriverManager.getConnection("jdbc:sqlite:" + path);
                var statement = connection.createStatement()) {
            statement.execute(
                    "create table persisted(id integer primary key, value text not null)");
            statement.executeUpdate("insert into persisted(value) values ('重新開啟仍存在')");
        }
        try (var connection = DriverManager.getConnection("jdbc:sqlite:" + path);
                var statement = connection.createStatement();
                var result = statement.executeQuery("select value from persisted")) {
            assertThat(result.next()).isTrue();
            assertThat(result.getString(1)).isEqualTo("重新開啟仍存在");
        }
    }
}
