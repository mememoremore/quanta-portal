<script setup>
import { computed, onMounted, ref } from 'vue'
import { getMyServices } from '@/api/portal/services'

const tabs = [
  { key: 'borrows', label: '图书借阅' },
  { key: 'orders', label: '塔服订单' },
]
const activeTab = ref('borrows')
const snapshot = ref({ borrows: [], orders: [] })
const loading = ref(true)
const errorMessage = ref('')
const activeRows = computed(() => snapshot.value[activeTab.value] || [])

const emptyCopy = computed(() => ({
  borrows: ['暂无图书借阅记录', '去图书库看看近期可借的书籍。'],
  orders: ['暂无塔服订单记录', '保存订购草稿后，会在这里显示最新状态。'],
}[activeTab.value]))

function statusLabel(status) {
  return ({
    DRAFT: '草稿', SUBMITTED: '待确认', APPROVED: '已确认', REJECTED: '已驳回',
    BORROWED: '借阅中', OVERDUE: '已逾期', RETURNED: '已归还',
  })[status] || status || '处理中'
}

async function loadServices() {
  loading.value = true
  errorMessage.value = ''
  try {
    snapshot.value = await getMyServices()
  } catch (error) {
    errorMessage.value = error.message || '服务记录加载失败'
  } finally {
    loading.value = false
  }
}

onMounted(loadServices)
</script>

<template>
  <section class="member-service-page">
    <header class="member-service-heading">
      <div><p>MY SERVICES</p><h1>我的服务</h1><span>统一查看图书与塔服的办理记录。</span></div>
      <div class="member-service-heading__actions">
        <button class="member-secondary-button" type="button" :disabled="loading" @click="loadServices">刷新记录</button>
      </div>
    </header>

    <p v-if="errorMessage" class="member-feedback is-error" role="alert">
      {{ errorMessage }} <button type="button" @click="loadServices">重新加载</button>
    </p>
    <div class="member-service-tabs" role="tablist" aria-label="服务记录分类">
      <button
        v-for="tab in tabs"
        :key="tab.key"
        type="button"
        role="tab"
        :aria-selected="activeTab === tab.key"
        :class="{ 'is-active': activeTab === tab.key }"
        @click="activeTab = tab.key"
      >
        {{ tab.label }} <span>{{ snapshot[tab.key].length }}</span>
      </button>
    </div>

    <div v-if="loading" class="member-service-state portal-card">正在加载服务记录…</div>
    <div v-else-if="activeRows.length" class="member-record-list">
      <article v-for="row in activeRows" :key="row.borrowId || row.orderId" class="member-record-card portal-card">
        <template v-if="activeTab === 'borrows'">
          <div><small>图书</small><strong>{{ row.bookName || `图书 #${row.bookId}` }}</strong></div>
          <div><small>应还日期</small><span>{{ row.dueTime || '待确认' }}</span></div>
        </template>
        <template v-else>
          <div><small>塔服</small><strong>{{ row.itemName || `款式 #${row.itemId}` }}</strong></div>
          <div><small>选项</small><span>{{ row.selectedColor || '-' }} · {{ row.selectedSize || '-' }} · {{ row.quantity }} 件</span></div>
        </template>
        <span class="member-status-tag">{{ statusLabel(row.status) }}</span>
      </article>
    </div>
    <div v-else class="member-service-state portal-card">
      <strong>{{ emptyCopy[0] }}</strong><p>{{ emptyCopy[1] }}</p>
    </div>
  </section>
</template>

<style src="../member.css"></style>
<style src="./services.css"></style>
<style scoped>
.member-service-heading__actions { display: flex; align-items: center; gap: 10px; }
.member-service-heading__actions a { text-decoration: none; display: inline-flex; align-items: center; }
</style>
