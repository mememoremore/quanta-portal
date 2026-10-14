<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getSharingRegistrations } from '@/api/sharing-signups'
import './sharing-signups.css'

const loading = ref(false)
const failed = ref(false)
const rows = ref([])
const total = ref(0)
const quota = ref(0)

async function loadList() {
  loading.value = true
  failed.value = false
  try {
    const result = await getSharingRegistrations()
    rows.value = result.rows
    total.value = result.total
    quota.value = result.quota
  } catch (error) {
    failed.value = true
    ElMessage.error(error.message || '精英分享会报名名单加载失败')
  } finally {
    loading.value = false
  }
}

onMounted(loadList)
</script>

<template>
  <section class="page-card sharing-signups-card">
    <header class="business-records__header">
      <h2>精英分享会报名名单</h2>
      <span class="sharing-signups__count">{{ total }} / {{ quota }}</span>
    </header>

    <div v-if="failed && !loading" class="business-records__error">
      <span>报名名单暂时无法加载。</span>
      <ElButton type="primary" link @click="loadList">重新加载</ElButton>
    </div>

    <ElTable
      v-else
      v-loading="loading"
      class="business-records__table"
      :data="rows"
      size="small"
      row-key="registrationId"
      empty-text="暂无报名记录"
    >
      <ElTableColumn type="index" label="序号" width="130" :index="(index) => index + 1" />
      <ElTableColumn prop="name" label="姓名" min-width="170" />
      <ElTableColumn prop="contact" label="联系方式" min-width="250" />
      <ElTableColumn prop="registeredAt" label="报名时间" min-width="300" />
      <ElTableColumn prop="remark" label="备注留言" min-width="360" />
    </ElTable>
  </section>
</template>
