<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Coin, Odometer, Timer, Warning } from '@element-plus/icons-vue'
import StatCard from '@/views/dashboard/components/StatCard.vue'
import { getMetricsLatest, getMetricsList, getMetricsUris } from '@/api/metrics'
import './metrics.css'

const loading = ref(false)
const failed = ref(false)
const latest = ref(null)
const history = ref([])
const hotUris = ref([])

const global = computed(() => latest.value?.global || {})

const qpsText = computed(() => formatQps(global.value.qps))
const avgText = computed(() => formatMs(global.value.avgCostMs))
const errorText = computed(() => {
  const count = Number(global.value.requestCount || 0)
  const errors = Number(global.value.error4xx || 0) + Number(global.value.error5xx || 0)
  if (!count) return '0%'
  return `${((errors / count) * 100).toFixed(2)}%`
})
const dbText = computed(() => {
  if (global.value.dbActive == null && global.value.dbMax == null) return '--'
  return `${global.value.dbActive ?? '--'} / ${global.value.dbMax ?? '--'}`
})

function formatQps(value) {
  if (value == null || value === '') return '--'
  return Number(value).toFixed(2)
}

function formatMs(value) {
  if (value == null || value === '') return '--'
  return `${value} ms`
}

function errorRate(row) {
  const count = Number(row.requestCount || 0)
  const errors = Number(row.error4xx || 0) + Number(row.error5xx || 0)
  if (!count) return '0%'
  return `${((errors / count) * 100).toFixed(2)}%`
}

async function loadAll() {
  loading.value = true
  failed.value = false
  try {
    const [latestRes, listRes, uriRes] = await Promise.all([
      getMetricsLatest(),
      getMetricsList(),
      getMetricsUris(),
    ])
    latest.value = latestRes.data || null
    history.value = listRes.data || []
    hotUris.value = uriRes.data || []
  } catch (error) {
    failed.value = true
    ElMessage.error(error.message || '访问指标加载失败')
  } finally {
    loading.value = false
  }
}

onMounted(loadAll)
</script>

<template>
  <div class="metrics-view">
    <div class="metrics-view__stats">
      <StatCard label="当前 QPS" :value="qpsText" :icon="Odometer" tone="blue" />
      <StatCard label="平均延迟" :value="avgText" :icon="Timer" tone="green" />
      <StatCard label="错误率" :value="errorText" :icon="Warning" tone="red" />
      <StatCard label="DB 活跃连接" :value="dbText" :icon="Coin" tone="orange" />
    </div>

    <div v-if="failed && !loading" class="metrics-view__error page-card">
      <span>访问指标暂时无法加载。</span>
      <ElButton text type="primary" @click="loadAll">重新加载</ElButton>
    </div>

    <section v-else class="page-card metrics-card">
      <header class="metrics-card__header">
        <h2>按分钟历史（近 24 小时）</h2>
        <span v-if="global.jvmUsedMb != null">JVM {{ global.jvmUsedMb }} / {{ global.jvmMaxMb }} MB</span>
      </header>
      <ElTable v-loading="loading" class="metrics-table" :data="history" size="small" empty-text="暂无采样数据，约 1 分钟后出现第一行">
        <ElTableColumn prop="snapshotTime" label="时间" min-width="170" />
        <ElTableColumn label="QPS" min-width="90">
          <template #default="{ row }">{{ formatQps(row.qps) }}</template>
        </ElTableColumn>
        <ElTableColumn prop="requestCount" label="请求数" min-width="90" />
        <ElTableColumn prop="avgCostMs" label="平均 ms" min-width="90" />
        <ElTableColumn prop="maxCostMs" label="最大 ms" min-width="90" />
        <ElTableColumn prop="error4xx" label="4xx" min-width="70" />
        <ElTableColumn prop="error5xx" label="5xx" min-width="70" />
        <ElTableColumn prop="inFlightMax" label="最大并发" min-width="90" />
        <ElTableColumn label="DB" min-width="90">
          <template #default="{ row }">{{ row.dbActive ?? '--' }}/{{ row.dbMax ?? '--' }}</template>
        </ElTableColumn>
      </ElTable>
    </section>

    <section class="page-card metrics-card">
      <header class="metrics-card__header"><h2>热点 URI（近 24 小时）</h2></header>
      <ElTable v-loading="loading" class="metrics-table" :data="hotUris" size="small" empty-text="暂无热点 URI">
        <ElTableColumn prop="method" label="方法" min-width="80" />
        <ElTableColumn prop="uri" label="路径" min-width="280" />
        <ElTableColumn prop="requestCount" label="请求数" min-width="90" />
        <ElTableColumn label="QPS" min-width="90">
          <template #default="{ row }">{{ formatQps(row.qps) }}</template>
        </ElTableColumn>
        <ElTableColumn prop="avgCostMs" label="平均 ms" min-width="90" />
        <ElTableColumn prop="maxCostMs" label="最大 ms" min-width="90" />
        <ElTableColumn label="错误率" min-width="90">
          <template #default="{ row }">{{ errorRate(row) }}</template>
        </ElTableColumn>
      </ElTable>
    </section>
  </div>
</template>
