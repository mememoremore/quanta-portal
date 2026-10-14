<script setup>
import { onMounted, reactive, ref } from 'vue'
import { Download, Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getActivities, getActivitySignups } from '@/api/activity-signups'
import { downloadCsv } from '@/utils/csv'
import './activity-signups.css'

const filters = reactive({ title: '', activityType: '', status: '' })
const activities = ref([])
const signupCounts = reactive({})
const loading = ref(false)
const failed = ref(false)
const dialogVisible = ref(false)
const signupLoading = ref(false)
const signups = ref([])
const selectedActivity = ref(null)

const typeLabels = { LECTURE: '宣讲会', SHARING: '分享会', GENERAL: '普通活动' }
const statusLabels = { DRAFT: '草稿', PUBLISHED: '已发布', CANCELED: '已取消', DELETED: '已删除' }
const signupStatusLabels = { APPLIED: '已报名', APPROVED: '已确认', CANCELED: '已取消', REJECTED: '已拒绝' }
const exportColumns = [
  { key: 'nickName', label: '姓名' },
  { key: 'studentNo', label: '学号' },
  { key: 'major', label: '专业' },
  { key: 'phonenumber', label: '联系方式' },
  { key: 'signupTime', label: '报名时间' },
  { key: 'statusText', label: '报名状态' },
]

function queryParams() {
  return Object.fromEntries(Object.entries(filters).filter(([, value]) => value))
}

async function loadCount(activityId) {
  try {
    const result = await getActivitySignups(activityId, { pageNum: 1, pageSize: 1 })
    signupCounts[activityId] = result.total
  } catch {
    signupCounts[activityId] = null
  }
}

async function loadActivities() {
  loading.value = true
  failed.value = false
  try {
    const result = await getActivities(queryParams())
    activities.value = result.rows
    await Promise.all(result.rows.map((item) => loadCount(item.activityId)))
  } catch (error) {
    activities.value = []
    failed.value = true
    ElMessage.error(error.message || '活动列表加载失败')
  } finally {
    loading.value = false
  }
}

function resetFilters() {
  Object.assign(filters, { title: '', activityType: '', status: '' })
  loadActivities()
}

async function openSignups(activity) {
  selectedActivity.value = activity
  dialogVisible.value = true
  signupLoading.value = true
  signups.value = []
  try {
    const result = await getActivitySignups(activity.activityId, { pageNum: 1, pageSize: 1000 })
    signups.value = result.rows
    signupCounts[activity.activityId] = result.total
  } catch (error) {
    ElMessage.error(error.message || '报名名单加载失败')
  } finally {
    signupLoading.value = false
  }
}

function exportSignups() {
  const rows = signups.value.map((item) => ({
    ...item,
    statusText: signupStatusLabels[item.status] || item.status || '未知',
  }))
  downloadCsv(`${selectedActivity.value.title}-报名名单.csv`, exportColumns, rows)
  ElMessage.success('名单导出成功')
}

onMounted(loadActivities)
</script>

<template>
  <section class="activity-signups">
    <header class="activity-signups__heading">
      <div><p>ACTIVITY SIGNUPS</p><h1>活动报名</h1></div>
      <span>统一查看所有活动的报名情况</span>
    </header>

    <div class="page-card activity-signups__filters">
      <ElInput v-model="filters.title" data-testid="activity-title-filter" clearable placeholder="搜索活动名称" @keyup.enter="loadActivities" />
      <ElSelect v-model="filters.activityType" clearable placeholder="活动类型">
        <ElOption label="宣讲会" value="LECTURE" /><ElOption label="分享会" value="SHARING" /><ElOption label="普通活动" value="GENERAL" />
      </ElSelect>
      <ElSelect v-model="filters.status" clearable placeholder="活动状态">
        <ElOption label="草稿" value="DRAFT" /><ElOption label="已发布" value="PUBLISHED" /><ElOption label="已取消" value="CANCELED" />
      </ElSelect>
      <ElButton data-testid="activity-search" type="primary" :icon="Search" @click="loadActivities">查询</ElButton>
      <ElButton @click="resetFilters">重置</ElButton>
    </div>

    <div class="page-card activity-signups__table-card">
      <div v-if="failed && !loading" class="activity-signups__state">
        <strong>活动列表加载失败</strong><ElButton type="primary" link @click="loadActivities">重新加载</ElButton>
      </div>
      <ElTable v-else v-loading="loading" :data="activities" row-key="activityId" empty-text="暂无活动">
        <ElTableColumn prop="title" label="活动名称" min-width="220" />
        <ElTableColumn label="类型" width="110"><template #default="{ row }">{{ typeLabels[row.activityType] || row.activityType }}</template></ElTableColumn>
        <ElTableColumn prop="activityStart" label="活动时间" min-width="150" />
        <ElTableColumn prop="locationDesc" label="地点" min-width="150" />
        <ElTableColumn label="报名人数" width="120"><template #default="{ row }">{{ signupCounts[row.activityId] ?? '—' }} / {{ row.capacity || '不限' }}</template></ElTableColumn>
        <ElTableColumn label="状态" width="100"><template #default="{ row }">{{ statusLabels[row.status] || row.status }}</template></ElTableColumn>
        <ElTableColumn label="操作" width="120" fixed="right">
          <template #default="{ row }"><ElButton :data-testid="`view-signups-${row.activityId}`" type="primary" link @click="openSignups(row)">查看名单</ElButton></template>
        </ElTableColumn>
      </ElTable>
    </div>

    <ElDialog v-model="dialogVisible" :teleported="false" width="980" :title="`${selectedActivity?.title || ''} · 报名名单`">
      <div class="activity-signups__dialog-tools">
        <span>共 {{ signups.length }} 人</span>
        <ElButton data-testid="export-signups" :icon="Download" :disabled="!signups.length" @click="exportSignups">导出名单</ElButton>
      </div>
      <ElTable v-loading="signupLoading" :data="signups" max-height="480" empty-text="暂无报名记录">
        <ElTableColumn prop="nickName" label="姓名" min-width="110" />
        <ElTableColumn prop="studentNo" label="学号" min-width="140" />
        <ElTableColumn prop="major" label="专业" min-width="150" />
        <ElTableColumn prop="phonenumber" label="联系方式" min-width="140" />
        <ElTableColumn prop="signupTime" label="报名时间" min-width="130" />
        <ElTableColumn label="状态" width="100"><template #default="{ row }">{{ signupStatusLabels[row.status] || row.status }}</template></ElTableColumn>
      </ElTable>
    </ElDialog>
  </section>
</template>
