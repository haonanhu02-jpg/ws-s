// Mocked HTTP regression: real Excel import UI, editing, reload and Excel export.
const assert = require('node:assert/strict');
const ExcelJS = require('../frontend/visitor-web/node_modules/exceljs');
const { chromium } = require(process.env.PLAYWRIGHT_MODULE || 'C:/Users/12778/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/playwright');
(async () => {
  const browser = await chromium.launch({ headless: true, executablePath: process.env.CHROME_PATH || 'C:/Program Files/Google/Chrome/Application/chrome.exe' });
  try {
    const page = await browser.newPage({ viewport: { width: 1280, height: 800 } });
    await page.clock.install({ time: new Date('2026-10-09T12:00:00+08:00') });
    await page.addInitScript(() => sessionStorage.setItem('visitor-user', JSON.stringify({ username: 'test', role: 'DORM_ADMIN' })));
    const nodes = ['盛心公寓', '伏龙宿舍', '花城宿舍', '岙底罗'].map((name, i) => {
      const id = i + 1;
      const roomNo = ['317', '210', '主卧', '206'][i];
      const bed = { id, roomId: id, label: '单床', bedCode: `${name}-${roomNo}`, enabled: true, cleaningRequired: false };
      return { building: { id, name, enabled: true, displayOrder: i }, rooms: [{ id, buildingId: id, roomNo, floorNo: i === 0 ? 3 : 2, facing: '南', roomType: '单间', enabled: true, livable: true, cleaningRequired: false, beds: [bed] }] };
    });
    const stays = [], writes = [];
    await page.route('**/api/visitor/**', async route => {
      const req = route.request(), url = new URL(req.url()), pathname = url.pathname;
      if (pathname.endsWith('/imports/stays')) {
        for (const command of req.postDataJSON()) {
          const bed = nodes.flatMap(n => n.rooms).flatMap(r => r.beds).find(b => b.bedCode === command.bedCode);
          stays.push({ ...command, id: bed.id, version: 0, bed, status: 'CHECKED_IN', person: { id: bed.id, name: command.name, centerName: command.centerName, department: command.department, gender: command.gender || '未填写', category: command.category || '未分类' }, cleaningRequired: false });
        }
        return route.fulfill({ json: { staysCreated: 4, staysUpdated: 0, peopleCreated: 4, skipped: [] } });
      }
      if (req.method() === 'PUT' && /\/stays\/\d+$/.test(pathname)) {
        const command = req.postDataJSON(); writes.push(command);
        // Same gender validation as the real UpdateStayCommand HTTP contract.
        if (command.gender != null && !['男', '女'].includes(command.gender)) return route.fulfill({ status: 400, json: { detail: '性别必须为男或女，或留空' } });
        const stay = stays.find(s => s.id === Number(pathname.split('/').pop()));
        Object.assign(stay, command, { person: { ...stay.person, ...command, gender: command.gender || '未填写' } });
        return route.fulfill({ json: stay });
      }
      const body = pathname.endsWith('/resources/tree') ? { buildings: nodes } : pathname.endsWith('/stays') ? stays : pathname.endsWith('/people') ? stays.map(s => s.person) : pathname.endsWith('/statistics') ? { summary: {}, buildings: [], categories: [], statuses: [] } : [];
      return route.fulfill({ json: body });
    });
    await page.goto(process.env.SCOPE_URL || 'http://127.0.0.1:5173/visitor/dormitory');
    await page.locator('.dorm-nav button').filter({ hasText: '入住台账' }).click();
    const book = new ExcelJS.Workbook(), sheet = book.addWorksheet('导入');
    sheet.addRow(['姓名', '中心', '部门', '床位编码', '计划入住', '员工入住申请单编码']);
    nodes.forEach(n => sheet.addRow([`导入${n.building.id}`, '导入中心', '导入部门', n.rooms[0].beds[0].bedCode, '2026-09-01', `APP-${n.building.id}`]));
    const chooser = page.waitForEvent('filechooser');
    await page.getByRole('button', { name: '导入入住数据', exact: true }).click();
    await (await chooser).setFiles({ name: 'import.xlsx', mimeType: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', buffer: Buffer.from(await book.xlsx.writeBuffer()) });
    await page.getByText('入住数据导入完成：新增住宿 4', { exact: false }).waitFor();
    for (const n of nodes) {
      await page.locator('.region-tabs button').filter({ hasText: n.building.name }).click();
      await page.locator('.dorm-nav button').filter({ hasText: '可视化平面图' }).click();
      await page.locator('.fp-beds button').first().click();
      for (const [label, value] of [['姓名', `已修改${n.building.id}`], ['中心', '修改中心'], ['部门', '修改部门'], ['备注', '修改后导出']]) {
        await page.locator('.booking-grid label').filter({ hasText: new RegExp(`^${label}`) }).locator('input,textarea').fill(value);
      }
      await page.getByRole('button', { name: '保存修改', exact: true }).click();
      await page.locator('.dorm-booking').waitFor({ state: 'hidden', timeout: 3000 });
      await page.reload();
      await page.locator('.region-tabs button').filter({ hasText: n.building.name }).click();
      await page.locator('.dorm-nav button').filter({ hasText: '入住台账' }).click();
      const download = page.waitForEvent('download');
      await page.getByRole('button', { name: '导出当前在住及预订（完整字段）', exact: true }).click();
      const output = new ExcelJS.Workbook(); await output.xlsx.readFile(await (await download).path());
      const data = output.worksheets[0], headers = data.getRow(1).values;
      const cell = name => data.getRow(2).getCell(headers.indexOf(name)).value;
      assert.equal(cell('姓名'), `已修改${n.building.id}`); assert.equal(cell('中心'), '修改中心'); assert.equal(cell('部门'), '修改部门'); assert.equal(cell('备注'), '修改后导出');
      assert.equal(data.rowCount, 2, 'export follows selected dormitory');
    }
    assert.equal(writes.length, 4);
    let resourceId = 100;
    const roomNos = [['319','321','323','325','327','329','331'], ['209','208','207','206','205','204','203'], ['次卧1','次卧2','次卧3','次卧4','次卧5','次卧6','次卧7'], ['205','204','203','202','201','207','208']];
    for (const [index, n] of nodes.entries()) {
      for (let kind = 0; kind < 7; kind++) {
        const id = resourceId++;
        const room = { id, buildingId: n.building.id, roomNo: roomNos[index][kind], floorNo: index === 0 ? 3 : 2, roomType: kind < 4 ? '标间' : '单间', enabled: kind !== 6, livable: true, cleaningRequired: false, beds: [] };
        for (let j = 0; j < (kind < 4 ? 2 : 1); j++) room.beds.push({ id: resourceId++, roomId: id, bedCode: `capacity-${id}-${j}`, label: j ? '靠门' : '靠窗', enabled: true, cleaningRequired: kind === 4 });
        n.rooms.push(room);
        if ([0,1,3,5].includes(kind)) {
          stays.push({ id: resourceId++, bed: room.beds[0], status: 'CHECKED_IN', plannedMoveIn: kind === 3 ? '2026-10-19' : '2026-09-01', person: { id: resourceId++, name: kind === 5 ? '未填写' : `统计人员${kind}`, gender: kind === 1 || kind === 5 ? '女' : '男', department: '部门' }, bedType: '长住房' });
        }
      }
    }
    await page.reload();
    for (const n of nodes) {
      await page.locator('.region-tabs button').filter({ hasText: n.building.name }).click();
      await page.locator('.dorm-nav button').filter({ hasText: '可视化平面图' }).click();
      const green = await page.locator('.fp-beds button').evaluateAll(elements => elements.filter(el => getComputedStyle(el).backgroundColor === 'rgb(232, 245, 233)').length);
      assert.equal(green, 6, `${n.building.name}: six available green beds`);
      await page.locator('.dorm-nav button').filter({ hasText: '统计总览' }).click();
      const cells = await page.locator('.stat-summary-block .summary-total td').allTextContents();
      assert.deepEqual(cells, ['合计','4','1','1','1','1','1','0','9'], `${n.building.name}: pending, male, female and occupied counts follow map`);
      assert.equal(cells.slice(1,4).reduce((sum,c) => sum + Number(c),0), green);
      const download = page.waitForEvent('download');
      await page.getByRole('button', { name: '导出统计总览', exact: true }).click();
      const workbook = new ExcelJS.Workbook(); await workbook.xlsx.readFile(await (await download).path());
      assert.deepEqual(workbook.worksheets[0].getRow(4).values.slice(2,10), ['合计',4,1,1,1,1,1,0], 'statistics export matches screen');
    }
    await page.locator('.region-tabs button').filter({ hasText: '全集团' }).click();
    assert.equal(await page.locator('.stat-summary-block').count(), 4);
    console.log('Four-dormitory import/edit/reload/export and map-aligned gender/cleaning capacity passed');
  } finally { await browser.close(); }
})().catch(e => { console.error(e); process.exitCode = 1; });
