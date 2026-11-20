const {
    chromium
} = require('../.tools/browser/node_modules/playwright');
const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
const output = '.tools/acceptance';
process.env.TEMP = process.env.TMP = path.resolve(output, 'browser-temp');
fs.mkdirSync(process.env.TEMP, {
    recursive: true
});
const base = process.env.HELPDESK_TEST_URL || 'http://127.0.0.1:18080';
const previous = JSON.parse(fs.readFileSync(output + '/browser-result.json', 'utf8'));
(async () => {
    const browser = await chromium.launch({
        executablePath: process.env.BROWSER_PATH ||
            'C:/Program Files/Google/Chrome/Application/chrome.exe',
        headless: true,
        chromiumSandbox: true,
        args: ['--disable-background-networking', '--disable-sync',
            '--no-first-run'
        ]
    });
    try {
        const context = await browser.newContext({
            serviceWorkers: 'block'
        });
        const external = [];
        await context.route('**/*', route => {
            if (new URL(route.request().url()).origin === new URL(base).origin)
                return route.continue();
            external.push(route.request().url());
            return route.abort();
        });
        const page = await context.newPage();
        await page.goto(base + '/login');
        await page.locator('input[name=username]').fill('admin');
        await page.locator('input[name=password]').fill('Demo1234!');
        await page.getByRole('button', {
            name: '登入 →',
            exact: true
        }).click();
        await page.waitForURL('**/dashboard');
        assert.equal(Number(await page.locator('.stat-card .number').first().innerText()),
            previous.snapshot.ticketCount);
        for (const [route, key] of [
                ['users', 'userCount'],
                ['departments', 'departmentCount'],
                ['categories', 'categoryCount']
            ]) {
            await page.goto(base + '/admin/' + route);
            assert.equal(await page.locator('tbody tr').count(), previous.snapshot[key]);
        }
        await page.goto(previous.ticketUrl);
        assert.equal((await page.locator('main dl .status').first().innerText()).trim(),
            '已結案');
        assert.equal(await page.locator('.repair-card').count(), 2);
        assert.ok((await page.locator('main').innerText()).includes('第二次長時間測試正常'));
        assert.deepEqual(external, []);
        const result = {
            passed: true,
            preserved: previous.snapshot,
            ticketUrl: previous.ticketUrl,
            repairRecords: 2,
            noDuplicateDemo: true,
            externalRequests: external
        };
        fs.writeFileSync(output + '/restart-result.json', JSON.stringify(result, null, 2));
        console.log(JSON.stringify(result, null, 2));
        await context.close();
    } finally {
        await browser.close();
    }
})().catch(error => {
    console.error(error);
    process.exitCode = 1;
});
