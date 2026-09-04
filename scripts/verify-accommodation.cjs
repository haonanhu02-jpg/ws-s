// Local integration test. Creates synthetic records and deliberately retains them (no deletion API).
// SMOKE_PASSWORD=... PLAYWRIGHT_MODULE=... node scripts/verify-accommodation.cjs
const assert = require('node:assert/strict');
const { chromium } = require(process.env.PLAYWRIGHT_MODULE || 'playwright');
const base = process.env.SMOKE_URL || 'http://localhost:8088';
assert(['localhost', '127.0.0.1'].includes(new URL(base).hostname), 'Run only against a local test deployment');
assert(process.env.SMOKE_PASSWORD, 'Set SMOKE_PASSWORD to the local guard/dorm password');
const stamp = Date.now().toString();
const created = [];
async function api(path, method = 'GET', body, auth = '', expected) {
  const r = await fetch(base + '/api/visitor' + path, { method, signal: AbortSignal.timeout(15000), headers: {
    'Content-Type': 'application/json', Authorization: auth, 'X-Operator': 'local-smoke',
  }, body: body === undefined ? undefined : JSON.stringify(body) });
  const text = await r.text();
  assert(expected ? r.status === expected : r.ok, `${method} ${path}: ${r.status} ${text}`);
  return text ? JSON.parse(text) : null;
}
async function eventually(check, label) {
  for (let i = 0; i < 40; i++) { if (await check()) return; await new Promise(r => setTimeout(r, 500)); }
  throw new Error('Timed out: ' + label);
}
async function run() {
  const guardSession = await api('/auth/login', 'POST', { username: 'guard', password: process.env.SMOKE_PASSWORD });
  const dormSession = await api('/auth/login', 'POST', { username: 'dorm', password: process.env.SMOKE_PASSWORD });
  const ga = `${guardSession.tokenType} ${guardSession.accessToken}`, da = `${dormSession.tokenType} ${dormSession.accessToken}`;
  const browser = await chromium.launch({ channel: 'chrome', headless: true });
  try {
    async function pageFor(session, path) {
      const ctx = await browser.newContext({ viewport: { width: 1440, height: 1000 } });
      ctx.setDefaultTimeout(20000);
      await ctx.addInitScript(s => {
        sessionStorage.setItem('visitor-user', JSON.stringify({ username: s.username, role: s.role }));
        sessionStorage.setItem('visitor-authorization', `${s.tokenType} ${s.accessToken}`);
      }, session);
      const page = await ctx.newPage(); await page.goto(base + path); return page;
    }
    // Public form is the destination of the QR code; submit it in a real browser.
    const visitor = await browser.newPage();
    visitor.setDefaultTimeout(20000);
    await visitor.goto(base + '/visitor/register');
    await visitor.getByPlaceholder('请输入真实姓名').fill('扫码测试' + stamp);
    await visitor.getByPlaceholder('用于现场联系').fill('13800000000');
    await visitor.getByPlaceholder('请输入被访人姓名').fill('测试接待人');
    await visitor.getByPlaceholder('请输入部门名称').fill('测试部门');
    await visitor.getByPlaceholder('请简要说明来访目的').fill('自动联调测试');
    await visitor.getByRole('checkbox', { name: /需要住宿/ }).check();
    const qrResponse = visitor.waitForResponse(r => r.url().endsWith('/api/visitor/registrations') && r.request().method() === 'POST');
    await visitor.getByRole('button', { name: '确认提交申请' }).click();
    const qr = await (await qrResponse).json(); assert(qr.visitId); created.push(qr.visitId);
    console.log('PASS public form submitted', qr.visitId);
    await eventually(async () => (await api('/guard/records?status=WAITING_ENTRY', 'GET', undefined, ga)).some(r => r.visitId === qr.visitId && r.accommodationRequired), 'QR -> guard');
    const dorm = await pageFor(dormSession, '/visitor/dormitory');
    await dorm.getByRole('heading', { name: '访客住宿申请' }).waitFor();
    await dorm.getByRole('row').filter({ hasText: '扫码测试' + stamp }).waitFor({ timeout: 25000 });
    console.log('PASS QR visible in dorm browser');
    const guard = await pageFor(guardSession, '/visitor/guard');
    await guard.getByRole('button', { name: '手工登记', exact: true }).click();
    const form = guard.locator('.record-dialog');
    await form.getByLabel(/访客姓名/).fill('手工测试' + stamp);
    await form.getByLabel(/联系方式/).fill('13800000001');
    await form.getByLabel(/被访人/).fill('测试接待人');
    await form.getByRole('checkbox', { name: '需要住宿', exact: true }).check();
    await guard.setViewportSize({ width: 390, height: 844 });
    assert(await form.evaluate(el => el.scrollWidth <= el.clientWidth), 'Mobile form overflows');
    await guard.screenshot({ path: process.env.SMOKE_SCREENSHOT_DIR ? process.env.SMOKE_SCREENSHOT_DIR + '/guard-mobile.png' : 'frontend/visitor-web/dist/guard-mobile.png', fullPage: true });
    console.log('PASS mobile screenshot captured');
    await guard.setViewportSize({ width: 1440, height: 1000 });
    console.log('Desktop viewport restored; saving manual form');
    await form.getByRole('button', { name: '保存', exact: true }).click();
    console.log('Manual save clicked');
    let manual;
    await eventually(async () => { manual = (await api('/guard/records?status=WAITING_ENTRY', 'GET', undefined, ga)).find(r => r.visitorName === '手工测试' + stamp); return !!manual; }, 'browser manual form persisted');
    assert(manual.visitId); created.push(manual.visitId);
    console.log('PASS manual form submitted', manual.visitId);
    await dorm.bringToFront();
    await dorm.getByRole('row').filter({ hasText: '手工测试' + stamp }).waitFor({ timeout: 25000 });
    console.log('PASS manual visible in dorm browser');
    const edit = { visitorName: '修改测试' + stamp, mobile: '13800000002', hostName: '新接待人', plateNumber: null, vehicleEnteringFactory: false, accommodationRequired: true };
    await api('/guard/records/' + manual.visitId, 'PUT', edit, ga);
    await dorm.getByRole('row').filter({ hasText: edit.visitorName }).waitFor({ timeout: 25000 });
    console.log('PASS edited details visible');
    await api('/guard/records/' + manual.visitId, 'PUT', { ...edit, accommodationRequired: false }, ga);
    await eventually(async () => !(await api('/dormitory/records', 'GET', undefined, da)).some(r => r.visitId === manual.visitId), 'pending cancellation');
    await api('/dormitory/records/' + manual.visitId + '/confirm', 'POST', undefined, da, 409);
    await api('/guard/records/' + manual.visitId, 'PUT', edit, ga);
    await eventually(async () => (await api('/dormitory/records', 'GET', undefined, da)).some(r => r.visitId === manual.visitId), 're-request');
    await dorm.getByRole('button', { name: '刷新申请' }).click();
    const row = dorm.getByRole('row').filter({ hasText: edit.visitorName });
    dorm.once('dialog', d => d.accept());
    await row.getByRole('button', { name: '确认住宿', exact: true }).click();
    await row.getByText('已确认 · 待安排床位').waitFor();
    const conflict = await api('/guard/records/' + manual.visitId, 'PUT', { ...edit, accommodationRequired: false }, ga, 409);
    assert.match(conflict.detail || conflict.message || '', /宿舍管理员/);
    const bedCode = 'SMOKE-' + stamp;
    await api('/dormitory/beds', 'POST', { buildingName: '测试楼（非生产）', roomNumber: stamp.slice(-6), bedCode }, da);
    await api('/dormitory/records/' + manual.visitId + '/assign-bed', 'POST', { bedCode }, da);
    await api('/guard/records/' + manual.visitId, 'PUT', { ...edit, visitorName: '保留床位测试' + stamp }, ga);
    await eventually(async () => { const r = await api('/dormitory/records/' + manual.visitId, 'GET', undefined, da); return r.visitorName === '保留床位测试' + stamp && r.bedCode === bedCode && r.accommodationConfirmed; }, 'edit preserves assigned bed');
    const noDorm = await api('/registrations', 'POST', { visitorName: '不住宿测试' + stamp, mobile: '13800000003', hostName: '测试接待人', hostDepartment: '测试部门', visitReason: '测试', accommodationRequired: false, hasVehicle: false, vehicleEnteringFactory: false });
    created.push(noDorm.visitId);
    await eventually(async () => (await api('/guard/records?status=WAITING_ENTRY', 'GET', undefined, ga)).some(r => r.visitId === noDorm.visitId), 'non-accommodation reaches guard');
    // Wait beyond projection and poll intervals before checking non-visibility.
    await new Promise(r => setTimeout(r, 6000));
    assert(!(await api('/dormitory/records', 'GET', undefined, da)).some(r => r.visitId === noDorm.visitId));
    const qrDorm = await api('/dormitory/records/' + qr.visitId, 'GET', undefined, da);
    assert.equal(qrDorm.hostDepartment, '测试部门'); assert.equal(qrDorm.visitReason, '自动联调测试');
    await dorm.getByRole('button', { name: '刷新申请' }).click();
    await dorm.getByRole('row').filter({ hasText: '保留床位测试' + stamp }).waitFor();
    await dorm.screenshot({ path: process.env.SMOKE_SCREENSHOT_DIR ? process.env.SMOKE_SCREENSHOT_DIR + '/visitor-accommodation.png' : 'frontend/visitor-web/dist/visitor-accommodation.png', fullPage: true });
    console.log(JSON.stringify({ passed: true, checks: ['QR browser submission -> guard -> dorm browser', 'manual browser form', 'mobile layout', 'detail sync', 'pending cancel', 'cancelled confirm rejected', 'confirmed cancel rejected', 'bed preserved', 'non-accommodation excluded'], syntheticRecordsRetained: created, syntheticBedRetained: bedCode }));
  } finally { await browser.close(); }
}
run().catch(e => { console.error(e); console.error('Synthetic records created:', created); process.exitCode = 1; });
