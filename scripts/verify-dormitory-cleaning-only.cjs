// Mocked APIs only. A cleaning-only save must never create or change a stay/person.
const assert = require('node:assert/strict');
const { chromium } = require(process.env.PLAYWRIGHT_MODULE || 'C:/Users/12778/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/playwright');
(async () => {
  const browser = await chromium.launch({ headless: true, executablePath: process.env.CHROME_PATH || 'C:/Program Files/Google/Chrome/Application/chrome.exe' });
  try {
    const page = await browser.newPage({ viewport: { width: 1280, height: 800 } });
    await page.clock.install({ time: new Date('2026-10-10T12:00:00+08:00') });
    await page.addInitScript(() => sessionStorage.setItem('visitor-user', JSON.stringify({ username: 'test', role: 'DORM_ADMIN' })));
    const nodes = ['盛心公寓','伏龙宿舍','花城宿舍','岙底罗'].map((name,i) => ({ building: { id:i+1,name,enabled:true,displayOrder:i }, rooms: [{ id:i+1,buildingId:i+1,roomNo:['317','210','主卧','206'][i],floorNo:i===0?3:2,roomType:'单间',enabled:true,livable:true,cleaningRequired:false,beds:[{id:i+1,roomId:i+1,label:'单床',bedCode:`${name}-测试`,enabled:true,cleaningRequired:true}]}] }));
    // Include a current stay, a future booking and a historical stay per bed.
    const stays = nodes.flatMap(n => ['CHECKED_IN','BOOKED','CHECKED_OUT'].map((status,j) => ({ id:n.building.id*10+j,version:0,status,person:{id:n.building.id,name:`人员${n.building.id}-${j}`,gender:'男',department:'部门'},bed:n.rooms[0].beds[0],plannedMoveIn:j===1?'2026-10-19':'2026-09-01',plannedMoveOut:j===0?'2026-10-18':undefined,bedType:'长住房',cleaningRequired:false,costCut:false })));
    const expectedStays = JSON.stringify(stays.map(({bed,...s})=>s));
    const writes=[];
    await page.route('**/api/visitor/**', async route => {
      const request=route.request(),path=new URL(request.url()).pathname;
      if(request.method()!=='GET') {
        const body=request.postDataJSON();writes.push({path,body});
        if(request.method()==='PUT' && /\/stays\/\d+$/.test(path)) {
          const stay=stays.find(s=>s.id===Number(path.split('/').at(-1)));
          const {name,centerName,department,gender,category,positionName,rankName,...details}=body;
          Object.assign(stay,details,{person:{...stay.person,name,centerName,department,gender,category,positionName,rankName}});
          return route.fulfill({json:stay});
        }
        if(/\/beds\/\d+\/cleaning$/.test(path)) {
          const id=Number(path.split('/').at(-2));nodes.find(n=>n.building.id===id).rooms[0].beds[0].cleaningRequired=body.required;
          return route.fulfill({json:{}});
        }
        if(path.endsWith('/people')) return route.fulfill({json:{id:999}});
        return route.fulfill({status:409,json:{detail:'该床位在所选入住日期内存在冲突'}});
      }
      return route.fulfill({json:path.endsWith('/resources/tree')?{buildings:nodes}:path.endsWith('/stays')?stays:path.endsWith('/people')?stays.map(s=>s.person):path.endsWith('/statistics')?{summary:{},buildings:[],categories:[],statuses:[]}:[]});
    });
    await page.goto(process.env.SCOPE_URL || 'http://127.0.0.1:5173/visitor/dormitory');
    await page.locator('.dorm-nav button').filter({hasText:'可视化平面图'}).click();
    for(const n of nodes) for(const option of ['', '否']) {
      n.rooms[0].beds[0].cleaningRequired=true;
      await page.reload();
      await page.locator('.dorm-nav button').filter({hasText:'可视化平面图'}).click();
      await page.locator('.region-tabs button').filter({hasText:n.building.name}).click();
      await page.locator('.fp-beds button').first().click();
      const select=page.locator('.booking-grid label').filter({hasText:'是否待打扫'}).locator('select');
      await select.selectOption({label:option});
      // Save whatever the old/new UI labels the submit action.
      await page.locator('.dorm-booking>button').last().click();
      await page.locator('.dorm-booking').waitFor({state:'hidden',timeout:3000});
      assert.deepEqual(writes.pop(),{path:`/api/visitor/dormitory/employee/beds/${n.building.id}/cleaning`,body:{required:false}});
      assert.equal(writes.length,0,'no extra people/stay writes');
      assert.equal(JSON.stringify(stays.map(({bed,...s})=>s)),expectedStays,'current/future/history unchanged');
      await page.reload();
      assert.equal(n.rooms[0].beds[0].cleaningRequired,false);
    }
    console.log('Four-dormitory cleaning yes to blank/no saves without creating stays or modifying history/bookings');
    await page.locator('.dorm-nav button').filter({hasText:'可视化平面图'}).click();
    for(const n of nodes) {
      await page.locator('.region-tabs button').filter({hasText:n.building.name}).click();
      await page.locator('.fp-beds button').first().click();
      await page.locator('.booking-schedule button').filter({hasText:`人员${n.building.id}-0`}).click();
      for(const input of await page.locator('.booking-grid input, .booking-grid textarea').all()) await input.fill('');
      for(const select of await page.locator('.booking-grid select').all()) await select.selectOption({index:0});
      await page.locator('.dorm-booking>button').last().click();
      await page.locator('.dorm-booking').waitFor({state:'hidden',timeout:3000});
      const write=writes.pop();assert.match(write.path,/\/stays\/\d+$/);
      for(const key of ['name','department','centerName','positionName','rankName']) assert.equal(write.body[key],'');
      for(const key of ['gender','category','bedType','plannedMoveIn','plannedMoveOut','costCut','promiseSigned','cleaningRequired','moveInWater']) assert.equal(write.body[key],null,key);
      assert.equal(writes.length,0);
      await page.reload();
      await page.locator('.dorm-nav button').filter({hasText:'可视化平面图'}).click();
      await page.locator('.region-tabs button').filter({hasText:n.building.name}).click();
      await page.locator('.fp-beds button').first().click();
      for(const input of await page.locator('.booking-grid input, .booking-grid textarea').all()) assert.equal(await input.inputValue(),'');
      for(const select of await page.locator('.booking-grid select').all()) assert.equal(await select.locator('option:checked').innerText(),'');
      await page.locator('.dorm-booking .drawer-close').click();
    }
    console.log('Four-dormitory blank edits preserve blank text, dates, numbers and yes/no fields after reload');
  } finally { await browser.close(); }
})().catch(e=>{console.error(e);process.exitCode=1;});
