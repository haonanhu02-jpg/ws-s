const assert = require('node:assert/strict');
const { chromium } = require(process.env.PLAYWRIGHT_MODULE || 'C:/Users/12778/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/playwright');
(async () => {
  const browser = await chromium.launch({ headless: true, executablePath: process.env.CHROME_PATH || 'C:/Program Files/Google/Chrome/Application/chrome.exe' });
  try {
    const page = await browser.newPage();
    await page.addInitScript(() => sessionStorage.setItem('visitor-user', JSON.stringify({ username: 'layout-test', role: 'DORM_ADMIN' })));
    await page.route('**/api/visitor/**', route => {
      const url = new URL(route.request().url());
      return route.fulfill({ json: url.pathname.endsWith('/resources/tree') ? { buildings: [] } : url.pathname.endsWith('/statistics') ? { summary: {}, categories: [], buildings: [], statuses: [] } : [] });
    });
    await page.goto(process.env.SCOPE_URL || 'http://127.0.0.1:5173/visitor/dormitory');
    for (const width of [1280, 900, 390]) {
      await page.setViewportSize({ width, height: 900 });
      await page.locator('.dorm-nav button').filter({ hasText: '统计总览' }).click();
      const cost = page.locator('.cost-cut-analysis');
      await cost.waitFor();
      const fields = await cost.locator('input,select').evaluateAll(els => els.map(el => ({ box: el.getBoundingClientRect().toJSON(), parent: el.closest('section').getBoundingClientRect().toJSON() })));
      for (const { box, parent } of fields) assert(box.x >= parent.x && box.right <= parent.right + 1, `cost filter overflows at ${width}`);
      for (let i = 1; i < fields.length; i++) {
        const a = fields[i-1].box, b = fields[i].box;
        assert(a.right <= b.x || a.bottom <= b.y, `cost filters overlap at ${width}`);
      }
      if (process.env.QA_DIR) await cost.screenshot({ path: `${process.env.QA_DIR}/cost-layout-${width}.png` });
      await page.locator('.dorm-nav button').filter({ hasText: '水电报表' }).click();
      const fee = page.locator('.fee-section');
      for (const button of await fee.locator('.section-title button,.fee-rule-grid>button').all()) {
        const box = await button.boundingBox();
        assert(box.height >= 36 && box.height <= 44, `fee button height ${box.height} at ${width}`);
        const parent = await fee.boundingBox();
        assert(box.x >= parent.x && box.x + box.width <= parent.x + parent.width + 1, `fee button overflows at ${width}`);
      }
      if (process.env.QA_DIR) await fee.screenshot({ path: `${process.env.QA_DIR}/fee-layout-${width}.png` });
    }
    console.log('Cost filters and compact settlement layout passed at desktop, tablet and mobile widths');
  } finally { await browser.close(); }
})().catch(e => { console.error(e); process.exitCode = 1; });
