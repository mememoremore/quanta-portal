<script setup>
import { onMounted, ref } from 'vue'
import { Download } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getLectureRegistrations } from '@/api/lecture-signups'
import { downloadCsv } from '@/utils/csv'
import './lecture-signups.css'

const rows = ref([])
const total = ref(0)
const quota = ref(0)
const loading = ref(false)
const failed = ref(false)

const exportColumns = [
  { key: 'name', label: '姓名' },
  { key: 'studentNo', label: '学号' },
  { key: 'major', label: '专业' },
  { key: 'phone', label: '联系电话' },
  { key: 'registeredAt', label: '报名时间' },
]

async function loadList() {
  loading.value = true
  failed.value = false
  try {
    const result = await getLectureRegistrations()
    rows.value = result.rows
    total.value = result.total
    quota.value = result.quota
  } catch (error) {
    failed.value = true
    ElMessage.error(error.message || '宣讲会报名名单加载失败')
  } finally {
    loading.value = false
  }
}

function exportList() {
  downloadCsv('宣讲会报名名单.csv', exportColumns, rows.value)
  ElMessage.success('名单导出成功')
}

onMounted(loadList)
</script>

<template>
  <section class="page-card lecture-signups-card">
    <header class="lecture-signups-card__header">
      <div class="lecture-signups-card__title">
        <h2>宣讲会报名名单</h2>
        <span>{{ total }} / {{ quota }}</span>
      </div>
      <ElButton size="small" :icon="Download" :disabled="!rows.length" @click="exportList">
        导出名单
      </ElButton>
    </header>

    <div v-if="failed && !loading" class="lecture-signups-card__error">
      <span>报名名单暂时无法加载。</span>
      <ElButton type="primary" link @click="loadList">重新加载</ElButton>
    </div>

    <ElTable v-else v-loading="loading" class="lecture-signups-table" :data="rows" size="small" row-key="registrationId" empty-text="暂无报名记录">
      <ElTableColumn type="index" label="序号" width="130" :index="(index) => index + 1" />
      <ElTableColumn prop="name" label="姓名" min-width="170" />
      <ElTableColumn prop="studentNo" label="学号" min-width="190" />
      <ElTableColumn prop="major" label="专业" min-width="230" />
      <ElTableColumn prop="phone" label="联系电话" min-width="250" />
      <ElTableColumn prop="registeredAt" label="报名时间" min-width="280" />
    </ElTable>
  </section>
</template>
