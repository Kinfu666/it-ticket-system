package com.helpdesk.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.helpdesk.entity.Category;
import com.helpdesk.entity.Department;
import com.helpdesk.entity.RepairRecord;
import com.helpdesk.entity.Ticket;
import com.helpdesk.entity.User;
import com.helpdesk.enums.Priority;
import com.helpdesk.enums.Role;
import com.helpdesk.enums.TicketStatus;
import com.helpdesk.repository.CategoryRepository;
import com.helpdesk.repository.DepartmentRepository;
import com.helpdesk.repository.RepairRecordRepository;
import com.helpdesk.repository.TicketRepository;
import com.helpdesk.repository.UserRepository;

@Service
@Profile("demo")
public class DemoService {
    private final DepartmentRepository departments;
    private final CategoryRepository categories;
    private final UserRepository users;
    private final TicketRepository tickets;
    private final RepairRecordRepository repairs;
    private final PasswordEncoder encoder;

    public DemoService(
            DepartmentRepository departments,
            CategoryRepository categories,
            UserRepository users,
            TicketRepository tickets,
            RepairRecordRepository repairs,
            PasswordEncoder encoder) {
        this.departments = departments;
        this.categories = categories;
        this.users = users;
        this.tickets = tickets;
        this.repairs = repairs;
        this.encoder = encoder;
    }

    private User user(String username, String name, Role role, Department department) {
        User u = new User();
        u.setUsername(username);
        u.setFullName(name);
        u.setRole(role);
        u.setDepartment(department);
        u.setPassword(encoder.encode("Demo1234!"));
        return users.save(u);
    }

    @Transactional
    public void initialize() {
        // 交易內一次完成；既有展示資料不重建，也不將展示帳號混入其他既有資料庫。
        if (tickets.existsByTicketNo("HD-DEMO-001")) {
            return;
        }
        if (users.count() > 0
                || departments.count() > 0
                || categories.count() > 0
                || tickets.count() > 0) {
            throw new IllegalStateException("demo 初始化需要空資料庫；請指定獨立的 HELPDESK_DATABASE，系統不會清空既有資料");
        }
        List<Department> deps = new ArrayList<>();
        for (String name : List.of("資訊管理部", "行政管理部", "業務部", "財務部")) {
            Department d = new Department();
            d.setName(name);
            deps.add(departments.save(d));
        }
        List<Category> cats = new ArrayList<>();
        for (String name : List.of("電腦設備", "網路連線", "印表機", "系統軟體", "帳號存取")) {
            Category c = new Category();
            c.setName(name);
            c.setDescription(name + "相關故障與服務需求");
            cats.add(categories.save(c));
        }
        user("admin", "陳予安", Role.ADMIN, deps.get(0));
        User engineer = user("engineer", "林承宇", Role.ENGINEER, deps.get(0));
        User employee = user("employee", "王思涵", Role.EMPLOYEE, deps.get(2));
        User engineer2 = user("engineer2", "吳柏辰", Role.ENGINEER, deps.get(0));
        User employee2 = user("employee2", "李佳穎", Role.EMPLOYEE, deps.get(3));
        String[] titles = {
            "電腦開機後螢幕無畫面",
            "會議室無線網路連線不穩",
            "印表機持續出現卡紙提示",
            "辦公軟體開啟時出現錯誤",
            "無法登入內部文件系統",
            "筆記型電腦風扇聲音異常",
            "辦公區網路插座無法連線",
            "列印文件顏色不正確",
            "更新後軟體無法啟動",
            "共用資料夾存取被拒絕",
            "外接螢幕偶爾閃爍",
            "視訊會議連線中斷"
        };
        Instant now = Instant.now();
        for (int i = 0; i < titles.length; i++) {
            TicketStatus status = TicketStatus.values()[i % 6];
            Ticket t = new Ticket();
            t.setTicketNo("HD-DEMO-" + String.format("%03d", i + 1));
            t.setTitle(titles[i]);
            t.setDescription("虛構展示案件：" + titles[i] + "。已重新啟動設備，仍需資訊人員協助確認。");
            t.setLocation((i % 3 + 1) + " 樓辦公區");
            t.setCategory(cats.get(i % 5));
            t.setPriority(Priority.values()[i % 3]);
            t.setRequester(i % 4 == 0 ? employee2 : employee);
            t.setStatus(status);
            t.setCreatedAt(now.minus(12 - i, ChronoUnit.DAYS));
            t.setUpdatedAt(now.minus(12 - i, ChronoUnit.HOURS));
            if (status != TicketStatus.PENDING && status != TicketStatus.CANCELLED) {
                t.setEngineer(i % 2 == 0 ? engineer2 : engineer);
            }
            if (status == TicketStatus.CLOSED) {
                t.setClosedAt(t.getUpdatedAt());
            }
            tickets.save(t);
            if (status == TicketStatus.RESOLVED || status == TicketStatus.CLOSED || i == 8) {
                RepairRecord r = new RepairRecord();
                r.setTicket(t);
                r.setEngineer(t.getEngineer());
                r.setCause("設備或設定異常");
                r.setSolution("檢查連線與系統設定，調整後重新測試");
                r.setResult("基本功能測試正常，請申請人再次確認");
                r.setCreatedAt(t.getUpdatedAt().minus(1, ChronoUnit.MINUTES));
                repairs.save(r);
                if (i == 8) {
                    t.setReturnReason("問題仍偶爾發生，請再協助檢查");
                }
            }
        }
    }
}
