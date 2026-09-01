<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()
const publicPage = computed(() => route.meta.public === true)

const user = computed(() => JSON.parse(sessionStorage.getItem('visitor-user') || '{}'))
const role = computed(() => user.value.role || '')
const roleNames: Record<string,string> = { GUARD:'门卫', DORM_ADMIN:'宿舍管理员', ADMIN:'后台管理员', SYSTEM_ADMIN:'系统管理员' }
const allMenus = [
  ['/dashboard', '首页总览', ['GUARD','DORM_ADMIN','ADMIN','SYSTEM_ADMIN']],
  ['/guard', '门卫工作台', ['GUARD','SYSTEM_ADMIN']],
  ['/dormitory', '宿舍工作台', ['DORM_ADMIN','SYSTEM_ADMIN']],
  ['/registrations', '登记查询', ['ADMIN','SYSTEM_ADMIN']],
  ['/reports', '综合报表', ['ADMIN','SYSTEM_ADMIN']],
  ['/system', '系统管理', ['SYSTEM_ADMIN']],
] as const
const menus = computed(() => allMenus.filter(menu => menu[2].includes(role.value)))
function logout(){sessionStorage.removeItem('visitor-user');sessionStorage.removeItem('visitor-authorization');router.replace('/login')}
</script>

<template>
  <router-view v-if="publicPage" />
  <div v-else class="layout" :class="{ 'dormitory-layout': route.path.startsWith('/dormitory') }">
    <aside>
      <div class="brand"><span class="brand-mark">万</span><div><h1>访客管理平台</h1><p class="brand-subtitle">万盛股份</p></div></div>
      <p class="nav-group-title">业务导航</p>
      <nav>
        <router-link v-for="menu in menus" :key="menu[0]" :to="menu[0]">{{ menu[1] }}</router-link>
      </nav>
      <div class="aside-user"><span>{{user.username}}</span><small>{{roleNames[role]||role}}</small></div>
    </aside>
    <section class="workspace">
      <header class="topbar"><div class="topbar-path"><strong>首页</strong><span>/</span><b>{{roleNames[role]||'内部用户'}}</b></div><div class="topbar-actions"><span class="user-avatar">{{(user.username||'U').slice(0,1).toUpperCase()}}</span><div class="topbar-user"><strong>{{user.username}}</strong><span>{{roleNames[role]||role}}</span></div><button class="logout" @click="logout">退出登录</button></div></header>
      <main><router-view /></main>
    </section>
  </div>
</template>
