<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const username = ref('')
const password = ref('')
const error = ref('')
const pending = ref(false)
const route = useRoute()
const router = useRouter()

async function login() {
  pending.value = true
  error.value = ''
  try {
    const response = await fetch('/api/visitor/auth/login', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ username: username.value, password: password.value }) })
    if (!response.ok) throw new Error('用户名或密码错误')
    const session = await response.json()
    const authorization = `${session.tokenType} ${session.accessToken}`
    const user = { username: session.username, role: session.role }
    sessionStorage.setItem('visitor-user', JSON.stringify(user))
    sessionStorage.setItem('visitor-authorization', authorization)
    await router.replace(String(route.query.redirect || '/dashboard'))
  } catch (reason) {
    error.value = reason instanceof Error ? reason.message : '登录失败'
  } finally {
    pending.value = false
  }
}
</script>

<template>
  <div class="center-page">
    <form class="panel login-panel" @submit.prevent="login">
      <div class="login-brand"><span class="brand-mark">万</span><div><strong>万盛股份</strong><small>WANSHENG</small></div></div>
      <h1>访客管理平台</h1>
      <p class="login-subtitle">内部管理系统</p>
      <label><span>账号</span><input v-model="username" autocomplete="username" required placeholder="请输入账号" /></label>
      <label><span>密码</span><input v-model="password" type="password" autocomplete="current-password" required placeholder="请输入密码" /></label>
      <p v-if="error" class="error">{{ error }}</p>
      <button :disabled="pending">{{ pending ? '登录中…' : '登录' }}</button>
      <router-link class="visitor-entry-link" to="/qr">访客扫码登记入口</router-link>
    </form>
  </div>
</template>
