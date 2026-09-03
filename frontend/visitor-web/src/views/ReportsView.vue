<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
const guards = ref<any[]>([]),
  dorm = ref<any[]>([]),
  audits = ref<any[]>([]),
  loading = ref(true);
const auth = () => ({ Authorization: sessionStorage.getItem("visitor-authorization") || "" });
async function load() {
  loading.value = true;
  const [g, d, a] = await Promise.all(
    ["/api/visitor/admin/guard-records", "/api/visitor/admin/dormitory-records", "/api/visitor/admin/audit-logs"].map(
      (u) => fetch(u, { headers: auth() }),
    ),
  );
  if (g.ok) guards.value = await g.json();
  if (d.ok) dorm.value = await d.json();
  if (a.ok) audits.value = await a.json();
  loading.value = false;
}
const statCards = computed(() => [
  { label: "进出记录", value: guards.value.length, icon: '<path d="M8 6h13M8 12h13M8 18h13M3 6h.01M3 12h.01M3 18h.01"/>', tone: "blue" },
  { label: "住宿记录", value: dorm.value.length, icon: '<path d="M3 11l9-8 9 8"/><path d="M5 10v10h14V10"/>', tone: "green" },
  { label: "操作审计", value: audits.value.length, icon: '<path d="M3 3v18h18"/><path d="M7 14l4-4 3 3 5-6"/>', tone: "amber" },
]);
const svgAttrs =
  'viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"';
function icon(v: string) {
  return `<svg ${svgAttrs}>${v}</svg>`;
}
onMounted(load);
</script>
<template>
  <header class="page-head">
    <div><p class="eyebrow">REPORTS</p><h2>综合查询与审计</h2><p class="muted">进出记录、住宿记录与操作审计的全局汇总。</p></div>
    <button class="secondary-button" :disabled="loading" @click="load">刷新</button>
  </header>
  <div class="dash-cards report-stats">
    <article v-for="c in statCards" :key="c.label" class="card stat-card dash-stat" :class="['stat-tone', c.tone]">
      <span class="dash-stat-icon" v-html="icon(c.icon)"></span>
      <div class="dash-stat-body"><span>{{ c.label }}</span><strong>{{ c.value }}</strong></div>
    </article>
  </div>
  <section class="panel wide-panel">
    <div class="section-title"><h3>操作审计</h3><span class="muted">{{ audits.length }} 条</span></div>
    <div class="table-wrap">
      <table>
        <thead><tr><th>来访编号</th><th>操作</th><th>操作人</th><th>时间</th></tr></thead>
        <tbody>
          <tr v-if="loading"><td colspan="4" class="empty-cell">正在加载…</td></tr>
          <tr v-else-if="!audits.length"><td colspan="4" class="empty-cell">暂无审计记录</td></tr>
          <tr v-for="(a, i) in audits" :key="i"><td>{{ a.visitId }}</td><td>{{ a.action || "BED_CHANGED" }}</td><td>{{ a.operator }}</td><td>{{ a.operatedAt }}</td></tr>
        </tbody>
      </table>
    </div>
  </section>
</template>
