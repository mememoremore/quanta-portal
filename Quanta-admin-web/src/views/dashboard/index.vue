<script setup>
import { onMounted, ref } from 'vue'
import { storeToRefs } from 'pinia'
import { ElMessage } from 'element-plus'
import { Document, Money, UserFilled } from '@element-plus/icons-vue'
import { getDashboardStats } from '@/api/dashboard'
import { useUserStore } from '@/stores/user'
import WelcomeBanner from './components/WelcomeBanner.vue'
import StatCard from './components/StatCard.vue'

const userStore = useUserStore()
const { displayName } = storeToRefs(userStore)
const loading = ref(true)
const failed = ref(false)
const stats = ref({
  members: null,
  resumesToday: null,
  pendingPayments: null,
})

async function loadStats() {
  loading.value = true
  failed.value = false
  try {
    const response = await getDashboardStats()
    stats.value = { ...stats.value, ...(response.data || {}) }
  } catch (error) {
    failed.value = true
    ElMessage.error(error.message || '控制台数据加载失败')
  } finally {
    loading.value = false
  }
}

onMounted(loadStats)
</script>

<template>
  <div class="dashboard-view">
    <header class="dashboard-view__heading"><p>QUANTA ADMIN</p><h1>控制台</h1></header>
    <WelcomeBanner :name="displayName" />

    <div v-loading="loading" class="dashboard-view__stats">
      <StatCard label="当届成员总数" :value="stats.members" :icon="UserFilled" tone="blue" />
      <StatCard label="今日新增简历" :value="stats.resumesToday" :icon="Document" tone="green" />
      <StatCard label="待审核塔服缴费" :value="stats.pendingPayments" :icon="Money" tone="red" />
    </div>

    <div v-if="failed" class="dashboard-view__error page-card">
      <span>部分统计数据暂时无法加载。</span>
      <ElButton text type="primary" @click="loadStats">重新加载</ElButton>
    </div>
  </div>
</template>

<style scoped>
.dashboard-view {
  min-width: 760px;
  max-width: 1640px;
  margin: 0 auto;
}

.dashboard-view__heading { margin-bottom: 22px; }
.dashboard-view__heading p { margin: 0 0 7px; color: var(--quanta-primary); font-size: 10px; font-weight: 750; letter-spacing: .17em; }
.dashboard-view__heading h1 { margin: 0; font-size: 27px; line-height: 1.4; letter-spacing: -.03em; }

.dashboard-view__stats {
  display: grid;
  min-height: 96px;
  margin-top: 22px;
  grid-template-columns: repeat(3, minmax(180px, 1fr));
  gap: 18px;
}

.dashboard-view__error {
  display: flex;
  margin-top: 18px;
  padding: 14px 18px;
  color: var(--quanta-text-secondary);
  align-items: center;
  justify-content: space-between;
}

@media (max-width: 1280px) {
  .dashboard-view__stats {
    grid-template-columns: repeat(2, minmax(220px, 1fr));
  }
}

@media (max-width: 900px) {
  .dashboard-view { min-width: 0; width: 100%; }
  .dashboard-view__stats { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}

@media (max-width: 600px) {
  .dashboard-view__stats { grid-template-columns: minmax(0, 1fr); gap: 12px; margin-top: 16px; }
  .dashboard-view__error { flex-wrap: wrap; gap: 8px; }
}
</style>
