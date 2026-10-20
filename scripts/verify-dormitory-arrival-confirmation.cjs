// Manual arrival is the shared presentation source for colour, alerts and counts.
const assert=require('node:assert/strict');
const ExcelJS=require('../frontend/visitor-web/node_modules/exceljs');
const {chromium}=require(process.env.PLAYWRIGHT_MODULE||'C:/Users/12778/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/playwright');
(async()=>{
 const browser=await chromium.launch({headless:true,executablePath:process.env.CHROME_PATH||'C:/Program Files/Google/Chrome/Application/chrome.exe'});
 try{
  const page=await browser.newPage({viewport:{width:1280,height:900}});
  await page.clock.install({time:new Date('2026-10-20T12:00:00+08:00')});
  await page.addInitScript(()=>sessionStorage.setItem('visitor-user',JSON.stringify({username:'test',role:'DORM_ADMIN'})));
  const nodes=['盛心公寓','伏龙宿舍','花城宿舍','岙底罗'].map((name,i)=>({building:{id:i+1,name,enabled:true,displayOrder:i},rooms:[{id:i+1,buildingId:i+1,roomNo:['317','210','主卧','206'][i],floorNo:i?2:3,roomType:'单间',enabled:true,livable:true,cleaningRequired:false,beds:[{id:i+1,roomId:i+1,bedCode:'BED-'+i,label:'单床',enabled:true,cleaningRequired:false}]}]}));
  const stays=nodes.flatMap((node,i)=>[0,1].map(j=>({id:i*2+j+1,version:0,arrivalConfirmed:false,bed:node.rooms[0].beds[0],status:i%2?'CHECKED_IN':'BOOKED',plannedMoveIn:j?'2026-11-01':'2026-10-20',plannedMoveOut:j?'2026-11-10':'2026-10-23',person:{id:1,name:j?'后续人员'+i:'同名人员',centerName:'中心',department:'部门',gender:'男',category:'己审批长住员工'},bedType:'长住房',costCut:false,cleaningRequired:false})));
  const original=JSON.parse(JSON.stringify(stays)),writes=[];let failNext=false;
  await page.route('**/api/visitor/**',route=>{
   const req=route.request(),path=new URL(req.url()).pathname;
   if(req.method()!=='GET'){
    assert.equal(req.method(),'PUT');assert.match(path,/\/stays\/\d+\/arrival-confirmation$/);
    const command=req.postDataJSON();assert.deepEqual(Object.keys(command).sort(),['confirmed','version']);
    const stay=stays.find(s=>s.id===Number(path.split('/').at(-2)));assert.equal(command.version,stay.version);
    if(failNext){failNext=false;return route.fulfill({status:409,json:{detail:'测试并发冲突，请刷新后重试'}});}
    writes.push({id:stay.id,...command});stay.arrivalConfirmed=command.confirmed;stay.version++;
    return route.fulfill({json:stay});
   }
   return route.fulfill({json:path.endsWith('/resources/tree')?{buildings:nodes}:path.endsWith('/stays')?stays:path.endsWith('/people')?[stays[0].person]:path.endsWith('/statistics')?{summary:{},buildings:[],categories:[],statuses:[]}:[]});
  });
  const nav=label=>page.locator('.dorm-nav button').filter({hasText:label});
  const warning=page.locator('.warning-columns article').filter({has:page.locator('h3',{hasText:'预定房间'})});
  const close=()=>page.locator('.dorm-booking .drawer-close').click();
  const colourOf=locator=>locator.evaluate(el=>{for(const animation of el.getAnimations())animation.finish();return getComputedStyle(el).backgroundColor;});
  const open=async()=>{await nav('可视化平面图').click();await page.locator('.fp-beds button').first().click();};
  await page.goto(process.env.SCOPE_URL||'http://127.0.0.1:5173/visitor/dormitory');
  for(const [i,node]of nodes.entries()){
   await page.locator('.region-tabs button').filter({hasText:node.building.name}).click();
   await nav('预警看板').click();assert.equal(await warning.locator('p:not(.muted)').count(),2);
   await open();
   const checkbox=page.getByRole('checkbox',{name:'同名人员是否入住',exact:true});
   assert.equal(await checkbox.isChecked(),false);
   const colour=await colourOf(page.locator('.booking-schedule button.active'));
   assert.equal(colour,'rgb(255, 248, 225)','Today without an arrival tick must be yellow, even for saved CHECKED_IN status');
   const nameInput=page.locator('.booking-grid label').filter({hasText:/^姓名/}).locator('input');await nameInput.fill('未保存草稿');
   await checkbox.check();await page.waitForFunction(()=>!document.querySelector('.arrival-confirmation input:disabled'));
   assert.ok(stays.filter(stay=>stay.id!==i*2+1).every(stay=>stay.arrivalConfirmed===false),'same-name guests and later reservations must not be checked together');
   assert.equal(await nameInput.inputValue(),'未保存草稿','checkbox must not submit or discard draft fields');
   assert.equal(await colourOf(page.locator('.booking-schedule button.active')),'rgb(219, 234, 254)','Tick immediately changes editor card to blue');
   if(i===0&&process.env.QA_OUTPUT_DIR){require('node:fs').mkdirSync(process.env.QA_OUTPUT_DIR,{recursive:true});await page.locator('.booking-schedule').screenshot({path:process.env.QA_OUTPUT_DIR+'/arrival-confirmation.png'});}
   await close();assert.equal(await colourOf(page.locator('.fp-beds button').first()),'rgb(219, 234, 254)','Tick immediately changes bed to blue');
   await nav('统计总览').click();assert.equal(await page.locator('.stat-body').filter({hasText:'总入住率'}).locator('b').innerText(),'100.0%');
   await nav('入住台账').click();assert.match(await page.locator('tbody tr').filter({hasText:'同名人员'}).first().locator('.ledger-status').innerText(),/已入住/);
   if(i===0){
    const downloaded=page.waitForEvent('download');await page.getByRole('button',{name:'导出全部历史明细',exact:true}).click();
    const workbook=new ExcelJS.Workbook();await workbook.xlsx.readFile(await (await downloaded).path());
    const sheet=workbook.worksheets[0],headers=sheet.getRow(1).values;
    assert.equal(sheet.getRow(2).getCell(headers.indexOf('当前状态')).value,'已入住');
    assert.equal(sheet.getRow(2).getCell(headers.indexOf('计划入住')).value,'2026-10-20');
   }
   await nav('预警看板').click();assert.equal(await warning.locator('p:not(.muted)').count(),1);assert.match(await warning.innerText(),new RegExp('后续人员'+i));
   await page.reload();await page.locator('.region-tabs button').filter({hasText:node.building.name}).click();await open();
   assert.equal(await checkbox.isChecked(),true,'flag survives reload');
   failNext=true;await checkbox.click();await page.getByRole('alert').filter({hasText:'测试并发冲突'}).waitFor();assert.equal(await checkbox.isChecked(),true,'failed request must restore saved tick');
   await checkbox.uncheck();await page.waitForFunction(()=>!document.querySelector('.arrival-confirmation input:disabled'));
   await close();assert.equal(await colourOf(page.locator('.fp-beds button').first()),'rgb(255, 248, 225)','Untick restores yellow');
   await nav('统计总览').click();assert.equal(await page.locator('.stat-body').filter({hasText:'总入住率'}).locator('b').innerText(),'0.0%');
   await nav('入住台账').click();assert.match(await page.locator('tbody tr').filter({hasText:'同名人员'}).first().locator('.ledger-status').innerText(),/已预定/);
   await nav('预警看板').click();assert.equal(await warning.locator('p:not(.muted)').count(),2);
   assert.deepEqual(stays.map(({version,arrivalConfirmed,...rest})=>rest),original.map(({version,arrivalConfirmed,...rest})=>rest),'all original fields/statuses/dates and other stays remain unchanged');
  }
  assert.equal(writes.length,8);
  await page.clock.fastForward(6*86400000);await nav('预警看板').click();assert.equal(await warning.locator('p:not(.muted)').count(),2,'Date advancement never assumes an unchecked guest has arrived');
  await open();assert.equal(await page.locator('.booking-schedule button.active').evaluate(el=>getComputedStyle(el).backgroundColor),'rgb(255, 248, 225)');
  console.log('Four-dormitory manual arrival synchronizes yellow/blue cards and beds, ledger, occupancy and alerts; preserves records and survives dates/reload/errors');
 }finally{await browser.close();}
})().catch(e=>{console.error(e);process.exitCode=1;});
