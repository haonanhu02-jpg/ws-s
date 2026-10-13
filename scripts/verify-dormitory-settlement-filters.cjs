// Read-only UI regression: combined ledger filters, four-dormitory scope and real Excel export.
const assert = require('node:assert/strict');
const ExcelJS = require('../frontend/visitor-web/node_modules/exceljs');
const { chromium } = require(process.env.PLAYWRIGHT_MODULE || 'C:/Users/12778/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/playwright');
(async () => {
 const browser = await chromium.launch({ headless: true, executablePath: process.env.CHROME_PATH || 'C:/Program Files/Google/Chrome/Application/chrome.exe' });
 try {
  const page = await browser.newPage({ viewport: { width: 1440, height: 1000 } });
  await page.addInitScript(() => sessionStorage.setItem('visitor-user', JSON.stringify({ username: 'test', role: 'DORM_ADMIN' })));
  const names = ['盛心公寓','伏龙宿舍','花城宿舍','岙底罗'];
  const nodes = names.map((name,i) => ({ building: { id:i+1,name,enabled:true,displayOrder:i }, rooms: [0,1].map(j => ({ id:i*2+j+1,buildingId:i+1,roomNo:String(201+j),floorNo:2,roomType:'单间',enabled:true,livable:true,beds:[{id:i*2+j+1,roomId:i*2+j+1,label:'单床',bedCode:`BED-${i}-${j}`,enabled:true,cleaningRequired:false}] })) }));
  const stays = nodes.flatMap((node,i) => node.rooms.map((room,j) => ({ id:room.id,bed:room.beds[0],status:'CHECKED_IN',plannedMoveIn:'2026-09-03',plannedMoveOut:'2026-12-01',person:{id:room.id,name:'改后姓名'+room.id,department:'改后部门',centerName:j?'工程中心':'法务中心',gender:j?'男':'女',positionName:'岗位'+i,rankName:'T4',category:'己审批长住员工'},promiseSigned:true,remark:j?'普通':'优先' })));
  const fees = nodes.flatMap(node => node.rooms.map((room,j) => ({ id:room.id,batchId:room.id,stayId:room.id,personId:room.id,personName:'锁定姓名'+room.id,department:'锁定部门',roomId:room.id,buildingName:node.building.name,roomNo:room.roomNo,billingMonth:j?'2026-10':'2026-09',occupiedDays:30,chargeableDays:30,occupantCount:1,waterUsage:1,electricUsage:20,freeElectricUsage:10,freeElectricAmount:7,waterAmount:15,electricAmount:7,totalAmount:22,batchStatus:'GENERATED',createdAt:'2026-10-01T00:00:00' })));
  // Historical bill whose stay was deleted still survives and has no invented ledger fields.
  fees.push({...fees[0],id:99,stayId:99,personName:'仅历史结算',billingMonth:'2026-08',batchStatus:'REVERSAL'});
  await page.route('**/api/visitor/**', route => {
   assert.equal(route.request().method(),'GET','filtering must not write data');
   const url = new URL(route.request().url()), path = url.pathname;
   if(path.endsWith('/fees/settlements')){assert.equal(url.searchParams.get('month'),null);assert.equal(url.searchParams.get('person'),null);}
   return route.fulfill({json:path.endsWith('/resources/tree')?{buildings:nodes}:path.endsWith('/stays')?stays:path.endsWith('/people')?stays.map(s=>s.person):path.endsWith('/fees/settlements')?fees:path.endsWith('/statistics')?{summary:{},categories:[],buildings:[],statuses:[]}:[]});
  });
  await page.goto(process.env.SCOPE_URL || 'http://127.0.0.1:5173/visitor/dormitory');
  await page.locator('.dorm-nav button').filter({hasText:'水电报表'}).click();
  const section=page.locator('.fee-section'), rows=section.locator('tbody tr').filter({hasNot:page.locator('.empty-cell')});
  const field=label=>section.locator('.fee-rule-grid label').filter({hasText:label}).locator('input:not([type=checkbox]),select');
  assert.equal(await rows.count(),9);
  assert.equal(await field('开始日期').getAttribute('type'),'date');assert.equal(await field('截止日期').getAttribute('type'),'date');
  await field('开始日期').fill('2026-09-15');assert.equal(await rows.count(),8,'Open-ended start retains September and October bills');
  await field('截止日期').fill('2026-10-05');assert.equal(await rows.count(),8,'Date range covers both boundary billing months');
  await field('开始日期').fill('');await field('截止日期').fill('2026-09-15');assert.equal(await rows.count(),5,'Open-ended end includes prior months');
  await field('开始日期').fill('2026-09-16');assert.equal(await rows.count(),0,'Reversed days in the same month are invalid');
  await field('开始日期').fill('2026-09-15');await field('截止日期').fill('2026-09-15');assert.equal(await rows.count(),4);
  await field('宿舍区域').selectOption('伏龙宿舍');assert.equal(await rows.count(),1);
  await section.locator('summary').click();await section.getByRole('button',{name:'添加筛选条件'}).click();
  const condition=section.locator('.settlement-filter-row').first();
  await condition.locator('select').first().selectOption('中心');await condition.locator('input').fill('法务中心');assert.equal(await rows.count(),1);
  await condition.locator('input').fill('工程中心');assert.equal(await rows.count(),0);
  await condition.locator('input').fill('法务中心');
  await field('查询人员').fill('锁定姓名3');assert.equal(await rows.count(),1);
  const downloadPromise=page.waitForEvent('download');await section.getByRole('button',{name:'导出Excel',exact:true}).click();
  const book=new ExcelJS.Workbook();await book.xlsx.readFile(await (await downloadPromise).path());
  assert.equal(book.worksheets[0].rowCount,2);assert.equal(book.worksheets[0].getRow(2).getCell(3).value,'锁定姓名3');assert.equal(book.worksheets[0].getRow(2).getCell(4).value,'锁定部门');
  await section.getByRole('button',{name:'重置',exact:true}).click();assert.equal(await rows.count(),9);
  for(const name of names){await page.locator('.region-tabs button').filter({hasText:name}).click();assert.equal(await rows.count(),name===names[0]?3:2);}
  await page.locator('.region-tabs button').filter({hasText:'全集团'}).click();
  await field('开始日期').fill('2026-10-05');await field('截止日期').fill('2026-09-15');assert.equal(await rows.count(),0);assert.equal(await section.getByRole('button',{name:'查询',exact:true}).isDisabled(),true);
  await section.getByRole('button',{name:'重置',exact:true}).click();
  await section.getByRole('button',{name:'添加筛选条件'}).click();
  await condition.locator('select').first().selectOption('中心');await condition.locator('select').nth(1).selectOption('empty');assert.equal(await rows.count(),1);assert.match(await rows.innerText(),/仅历史结算/);
  if(process.env.QA_OUTPUT_DIR){require('node:fs').mkdirSync(process.env.QA_OUTPUT_DIR,{recursive:true});await section.screenshot({path:process.env.QA_OUTPUT_DIR+'/settlement-filters.png'});}
  for(const width of [1440,900,390]){await page.setViewportSize({width,height:1000});for(const input of await section.locator('.settlement-search input,.settlement-search select,.settlement-search button').all()){const b=await input.boundingBox(),p=await section.boundingBox();if(b)assert(b.x>=p.x&&b.x+b.width<=p.x+p.width+1,'filter must fit section');}}
  console.log('Four-dormitory month/region/all-ledger filters, locked values, deleted stays, filtered Excel and responsive controls passed');
 } finally { await browser.close(); }
})().catch(error=>{console.error(error);process.exitCode=1;});
