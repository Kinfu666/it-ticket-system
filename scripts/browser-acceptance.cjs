// 驗收工具，不是應用程式依賴。套件安裝在專案 .tools/browser。
const {
    chromium
} = require('../.tools/browser/node_modules/playwright');
const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
const base = process.env.HELPDESK_TEST_URL || 'http://127.0.0.1:18080';
const output = '.tools/acceptance';
fs.mkdirSync(output, {
    recursive: true
});
const browserTemp = path.resolve(output, 'browser-temp');
fs.mkdirSync(browserTemp, {
    recursive: true
});
process.env.TEMP = browserTemp;
process.env.TMP = browserTemp;

(async () => {
    const browser = await chromium.launch({
        executablePath: process.env.BROWSER_PATH ||
            'C:/Program Files/Google/Chrome/Application/chrome.exe',
        headless: true,
        chromiumSandbox: true,
        args: ['--disable-background-networking', '--disable-sync',
            '--no-first-run'
        ],
    });
    const errors = [],
        externalRequests = [],
        contexts = [];
    async function pageFor(username) {
        const context = await browser.newContext({
            viewport: {
                width: 1440,
                height: 1000
            },
            serviceWorkers: 'block'
        });
        contexts.push(context);
        await context.route('**/*', route => {
            const url = new URL(route.request().url());
            if (url.origin === new URL(base).origin) return route.continue();
            externalRequests.push(url.href);
            return route.abort();
        });
        const page = await context.newPage();
        page.on('pageerror', error => errors.push(error.message));
        await page.goto(base + '/login');
        if (username === 'employee') await page.screenshot({
            path: output + '/login.png',
            fullPage: true
        });
        await page.getByLabel('帳號', {
            exact: true
        }).fill(username);
        await page.getByLabel('密碼', {
            exact: true
        }).fill('Demo1234!');
        await page.getByRole('button', {
            name: '登入 →',
            exact: true
        }).click();
        await page.waitForURL('**/dashboard');
        return page;
    }
    async function status(page, label) {
        assert.equal((await page.locator('main dl .status').first().innerText()).trim(),
            label);
    }
    async function repair(page, result) {
        await page.getByLabel('故障原因', {
            exact: true
        }).fill('電源供應異常');
        await page.getByLabel('處理方式', {
            exact: true
        }).fill('檢查並更換電源設備');
        await page.getByLabel('維修結果', {
            exact: true
        }).fill(result);
        await page.getByRole('button', {
            name: '提交維修完成',
            exact: true
        }).click();
        await status(page, '待確認');
    }
    try {
        const employee = await pageFor('employee');
        await employee.screenshot({
            path: output + '/employee-dashboard.png',
            fullPage: true
        });
        assert.equal(await employee.locator('a[href="/admin/users"]').count(), 0);
        await employee.goto(base + '/tickets/new');
        const title = '瀏覽器驗收開機異常 ' + Date.now();
        await employee.getByLabel('標題', {
            exact: true
        }).fill(title);
        await employee.locator('select[name=categoryId]').selectOption({
            label: '電腦設備'
        });
        await employee.getByLabel('位置', {
            exact: true
        }).fill('二樓測試區');
        await employee.getByLabel('故障描述', {
            exact: true
        }).fill('虛構驗收案件，電腦無法開機。');
        await employee.getByRole('button', {
            name: '送出申請',
            exact: true
        }).click();
        await employee.waitForURL(/\/tickets\/\d+$/);
        const ticketUrl = employee.url();
        await status(employee, '待分派');
        const admin = await pageFor('admin');
        await admin.screenshot({
            path: output + '/admin-dashboard.png',
            fullPage: true
        });
        await admin.goto(ticketUrl);
        await admin.locator('select[name=engineerId]').selectOption({
            label: '林承宇'
        });
        await admin.getByRole('button', {
            name: '分派／重新分派',
            exact: true
        }).click();
        await status(admin, '待處理');
        const engineer = await pageFor('engineer');
        await engineer.screenshot({
            path: output + '/engineer-dashboard.png',
            fullPage: true
        });
        await engineer.goto(ticketUrl);
        await engineer.getByRole('button', {
            name: '開始維修',
            exact: true
        }).click();
        await status(engineer, '處理中');
        await repair(engineer, '第一次測試正常');
        await employee.goto(ticketUrl);
        await employee.getByLabel('退回原因', {
            exact: true
        }).fill('仍偶爾無法開機，請再檢查');
        await employee.getByRole('button', {
            name: '退回重新處理',
            exact: true
        }).click();
        await status(employee, '處理中');
        await engineer.goto(ticketUrl);
        await repair(engineer, '第二次長時間測試正常');
        await employee.goto(ticketUrl);
        await employee.getByRole('button', {
            name: '確認修復並結案',
            exact: true
        }).click();
        await status(employee, '已結案');
        assert.equal(await employee.locator('.repair-card').count(), 2);
        await employee.screenshot({
            path: output + '/closed-ticket.png',
            fullPage: true
        });
        const other = await pageFor('employee2');
        const forbidden = await other.goto(ticketUrl);
        assert.equal(forbidden.status(), 403);
        await employee.goto(base + '/tickets');
        await employee.getByLabel('標題／單號', {
            exact: true
        }).fill(title);
        await employee.getByRole('button', {
            name: '查詢',
            exact: true
        }).click();
        assert.equal(await employee.locator('tbody tr').count(), 1);
        assert.ok((await employee.locator('tbody').innerText()).includes(title));
        await employee.screenshot({
            path: output + '/ticket-search.png',
            fullPage: true
        });
        await employee.goto(base + '/tickets/new');
        await employee.getByLabel('標題', {
            exact: true
        }).fill('瀏覽器取消驗收');
        await employee.locator('select[name=categoryId]').selectOption({
            label: '電腦設備'
        });
        await employee.getByLabel('位置', {
            exact: true
        }).fill('測試區');
        await employee.getByLabel('故障描述', {
            exact: true
        }).fill('虛構取消測試');
        await employee.getByRole('button', {
            name: '送出申請',
            exact: true
        }).click();
        await employee.getByRole('button', {
            name: '取消報修',
            exact: true
        }).click();
        await status(employee, '已取消');
        await admin.goto(base + '/admin/reports');
        await admin.screenshot({
            path: output + '/reports.png',
            fullPage: true
        });
        const department = '瀏覽器驗收部門 ' + Date.now();
        await admin.goto(base + '/admin/departments/new');
        await admin.getByLabel('名稱', {
            exact: true
        }).fill(department);
        await admin.getByRole('button', {
            name: '儲存',
            exact: true
        }).click();
        assert.ok((await admin.locator('table').innerText()).includes(department));
        const qaUser = 'qa_' + Date.now();
        await admin.goto(base + '/admin/users/new');
        await admin.getByLabel('帳號', {
            exact: true
        }).fill(qaUser);
        await admin.getByLabel('姓名', {
            exact: true
        }).fill('測試用虛構帳號');
        await admin.locator('input[name=password]').fill('Browser123!');
        await admin.locator('select[name=role]').selectOption('EMPLOYEE');
        await admin.locator('select[name=departmentId]').selectOption({
            label: department
        });
        await admin.getByRole('button', {
            name: '儲存',
            exact: true
        }).click();
        const row = admin.getByRole('row').filter({
            hasText: qaUser
        });
        await row.getByRole('button', {
            name: '停用',
            exact: true
        }).click();
        assert.ok((await admin.getByRole('row').filter({
            hasText: qaUser
        }).innerText()).includes('停用'));
        await admin.screenshot({
            path: output + '/user-management.png',
            fullPage: true
        });
        await employee.setViewportSize({
            width: 390,
            height: 844
        });
        await employee.goto(base + '/dashboard');
        await employee.screenshot({
            path: output + '/mobile-dashboard.png',
            fullPage: true
        });
        await employee.getByRole('button', {
            name: /安全登出/
        }).click();
        await employee.waitForURL('**/login?logout');
        await employee.goto(base + '/tickets');
        assert.ok(employee.url().includes('/login'));
        assert.deepEqual(errors, []);
        assert.deepEqual(externalRequests, []);
        await admin.goto(base + '/dashboard');
        const ticketCount = Number(await admin.locator('.stat-card .number').first()
            .innerText());
        await admin.goto(base + '/admin/users');
        const userCount = await admin.locator('tbody tr').count();
        await admin.goto(base + '/admin/departments');
        const departmentCount = await admin.locator('tbody tr').count();
        await admin.goto(base + '/admin/categories');
        const categoryCount = await admin.locator('tbody tr').count();
        const snapshot = {
            ticketCount,
            userCount,
            departmentCount,
            categoryCount
        };
        const result = {
            passed: true,
            ticketUrl,
            title,
            snapshot,
            checked: ['three roles', 'full workflow', 'reject and repair twice',
                'cancel', 'ownership 403', 'search', 'department create',
                'user create/disable', 'logout', 'mobile', 'local resources'
            ],
            pageErrors: errors,
            externalRequests
        };
        fs.writeFileSync(output + '/browser-result.json', JSON.stringify(result, null, 2));
        console.log(JSON.stringify(result, null, 2));
    } catch (error) {
        for (let i = 0; i < contexts.length; i++)
            for (const page of contexts[i].pages()) {
                console.error('FAILED PAGE', page.url());
                await page.screenshot({
                    path: output + '/failure-' + i + '.png',
                    fullPage: true
                });
                fs.writeFileSync(output + '/failure-' + i + '.html', await page.content());
            }
        throw error;
    } finally {
        for (const context of contexts) await context.close();
        await browser.close();
    }
})().catch(error => {
    console.error(error);
    process.exitCode = 1;
});
