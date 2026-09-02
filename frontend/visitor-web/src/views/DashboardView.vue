<script setup lang="ts">
import{computed,onMounted,ref}from'vue'
import{dormitoryDashboardStats}from'./dashboardStats'
const user=JSON.parse(sessionStorage.getItem('visitor-user')||'{}'),role=user.role||''
const stats=ref<Record<string,number>>({}),error=ref('')
const allCards=[['todayRegistrations','今日登记'],['waitingEntry','待入厂'],['inFactory','当前在厂'],['exited','今日已离场'],['needsAccommodation','需要住宿'],['pendingBed','待安排床位'],['currentAccommodation','当前住宿']]
const cards=computed(()=>role==='GUARD'?allCards.slice(1,4):role==='DORM_ADMIN'?allCards.slice(4):allCards)
const welcome=computed(()=>role==='GUARD'?'处理访客入厂与离场确认':role==='DORM_ADMIN'?'处理住宿确认与床位安排':role==='ADMIN'?'查看访客全流程数据':'维护系统权限并查看全局数据')
const links=computed(()=>role==='GUARD'?[['/guard','门卫工作台','处理入厂与离场确认']]:role==='DORM_ADMIN'?[['/dormitory','宿舍工作台','住宿确认与床位安排']]:role==='ADMIN'?[['/registrations','登记记录','查看访客登记明细'],['/reports','综合报表','查看数据汇总']]:[['/guard','门卫工作台','处理入厂与离场'],['/dormitory','宿舍工作台','住宿与床位管理'],['/system','系统管理','权限与全局配置']])
const initials=computed(()=>{const n=(user.username||'?').trim();return n?n.slice(0,1).toUpperCase():'?'})
const STAT_ICONS:Record<string,string>={
  todayRegistrations:'<rect x="8" y="3" width="8" height="4" rx="1"/><path d="M9 5H6a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7a2 2 0 0 0-2-2h-3"/><path d="M9 14l2 2 4-4"/>',
  waitingEntry:'<circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 3"/>',
  inFactory:'<path d="M3 21V9l6 4V9l6 4V5l6 4v12z"/>',
  exited:'<path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><path d="M16 17l5-5-5-5"/><path d="M21 12H9"/>',
  needsAccommodation:'<path d="M3 18v-6a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2v6"/><path d="M3 14h18"/><path d="M3 18v2M21 18v2"/><path d="M6 10V8a1 1 0 0 1 1-1h3a1 1 0 0 1 1 1v2"/>',
  pendingBed:'<path d="M6 3h12M6 21h12M8 3v6l4 4 4-4V3M8 21v-6l4-4 4 4v6"/>',
  currentAccommodation:'<path d="M3 11l9-8 9 8"/><path d="M5 10v10h14V10"/>'
}
const LINK_ICONS:Record<string,string>={
  '/guard':'<path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>',
  '/dormitory':'<path d="M3 11l9-8 9 8"/><path d="M5 10v10h14V10"/>',
  '/registrations':'<path d="M8 6h13M8 12h13M8 18h13M3 6h.01M3 12h.01M3 18h.01"/>',
  '/reports':'<path d="M3 3v18h18"/><path d="M7 14l4-4 3 3 5-6"/>',
  '/system':'<path d="M4 6h16M4 12h16M4 18h16"/><circle cx="9" cy="6" r="2"/><circle cx="15" cy="12" r="2"/><circle cx="8" cy="18" r="2"/>'
}
const svgAttrs='viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"'
function iconFor(k:string){return `<svg ${svgAttrs}>`+(STAT_ICONS[k]||'')+'</svg>'}
function linkIcon(p:string){return `<svg ${svgAttrs}>`+(LINK_ICONS[p]||'')+'</svg>'}
onMounted(async()=>{if(!['DORM_ADMIN','ADMIN','SYSTEM_ADMIN'].includes(role))return;const headers={Authorization:sessionStorage.getItem('visitor-authorization')||''};const url=role==='DORM_ADMIN'?'/api/visitor/dormitory/records':'/api/visitor/admin/dashboard';const r=await fetch(url,{headers});if(!r.ok){error.value='统计加载失败';return}const data=await r.json();stats.value=role==='DORM_ADMIN'?dormitoryDashboardStats(data):data})
</script>
<template><header class="page-head dash-hero"><div class="dash-hero-text"><p class="eyebrow">OVERVIEW</p><h2>你好，{{user.username}}</h2><p class="muted">{{welcome}}</p></div><div class="dash-avatar" aria-hidden="true">{{initials}}</div></header><p v-if="error" class="error">{{error}}</p><div class="cards dash-cards"><article v-for="c in cards" :key="c[0]" class="card stat-card dash-stat"><span class="dash-stat-icon" v-html="iconFor(c[0])"></span><div class="dash-stat-body"><span>{{c[1]}}</span><strong>{{stats[c[0]]??'—'}}</strong></div></article></div><section class="quick-panel"><h3>快捷入口</h3><div class="quick-links dash-quick"><router-link v-for="link in links" :key="link[0]" :to="link[0]" class="dash-quick-card"><span class="dash-quick-icon" v-html="linkIcon(link[0])"></span><span class="dash-quick-text"><b>{{link[1]}}</b><small v-if="link[2]">{{link[2]}}</small></span><span class="dash-quick-arrow">→</span></router-link></div></section></template>
