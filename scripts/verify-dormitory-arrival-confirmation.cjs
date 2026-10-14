// Mocked UI regression: an independent per-stay flag, not a new occupancy state.
const assert=require('node:assert/strict');
const {chromium}=require(process.env.PLAYWRIGHT_MODULE||'C:/Users/12778/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/playwright');
(async()=>{
 const browser=await chromium.launch({headless:true,executablePath:process.env.CHROME_PATH||'C:/Program Files/Google/Chrome/Application/chrome.exe'});
 try{
  const page=await browser.newPage({viewport:{width:1280,height:900}});
  await page.clock.install({time:new Date('2026-10-14T12:00:00+08:00')});
  await page.addInitScript(()=>sessionStorage.setItem('visitor-user',JSON.stringify({username:'test',role:'DORM_ADMIN'})));
  const nodes=['盛心公寓','伏龙宿舍','花城宿舍','岙底罗'].map((name,i)=>({building:{id:i+1,name,enabled:true,displayOrder:i},rooms:[{id:i+1,buildingId:i+1,roomNo:['317','210','主卧','206'][i],floorNo:i?2:3,roomType:'单间',enabled:true,livable:true,cleaningRequired:false,beds:[{id:i+1,roomId:i+1,bedCode:'BED-'+i,label:'单床',enabled:true,cleaningRequired:false}]}]}));
  const stays=nodes.flatMap((node,i)=>[0,1].map(j=>({id:i*2+j+1,version:0,arrivalConfirmed:false,bed:node.rooms[0].beds[0],status:'BOOKED',plannedMoveIn:j?'2026-11-01':'2026-10-19',plannedMoveOut:j?'2026-11-10':'2026-10-25',person:{id:1,name:j?'后续人员'+i:'同名人员',centerName:'中心',department:'部门',gender:'男',category:'己审批长住员工'},bedType:'长住房',costCut:false,cleaningRequired:false})));
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
  const open=async()=>{await nav('可视化平面图').click();await page.locator('.fp-beds button').first().click();};
  await page.goto(process.env.SCOPE_URL||'http://127.0.0.1:5173/visitor/dormitory');
  for(const [i,node]of nodes.entries()){
   await page.locator('.region-tabs button').filter({hasText:node.building.name}).click();
   await nav('预警看板').click();assert.equal(await warning.locator('p:not(.muted)').count(),2);
   await open();
   const checkbox=page.getByRole('checkbox',{name:'同名人员是否入住',exact:true});
   assert.equal(await checkbox.isChecked(),false);
   const colour=await page.locator('.booking-schedule button.active').evaluate(el=>getComputedStyle(el).backgroundColor);
   const nameInput=page.locator('.booking-grid label').filter({hasText:/^姓名/}).locator('input');await nameInput.fill('未保存草稿');
   await checkbox.check();await page.waitForFunction(()=>!document.querySelector('.arrival-confirmation input:disabled'));
   assert.equal(await nameInput.inputValue(),'未保存草稿','checkbox must not submit or discard draft fields');
   assert.equal(await page.locator('.booking-schedule button.active').evaluate(el=>getComputedStyle(el).backgroundColor),colour);
   if(i===0&&process.env.QA_OUTPUT_DIR){require('node:fs').mkdirSync(process.env.QA_OUTPUT_DIR,{recursive:true});await page.locator('.booking-schedule').screenshot({path:process.env.QA_OUTPUT_DIR+'/arrival-confirmation.png'});}
   await close();await nav('预警看板').click();assert.equal(await warning.locator('p:not(.muted)').count(),1);assert.match(await warning.innerText(),new RegExp('后续人员'+i));
   await page.reload();await page.locator('.region-tabs button').filter({hasText:node.building.name}).click();await open();
   assert.equal(await checkbox.isChecked(),true,'flag survives reload');
   failNext=true;await checkbox.click();await page.getByRole('alert').filter({hasText:'测试并发冲突'}).waitFor();assert.equal(await checkbox.isChecked(),true,'failed request must restore saved tick');
   await checkbox.uncheck();await page.waitForFunction(()=>!document.querySelector('.arrival-confirmation input:disabled'));
   await close();await nav('预警看板').click();assert.equal(await warning.locator('p:not(.muted)').count(),2);
   assert.deepEqual(stays.map(({version,arrivalConfirmed,...rest})=>rest),original.map(({version,arrivalConfirmed,...rest})=>rest),'all original fields/statuses/dates and other stays remain unchanged');
  }
  assert.equal(writes.length,8);
  await page.clock.fastForward(6*86400000);await nav('预警看板').click();assert.equal(await warning.locator('p:not(.muted)').count(),1,'original date-based expiry of booking alerts remains intact');
  console.log('Four-dormitory arrival ticks persist/reverse per stay, restore on errors, preserve drafts, dates, colours and original alert rules');
 }finally{await browser.close();}
})().catch(e=>{console.error(e);process.exitCode=1;});
