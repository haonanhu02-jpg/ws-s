// Read-only browser regression against mocked APIs, covering all four dormitories.
const assert = require('node:assert/strict');
const fs = require('node:fs/promises');
const path = require('node:path');
const { chromium } = require(process.env.PLAYWRIGHT_MODULE || 'C:/Users/12778/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/playwright');
(async () => {
  const browser = await chromium.launch({ headless: true, executablePath: process.env.CHROME_PATH || 'C:/Program Files/Google/Chrome/Application/chrome.exe' });
  try {
    const page = await browser.newPage({ viewport: { width: 1280, height: 720 } });
    await page.clock.install({ time: new Date('2026-10-18T23:59:30+08:00') });
    await page.addInitScript(() => sessionStorage.setItem('visitor-user', JSON.stringify({ username: 'test', role: 'DORM_ADMIN' })));
    const buildings = ['盛心公寓', '伏龙宿舍', '花城宿舍', '岙底罗(万盛空间)'].map((name, i) => {
      const id = i + 1;
      const roomNo = ['317', '210', '主卧', '206'][i];
      return { building: { id, name, enabled: true, displayOrder: i }, rooms: [{ id, buildingId: id, roomNo, floorNo: i === 0 ? 3 : 2, facing: '南', roomType: '单间', enabled: true, livable: true, cleaningRequired: false, beds: [{ id, roomId: id, label: '单床', bedCode: `${name}-${roomNo}`, enabled: true, cleaningRequired: false }] }] };
    });
    const stays = buildings.map((node, i) => ({ id: i + 1, bed: node.rooms[0].beds[0], person: { id: i + 1, name: `预订人员${i + 1}`, department: '法务部', gender: '男', category: '' }, status: i % 2 ? 'BOOKED' : 'CHECKED_IN', plannedMoveIn: '2026-10-19', costCut: false }));
    await page.route('**/api/visitor/**', async route => {
      const path = new URL(route.request().url()).pathname;
      const body = path.endsWith('/resources/tree') ? { buildings } : path.endsWith('/stays') ? stays : path.endsWith('/people') ? stays.map(s => s.person) : path.endsWith('/statistics') ? { summary: {}, buildings: [], categories: [], statuses: [] } : [];
      await route.fulfill({ json: body });
    });
    await page.goto(process.env.SCOPE_URL || 'http://127.0.0.1:5173/visitor/dormitory');
    await page.locator('.dorm-nav button').filter({ hasText: '可视化平面图' }).click();
    const failures = [];
    const check = (condition, message) => { if (!condition) failures.push(message); };
    for (const node of buildings) {
      const tab = page.locator('.region-tabs button').filter({ hasText: node.building.name });
      await tab.click();
      const bed = page.locator('.fp-beds button').first();
      await bed.waitFor();
      check(await bed.evaluate(el => el.classList.contains('booked') && !el.classList.contains('occupied')), `${node.building.name}: future bed must be booked`);
      check(await bed.evaluate(el => getComputedStyle(el).backgroundColor === 'rgb(255, 248, 225)'), `${node.building.name}: future bed must visibly be yellow`);
      await bed.click();
      const reservation = page.locator('.booking-schedule button').first();
      check((await reservation.innerText()).includes('已预定'), `${node.building.name}: future editor must say booked`);
      check(await reservation.evaluate(el => getComputedStyle(el).backgroundColor === 'rgb(255, 248, 225)'), `${node.building.name}: selected future reservation must be yellow`);
      const card = page.locator('.dorm-booking');
      const before = await page.locator('.dorm-booking .drawer-close').boundingBox();
      await card.evaluate(el => { el.scrollTop = el.scrollHeight; });
      await page.waitForTimeout(50);
      const after = await page.locator('.dorm-booking .drawer-close').boundingBox();
      check(after && before && Math.abs(after.y - before.y) < 2 && after.y > 0, `${node.building.name}: close must stay at the same visible position after scroll`);
      if (node.building.id === 1 && process.env.QA_DIR) {
        await fs.mkdir(process.env.QA_DIR, { recursive: true });
        await page.screenshot({ path: path.join(process.env.QA_DIR, 'booking-scrolled.png') });
        await card.evaluate(el => { el.scrollTop = 0; });
        await page.screenshot({ path: path.join(process.env.QA_DIR, 'booking-yellow.png') });
        await card.evaluate(el => { el.scrollTop = el.scrollHeight; });
      }
      await page.locator('.dorm-booking .drawer-close').click();
    }
    await page.locator('.region-tabs button').filter({ hasText: '全集团' }).click();
    await page.clock.fastForward(61000);
    const beds = page.locator('.fp-beds button');
    for (let i = 0; i < await beds.count(); i++) check(await beds.nth(i).evaluate(el => el.classList.contains('occupied') && !el.classList.contains('booked')), 'cross-midnight map must update without reload');
    await beds.first().click();
    check((await page.locator('.booking-schedule button').first().innerText()).includes('已入住'), 'cross-midnight editor must match map');
    check(await page.locator('.booking-schedule button').first().evaluate(el => getComputedStyle(el).backgroundColor === 'rgb(219, 234, 254)'), 'occupied editor card must be blue');
    // Resource dialogs share the same close-button rule, including mobile scrolling.
    await page.locator('.dorm-booking .drawer-close').click();
    await page.locator('.dorm-nav button').filter({ hasText: '设置' }).click();
    await page.getByRole('button', { name: '新增楼栋', exact: true }).click();
    await page.setViewportSize({ width: 390, height: 480 });
    const resource = page.locator('.resource-form');
    const before = await resource.locator('.drawer-close').boundingBox();
    await resource.evaluate(el => { el.scrollTop = el.scrollHeight; });
    await page.waitForTimeout(50);
    const after = await resource.locator('.drawer-close').boundingBox();
    check(after && before && Math.abs(after.y - before.y) < 2, 'mobile resource-dialog close must stay fixed while scrolling');
    assert.deepEqual(failures, []);
    console.log('Four-dormitory reservation colours, midnight update and sticky dialog closes passed');
  } finally { await browser.close(); }
})().catch(e => { console.error(e); process.exitCode = 1; });
