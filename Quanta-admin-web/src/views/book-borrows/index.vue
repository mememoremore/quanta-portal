<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import StatusTag from '@/components/StatusTag.vue'
import { getBookBorrowRecords } from '@/api/book-borrows'
import './book-borrows.css'

const rows = ref([])
const loading = ref(false)
const failed = ref(false)
const statusMap = {
  BORROWED: { label: '借阅中', type: 'primary' },
  OVERDUE: { label: '已逾期', type: 'danger' },
  RETURNED: { label: '已归还', type: 'success' },
}

function statusMeta(status) {
  return statusMap[status] || { label: status || '-', type: 'info' }
}

function rowClassName({ row }) {
  return row.status === 'OVERDUE' ? 'book-borrows-table__row--overdue' : ''
}

async function loadList() {
  loading.value = true
  failed.value = false
  try {
    rows.value = (await getBookBorrowRecords()).rows
  } catch (error) {
    failed.value = true
    ElMessage.error(error.message || '图书借阅记录加载失败')
  } finally {
    loading.value = false
  }
}

onMounted(loadList)
</script>

<template>
  <section class="page-card book-borrows-card">
    <header class="book-borrows-card__header"><h2>图书借阅记录</h2></header>
    <div v-if="failed && !loading" class="book-borrows-card__error">
      <span>借阅记录暂时无法加载。</span>
      <ElButton type="primary" link @click="loadList">重新加载</ElButton>
    </div>
    <ElTable v-else v-loading="loading" class="book-borrows-table" :data="rows" :row-class-name="rowClassName" size="small" row-key="borrowId" empty-text="暂无借阅记录">
      <ElTableColumn prop="nickName" label="借阅人" min-width="130" />
      <ElTableColumn prop="isbn" label="书籍编号" min-width="130" />
      <ElTableColumn prop="bookName" label="书名" min-width="300" />
      <ElTableColumn label="类型" min-width="130">
        <template #default="{ row }"><span class="book-borrows-table__type" :class="{ 'is-textbook': row.bookType === '教材' }">{{ row.bookType }}</span></template>
      </ElTableColumn>
      <ElTableColumn prop="borrowTime" label="借阅日期" min-width="170" />
      <ElTableColumn prop="dueTime" label="应还日期" min-width="170" />
      <ElTableColumn label="实际归还" min-width="170"><template #default="{ row }">{{ row.returnTime || '-' }}</template></ElTableColumn>
      <ElTableColumn label="状态" min-width="140"><template #default="{ row }"><StatusTag :type="statusMeta(row.status).type">{{ statusMeta(row.status).label }}</StatusTag></template></ElTableColumn>
    </ElTable>
  </section>
</template>
