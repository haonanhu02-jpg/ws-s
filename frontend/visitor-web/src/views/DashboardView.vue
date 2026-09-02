<script setup lang="ts">
import{computed,onMounted,ref}from'vue'
import{dormitoryDashboardStats}from'./dashboardStats'
const user=JSON.parse(sessionStorage.getItem('visitor-user')||'{}'),role=user.role||''
const stats=ref<Record<string,number>>({}),error=ref('')
const allCards=[['todayRegistrations','今日登记'],['waitingEntry','待入厂'],['inFactory','当前在厂'],['exited','今日已离场'],['needsAccommodation','需要住宿'],['pendingBed','待安排床位'],['currentAccommodation','当前住宿']]
const cards=computed(()=>role==='GUARD'?allCards.slice(1,4):role==='DORM_ADMIN'?allCards.slice(4):allCards)
const welcome=computed(()=>role==='GUARD'?'处理访客入厂与离场确认':role==='DORM_ADMIN'?'处理住宿确认与床位安排':role==='ADMIN'?'查看访客全流程数据':'维护系统权限并查看全局数据')
const links=computed(()=>role==='GUARD'?[['/guard','进入门卫工作台']]:role==='DORM_ADMIN'?[['/dormitory','进入宿舍工作台']]:role==='ADMIN'?[['/registrations','查看登记记录'],['/reports','查看综合报表']]:[['/guard','门卫工作台'],['/dormitory','宿舍工作台'],['/system','系统管理']])
onMounted(async()=>{if(!['DORM_ADMIN','ADMIN','SYSTEM_ADMIN'].includes(role))return;const headers={Authorization:sessionStorage.getItem('visitor-authorization')||''};const url=role==='DORM_ADMIN'?'/api/visitor/dormitory/records':'/api/visitor/admin/dashboard';const r=await fetch(url,{headers});if(!r.ok){error.value='统计加载失败';return}const data=await r.json();stats.value=role==='DORM_ADMIN'?dormitoryDashboardStats(data):data})
</script>
<template><header class="page-head"><div><p class="eyebrow">OVERVIEW</p><h2>你好，{{user.username}}</h2><p class="muted">{{welcome}}</p></div></header><p v-if="error" class="error">{{error}}</p><div class="cards"><article v-for="c in cards" :key="c[0]" class="card stat-card"><span>{{c[1]}}</span><strong>{{stats[c[0]]??'—'}}</strong></article></div><section class="quick-panel"><h3>快捷入口</h3><div class="quick-links"><router-link v-for="link in links" :key="link[0]" :to="link[0]">{{link[1]}} <span>→</span></router-link></div></section></template>
