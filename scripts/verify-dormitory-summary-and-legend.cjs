// Read-only browser regression with mocked resources, stays and Excel export.
const assert = require('node:assert/strict');
const ExcelJS = require('../frontend/visitor-web/node_modules/exceljs');
const {chromium} = require(process.env.PLAYWRIGHT_MODULE || 'C:/Users/12778/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/playwright');
(async()=>{
 const browser=await chromium.launch({headless:true,executablePath:process.env.CHROME_PATH || 'C:/Program Files/Google/Chrome/Application/chrome.exe'});
 try {
  const page=await browser.newPage({viewport:{width:1440,height:900}});
  await page.clock.install({time:new Date('2026-10-10T12:00:00+08:00')});
  await page.addInitScript(()=>sessionStorage.setItem('visitor-user',JSON.stringify({username:'test',role:'DORM_ADMIN'})));
  const stays=[];
  const nodes=['盛心公寓','伏龙宿舍','花城宿舍','岙底罗'].map((name,i)=>({building:{id:i+1,name,enabled:true,displayOrder:i},rooms:['live','disabled','public','free','book','clean'].map((state,j)=>{
   const id=i*10+j+1,bed={id,roomId:id,label:'单床',bedCode:name+'-'+j,enabled:true,cleaningRequired:state==='clean'};
   if(['live','book'].includes(state))stays.push({id,bed,person:{id,name:'人员'+id,gender:'男',department:'部门'},status:'BOOKED',plannedMoveIn:state==='live'?'2026-10-01':'2026-11-01',costCut:false,cleaningRequired:false});
   return {id,buildingId:i+1,roomNo:['210','209','208','207','206','205'][j],floorNo:2,roomType:'单间',enabled:state!=='disabled',livable:state!=='public',cleaningRequired:state==='clean',beds:[bed]};
  })}));
  for(const [index,node]of nodes.entries())for(const [offset,status]of ['CHECKED_IN','BOOKED'].entries()){
   const id=index*10+7+offset,bed={id,roomId:id,label:'单床',bedCode:node.building.name+'-other-'+offset,enabled:true,cleaningRequired:false};
   node.rooms.push({id,buildingId:node.building.id,roomNo:String(201+offset),floorNo:2,roomType:'单间',enabled:true,livable:true,cleaningRequired:false,beds:[bed]});
   stays.push({id,bed,person:{id,name:'未知性别'+id,gender:offset?'未填写':'',department:'部门'},status,plannedMoveIn:offset?'2026-11-01':'2026-10-01',costCut:false,cleaningRequired:false});
  }
  await page.route('**/api/visitor/**',route=>{
   assert.equal(route.request().method(),'GET','diagnostic must not write business data');
   const path=new URL(route.request().url()).pathname;
   return route.fulfill({json:path.endsWith('/resources/tree')?{buildings:nodes}:path.endsWith('/stays')?stays:path.endsWith('/people')?stays.map(s=>s.person):path.endsWith('/statistics')?{summary:{},buildings:[],categories:[],statuses:[]}:[]});
  });
  await page.goto(process.env.SCOPE_URL || 'http://127.0.0.1:5173/visitor/dormitory');
  await page.locator('.dorm-nav button').filter({hasText:'统计总览'}).click();
  assert.equal(await page.locator('.capacity-table').count(),4);
  if(process.env.QA_OUTPUT_DIR){require('node:fs').mkdirSync(process.env.QA_OUTPUT_DIR,{recursive:true});await page.screenshot({path:process.env.QA_OUTPUT_DIR+'/statistics.png',fullPage:true});}
  for(const table of await page.locator('.capacity-table').all()){
   assert.equal(await table.locator('thead th').last().innerText(),'总计');
   for(const row of await table.locator('tbody tr').all()){
    const values=(await row.locator('td').allTextContents()).slice(1).map(Number);
    assert.equal(values.at(-1),values.slice(0,-1).reduce((a,b)=>a+b,0));
   }
   assert.equal(await table.locator('tbody tr.summary-total td').last().innerText(),'6');
   const cells=await table.locator('tbody tr.summary-total td').allTextContents();
   assert.deepEqual(cells.slice(-4),['1','1','1','6'],'Unknown occupied/booked genders and cleaning bed remain in total');
  }
  for(const node of nodes){
   await page.locator('.region-tabs button').filter({hasText:node.building.name}).click();
   assert.equal(await page.locator('.stat-body').filter({hasText:'床位总数'}).locator('b').innerText(),'6');
   assert.equal(await page.locator('.capacity-table .summary-total td').last().innerText(),'6');
  }
  await page.locator('.region-tabs button').filter({hasText:'全集团'}).click();
  const downloadPromise=page.waitForEvent('download');
  await page.getByRole('button',{name:'导出统计总览',exact:true}).click();
  const download=await downloadPromise,workbook=new ExcelJS.Workbook();await workbook.xlsx.readFile(await download.path());
  const sheet=workbook.worksheets[0],headers=sheet.getRow(1).values;assert.equal(headers.at(-1),'总计');
  assert.ok(headers.includes('已入住其他'));assert.ok(headers.includes('已预订其他'));assert.ok(headers.includes('待打扫'));
  for(let r=2;r<=sheet.rowCount;r++){
   const row=sheet.getRow(r),total=row.getCell(sheet.columnCount).value;
   assert.equal(total,row.getCell(2).value==='标间'?0:6);
   assert.equal(total,row.values.slice(3,13).reduce((sum,value)=>sum+Number(value),0),'Excel categories reconcile with capacity');
  }
  await page.locator('.dorm-nav button').filter({hasText:'可视化平面图'}).click();
  await page.locator('.region-tabs button').filter({hasText:'盛心公寓'}).click();
  const originalRoom=no=>page.locator('.fp-room').filter({has:page.locator('header strong',{hasText:no})});
  assert.equal(await page.locator('.fp-sw.live').evaluate(el=>getComputedStyle(el).backgroundColor),'rgb(227, 242, 253)','Shengxin legend remains unchanged');
  assert.equal(await page.locator('.fp-sw.clean').evaluate(el=>getComputedStyle(el).backgroundColor),'rgb(216, 58, 58)','Shengxin cleaning legend remains unchanged');
  assert.equal(await page.locator('.fp-sw.unavailable').count(),0,'Shengxin original legend has no additional state');
  assert.equal(await originalRoom('209').evaluate(el=>getComputedStyle(el).backgroundColor),'rgb(243, 229, 245)','Shengxin original disabled-room presentation remains unchanged');
  assert.equal(await originalRoom('210').locator('.fp-beds button').evaluate(el=>getComputedStyle(el).backgroundColor),'rgb(219, 234, 254)');
  await page.locator('.region-tabs button').filter({hasText:'全集团'}).click();
  assert.deepEqual(await page.locator('.fp-building').evaluateAll(elements=>elements.map(el=>el.classList.contains('fp-adapted'))),[false,true,true,true],'Only the other three dormitories receive adapted styles');
  assert.equal(await page.locator('.fp-building').first().locator('.fp-room').filter({has:page.locator('header strong',{hasText:'209'})}).evaluate(el=>getComputedStyle(el).backgroundColor),'rgb(243, 229, 245)','Group view also preserves Shengxin');
  await page.locator('.region-tabs button').filter({hasText:'伏龙宿舍'}).click();
  assert.match(await page.locator('.fp-legend').innerText(),/停用/);
  const colour=locator=>locator.evaluate(el=>getComputedStyle(el).backgroundColor);
  const room=no=>page.locator('.fp-room').filter({has:page.locator('header strong',{hasText:no})});
  const legend=state=>colour(page.locator('.fp-sw.'+state));
  for(const [no,state]of [['210','live'],['207','ok'],['206','book'],['205','clean']])assert.equal(await colour(room(no).locator('.fp-beds button')),await legend(state),no);
  assert.equal(await colour(room('209')),await legend('unavailable'));
  assert.equal(await colour(room('209').locator('.fp-beds button')),await legend('unavailable'));
  assert.equal(await colour(room('208')),await legend('public'));
  assert.equal(await colour(room('208').locator('.fp-beds button')),await legend('public'));
  assert.equal(await colour(page.locator('.fp-wet-area').first()),await legend('public'));
  assert.equal(await colour(page.locator('.fp-stair').first()),await legend('public'));
  assert.equal(await colour(page.locator('.fp-drying-area').first()),await legend('public'));
  if(process.env.QA_OUTPUT_DIR)await page.screenshot({path:process.env.QA_OUTPUT_DIR+'/fulong.png',fullPage:true});
  console.log('Shengxin original styles preserved in single/group views; four-dormitory totals/export retained; other dormitories receive scoped palette');
 }finally{await browser.close();}
})().catch(e=>{console.error(e);process.exitCode=1;});
