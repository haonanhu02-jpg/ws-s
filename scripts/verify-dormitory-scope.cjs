// Read-only browser regression: all APIs are mocked; no business data is written.
const assert = require('node:assert/strict');
const { chromium } = require(process.env.PLAYWRIGHT_MODULE || 'C:/Users/12778/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/playwright');
const names = ['岙底罗', '花城宿舍', '伏龙宿舍', '盛心公寓'];
const buildings = names.map((name, i) => ({ building: { id: i + 1, name, regionName: '总部', enabled: true, displayOrder: i }, rooms: Array.from({ length: 24 }, (_, j) => ({ id: i * 100 + j + 1, buildingId: i + 1, roomNo: String(201 + j), floorNo: 2, facing: '南', roomType: '单间', livable: true, enabled: true, displayOrder: j, cleaningRequired: false, beds: [{ id: i * 100 + j + 1, roomId: i * 100 + j + 1, label: '单床', bedCode: `${name}-${201+j}-单床`, enabled: true, cleaningRequired: false }] })) }));
const stays = buildings.map((n, i) => ({ id: i+1, bed: n.rooms[0].beds[0], person: { id: i+1, name: `人员${i}`, department: '部门', centerName: '中心', gender: '男', category: '员工' }, status: 'BOOKED', plannedMoveIn: '2026-01-01', bedType: '长住房', costCut: true }));
(async () => {
  const browser = await chromium.launch({ headless: true, executablePath: process.env.CHROME_PATH || 'C:/Program Files/Google/Chrome/Application/chrome.exe' });
  try {
    const page = await browser.newPage({ viewport: { width: 1366, height: 768 } });
    await page.addInitScript(() => sessionStorage.setItem('visitor-user', JSON.stringify({ username: 'scope-test', role: 'DORM_ADMIN' })));
    await page.route('**/api/visitor/**', async route => {
      const url = new URL(route.request().url());
      let body = [];
      if (url.pathname.endsWith('/resources/tree')) body = { buildings };
      else if (url.pathname.endsWith('/stays')) body = stays;
      else if (url.pathname.endsWith('/people')) body = stays.map(s => s.person);
      else if (url.pathname.endsWith('/statistics')) body = { summary: {}, buildings: [], categories: [], statuses: [] };
      await route.fulfill({ json: body });
    });
    await page.goto(process.env.SCOPE_URL || 'http://127.0.0.1:5173/visitor/dormitory');
    await page.locator('.dorm-nav button').filter({ hasText: '设置' }).click();
    await page.locator('.resource-setting').first().waitFor();
    await page.locator('.region-tabs button').filter({ hasText: '伏龙宿舍' }).click();
    assert.deepEqual(await page.locator('.resource-setting>header strong').allTextContents(), ['伏龙宿舍'], 'settings must follow selected dormitory');
    await page.locator('.region-tabs button').filter({ hasText: '全集团' }).click();
    assert.deepEqual(await page.locator('.region-tabs button').allTextContents(), ['全集团', '盛心公寓', '伏龙宿舍', '花城宿舍', '岙底罗(万盛空间)']);
    assert.deepEqual(await page.locator('.resource-setting>header strong').allTextContents(), ['盛心公寓', '伏龙宿舍', '花城宿舍', '岙底罗']);
    const contentBox = await page.locator('.dorm-content').boundingBox();
    assert(contentBox && contentBox.x >= 176, 'content must not overlap fixed sidebar');
    await page.locator('.dorm-nav button').filter({ hasText: '统计总览' }).click();
    assert.deepEqual(await page.locator('.stat-summary-block>h3').allTextContents(), ['盛心公寓总览', '伏龙宿舍总览', '花城宿舍总览', '岙底罗总览']);
    await page.locator('.dorm-nav button').filter({ hasText: '设置' }).click();
    await page.evaluate(() => window.scrollTo(0, 1000));
    await page.waitForTimeout(150);
    const nav = await page.locator('.dorm-nav').boundingBox();
    const navStyle = await page.locator('.dorm-nav').evaluate(el => { const c = getComputedStyle(el); return { height: c.height, minHeight: c.minHeight, variable: c.getPropertyValue('--dorm-head-height'), padding: c.padding }; });
    assert(nav && nav.y >= 0 && nav.y + nav.height <= 769, `sidebar stays visible after scroll: ${JSON.stringify({ nav, navStyle })}`);
    for (const name of ['盛心公寓', '伏龙宿舍', '花城宿舍', '岙底罗(万盛空间)']) {
      await page.locator('.region-tabs button').filter({ hasText: name }).click();
      await page.locator('.dorm-nav button').filter({ hasText: '入住台账' }).click();
      const beds = await page.locator('.dorm-content > .table-wrap').first().locator('tbody tr').allTextContents();
      const rawName = name.startsWith('岙') ? '岙底罗' : name;
      assert(beds.length === 1 && beds[0].includes(rawName), `ledger scope: ${name}`);
      await page.locator('.dorm-nav button').filter({ hasText: '统计总览' }).click();
      assert.deepEqual(await page.locator('.stat-summary-block>h3').allTextContents(), [`${rawName}总览`]);
    }
    await page.setViewportSize({ width: 390, height: 844 });
    await page.locator('.dorm-nav button').filter({ hasText: '设置' }).click();
    await page.evaluate(() => window.scrollTo(0, 500));
    await page.waitForTimeout(150);
    const mobileNav = await page.locator('.dorm-nav').boundingBox();
    assert(mobileNav && mobileNav.y >= 0 && mobileNav.y < 844, 'mobile navigation stays visible');
    console.log('Dormitory scope/order/sticky navigation regression passed');
  } finally { await browser.close(); }
})().catch(e => { console.error(e); process.exitCode = 1; });
