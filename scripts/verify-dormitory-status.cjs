// Mocked browser regression; never writes production data.
const assert = require('node:assert/strict');
const { chromium } = require(process.env.PLAYWRIGHT_MODULE || 'C:/Users/12778/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/playwright');
(async () => {
  const browser = await chromium.launch({ headless: true, executablePath: process.env.CHROME_PATH || 'C:/Program Files/Google/Chrome/Application/chrome.exe' });
  try {
    const page = await browser.newPage();
    await page.clock.install({ time: new Date('2026-10-09T12:00:00+08:00') });
    await page.addInitScript(() => sessionStorage.setItem('visitor-user', JSON.stringify({ username: 'test', role: 'DORM_ADMIN' })));
    const rooms = [317,318,319].map((roomNo,i) => ({ id:i+1,buildingId:1,roomNo:String(roomNo),floorNo:3,facing:'南',roomType:'单间',enabled:true,livable:true,cleaningRequired:false,beds:[{ id:i+1,roomId:i+1,label:'单床',bedCode:`盛心公寓-${roomNo}-单床`,enabled:true,cleaningRequired:false }] }));
    const stays = rooms.map((r,i) => ({ id:i+1,bed:r.beds[0],person:{id:i+1,name:['尚建华','欧阳春','缺编码'][i],department:'法务部',centerName:'中心',gender:'男',category:'己审批长住员工'},status:i===2?'BOOKED':'CHECKED_IN',plannedMoveIn:i===0?'2026-10-19':'2026-09-01',bedType:'长住房',applicationCode:i===1?'YGRZ2026090001':'',costCut:false }));
    stays[2].plannedMoveOut='2026-10-15';
    stays.push({...stays[2],id:4,plannedMoveIn:'2026-10-20',plannedMoveOut:undefined,applicationCode:'NEXT-001'});
    await page.route('**/api/visitor/**',async route => {
      const url=new URL(route.request().url()); let body=[];
      if(url.pathname.endsWith('/resources/tree')) body={buildings:[{building:{id:1,name:'盛心公寓',enabled:true,displayOrder:0},rooms}]};
      else if(/\/stays\/\d+$/.test(url.pathname) && route.request().method()==='PUT') {
        const stay=stays.find(s=>s.id===Number(url.pathname.split('/').pop())); Object.assign(stay,route.request().postDataJSON()); body=stay;
      } else if(url.pathname.endsWith('/stays')) body=stays;
      else if(url.pathname.endsWith('/people')) body=stays.map(s=>s.person);
      else if(url.pathname.endsWith('/statistics')) body={summary:{},buildings:[],categories:[],statuses:[]};
      await route.fulfill({json:body});
    });
    await page.goto(process.env.SCOPE_URL || 'http://127.0.0.1:5173/visitor/dormitory');
    const nav=label=>page.locator('.dorm-nav button').filter({hasText:label});
    await nav('可视化平面图').click();
    // Use the bed label so this assertion does not depend on room-number typography.
    const bed=page.locator('.fp-beds button[aria-label^="317"]');
    await bed.first().waitFor();
    assert(await bed.first().evaluate(el=>el.closest('article').classList.contains('book')), 'future CHECKED_IN record must render as booked room');
    assert(await bed.first().evaluate(el=>el.classList.contains('booked') && !el.classList.contains('occupied')), 'future bed must only use booked colour');
    assert(await page.locator('.fp-beds button[aria-label^="319"]').first().evaluate(el=>el.classList.contains('occupied') && !el.classList.contains('booked')), 'past BOOKED record must only use occupied colour');
    await bed.first().click();
    assert((await page.locator('.booking-schedule button').allTextContents()).join('').includes('已预定'), 'reservation editor must match the map');
    await page.locator('.drawer-close').first().click();
    await nav('预警看板').click();
    const application=page.locator('section,article,div').filter({has:page.locator('h3').filter({hasText:'员工入住申请单编码'})}).last();
    assert(!(await application.innerText()).includes('欧阳春'), 'filled application code must not alert');
    assert(!(await application.innerText()).includes('尚建华'), 'future stay must not alert');
    assert((await application.innerText()).includes('缺编码'), 'missing due code must alert');
    await nav('可视化平面图').click();
    await page.locator('.fp-beds button[aria-label^="319"]').first().click();
    assert.equal(await page.locator('label').filter({hasText:'员工入住申请单编码'}).locator('input').inputValue(), '', 'default editor must open current missing-code record, not later filled reservation');
    await page.locator('.drawer-close').first().click();
    await nav('预警看板').click();
    await application.getByRole('button',{name:'补填编码'}).click();
    await page.locator('label').filter({hasText:'员工入住申请单编码'}).locator('input').fill('YGRZ2026100002');
    await page.getByRole('button',{name:'保存修改',exact:true}).click();
    await nav('预警看板').click();
    assert((await application.innerText()).includes('（0）'), 'saving code clears warning immediately');
    await page.reload();
    await nav('预警看板').click();
    assert((await application.innerText()).includes('（0）'), 'saved code stays clear after reload');
    await nav('入住台账').click();
    const futureRow=page.locator('tbody tr').filter({hasText:'尚建华'}).first();
    assert((await futureRow.locator('.ledger-status').innerText()).includes('已预定'), 'ledger matches future room status');
    console.log('Dormitory date/status/application-code regression passed');
  } finally { await browser.close(); }
})().catch(e=>{console.error(e);process.exitCode=1;});
