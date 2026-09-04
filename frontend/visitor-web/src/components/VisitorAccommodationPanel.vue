<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { visitorAccommodationApi, type VisitorAccommodation } from '../api/visitorAccommodation'

const records = ref<VisitorAccommodation[]>([])
const keyword = ref(''), error = ref(''), busy = ref(''), updatedAt = ref('')
const loading = ref(false)
let timer: ReturnType<typeof setInterval> | undefined
let disposed = false
const rows = computed(() => records.value.filter(r => r.accommodationRequired &&
  [r.visitorName, r.mobile, r.hostName, r.visitId].some(value => value.includes(keyword.value.trim()))))
async function load() {
  if (loading.value) return
  loading.value = true
  try {
    const result = await visitorAccommodationApi.list()
    if (disposed) return
    records.value = result
    error.value = ''
    updatedAt.value = new Date().toLocaleTimeString('zh-CN')
  } catch (e) { if (!disposed) error.value = e instanceof Error ? e.message : '访客住宿申请加载失败' }
  finally { loading.value = false }
}
async function confirm(record: VisitorAccommodation) {
  if (!window.confirm(`确认 ${record.visitorName} 的住宿申请？确认后门卫不能取消。`)) return
  busy.value = record.visitId
  try {
    const result = await visitorAccommodationApi.confirm(record.visitId)
    records.value = records.value.map(r => r.visitId === result.visitId ? result : r)
    error.value = ''
  } catch (e) { error.value = e instanceof Error ? e.message : '确认失败，请刷新记录' }
  finally { busy.value = '' }
}
onMounted(() => { void load(); timer = setInterval(() => { if (!document.hidden && !busy.value) void load() }, 5000) })
onUnmounted(() => { disposed = true; if (timer) clearInterval(timer) })
</script>

<template>
  <section class="visitor-accommodation">
    <header class="visitor-accommodation-head">
      <div><h2>访客住宿申请</h2><p>扫码登记或门卫手工登记中选择“需要住宿”的访客在这里显示，无需等待门卫确认入厂。</p></div>
      <button type="button" :disabled="loading" @click="load">{{ loading ? '刷新中…' : '刷新申请' }}</button>
    </header>
    <div class="visitor-accommodation-toolbar">
      <label><span>搜索访客</span><input v-model="keyword" type="search" placeholder="姓名、手机号、被访人或编号" /></label>
      <span>共 {{ rows.length }} 条 · 每 5 秒刷新<span v-if="updatedAt"> · 最近更新 {{ updatedAt }}</span></span>
    </div>
    <p v-if="error" class="notice-error" role="alert">{{ error }}（已有列表可能不是最新数据）</p>
    <div class="table-wrap"><table>
      <thead><tr><th>访客</th><th>联系方式</th><th>被访人 / 部门</th><th>来访事由</th><th>住宿状态</th><th>操作</th></tr></thead>
      <tbody>
        <tr v-if="!rows.length"><td colspan="6">{{ loading ? '正在加载…' : error ? '暂时无法获取申请' : '暂无需要住宿的访客申请' }}</td></tr>
        <tr v-for="r in rows" :key="r.visitId">
          <td>{{ r.visitorName }}<small>{{ r.visitId }}</small></td><td>{{ r.mobile }}</td>
          <td>{{ r.hostName }}<small>{{ r.hostDepartment }}</small></td><td>{{ r.visitReason }}</td>
          <td>{{ r.bedCode ? `已分床 · ${r.bedCode}` : r.accommodationConfirmed ? '已确认 · 待安排床位' : '待确认住宿' }}</td>
          <td><button v-if="!r.accommodationConfirmed" type="button" :disabled="!!busy || !!error" @click="confirm(r)">{{ busy === r.visitId ? '确认中…' : '确认住宿' }}</button><span v-else>由宿舍管理员处理</span></td>
        </tr>
      </tbody>
    </table></div>
    <p class="visitor-accommodation-note">此处为访客申请，不自动创建员工入住台账；已确认或分配床位的申请不会因门卫取消而释放床位。</p>
  </section>
</template>

<style scoped>
.visitor-accommodation{background:#fff;border:1px solid #e2e8f0;border-radius:14px;padding:24px;color:#334155}
.visitor-accommodation-head,.visitor-accommodation-toolbar{display:flex;align-items:center;justify-content:space-between;gap:20px;margin-bottom:24px}
h2{margin:0 0 10px;color:#17233b}p{line-height:1.7;margin:0;font-size:14px;color:#64748b}
button{width:auto;margin:0;white-space:nowrap;border-radius:8px;padding:10px 16px;background:#315af2;color:white;border:0}
button:disabled{opacity:.5;cursor:wait}
.visitor-accommodation-toolbar label{display:flex;align-items:center;gap:12px;margin:0;flex:1}.visitor-accommodation-toolbar input{width:min(100%,380px);margin:0;min-height:42px}
.visitor-accommodation-toolbar>span{font-size:12px;color:#64748b}small{display:block;font-size:12px;color:#64748b;margin-top:6px;overflow-wrap:anywhere}
th,td{padding:16px;text-align:left}tbody tr:nth-child(even){background:#f8fafc}thead{background:#f1f5f9}
.visitor-accommodation-note{margin-top:18px}
@media(max-width:720px){.visitor-accommodation{padding:16px}.visitor-accommodation-head,.visitor-accommodation-toolbar{align-items:stretch;flex-direction:column}.visitor-accommodation-toolbar label{flex-direction:column;align-items:stretch}}
</style>
