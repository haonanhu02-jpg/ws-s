<script setup lang="ts">
import { onMounted, ref } from "vue";
const rows = ref<any[]>([]),
  loading = ref(true),
  error = ref("");
async function load() {
  loading.value = true;
  error.value = "";
  try {
    const r = await fetch("/api/visitor/admin/registrations", {
      headers: { Authorization: sessionStorage.getItem("visitor-authorization") || "" },
    });
    if (r.ok) rows.value = await r.json();
    else error.value = "加载失败";
  } catch (e) {
    error.value = e instanceof Error ? e.message : "加载失败";
  } finally {
    loading.value = false;
  }
}
onMounted(load);
</script>
<template>
  <header class="page-head">
    <div><p class="eyebrow">REGISTRATIONS</p><h2>登记查询</h2><p class="muted">查看全部来访登记明细。</p></div>
    <button class="secondary-button" :disabled="loading" @click="load">刷新</button>
  </header>
  <p v-if="error" class="notice-error">{{ error }}</p>
  <section class="panel wide-panel">
    <div class="section-title"><h3>登记记录</h3><span class="muted">{{ rows.length }} 条</span></div>
    <div class="table-wrap">
      <table>
        <thead><tr><th>编号</th><th>姓名</th><th>手机号</th><th>被访部门</th><th>事由</th><th>登记时间</th></tr></thead>
        <tbody>
          <tr v-if="loading"><td colspan="6" class="empty-cell">正在加载…</td></tr>
          <tr v-else-if="!rows.length"><td colspan="6" class="empty-cell">暂无登记记录</td></tr>
          <tr v-for="r in rows" :key="r.visitId"><td><code>{{ r.visitId }}</code></td><td>{{ r.visitorName }}</td><td>{{ r.mobile }}</td><td>{{ r.hostDepartment }}</td><td>{{ r.visitReason }}</td><td>{{ r.registeredAt }}</td></tr>
        </tbody>
      </table>
    </div>
  </section>
</template>
