<script setup>
import { computed, onMounted, ref } from 'vue'
import StatusTag from '@/components/StatusTag.vue'
import { getMemberApplications, recruitmentDepartments } from '@/api/portal/recruitment'

const applications = ref([])
const keyword = ref('')
const statusFilter = ref('')
const loading = ref(true)
const errorMessage = ref('')

const departmentLabels = Object.fromEntries(
  recruitmentDepartments.map(({ value, label }) => [value, label]),
)

const statusOptions = [
  { value: 'SUBMITTED', label: '已投递' },
  { value: 'PROCESSING', label: '处理中' },
  { value: 'OFFERED', label: '已录用' },
  { value: 'REJECTED', label: '已淘汰' },
]

const statusLabels = Object.fromEntries(statusOptions.map(({ value, label }) => [value, label]))

function statusType(status) {
  return {
    SUBMITTED: 'info',
    PROCESSING: 'warning',
    OFFERED: 'success',
    REJECTED: 'danger',
  }[status] || 'info'
}

function roundStatusType(status) {
  if (!status) return ''
  return { PASS: 'success', FAIL: 'danger', OUT: 'danger', WAITING: 'warning', PENDING: 'info' }[status] || 'info'
}

function roundStatusLabel(status) {
  if (!status) return '―'
  return { PASS: 'Pass', FAIL: 'Out', OUT: 'Out', WAITING: '待面', PENDING: '待评' }[status] || status
}

const visibleRows = computed(() => applications.value.filter((item) => {
  const text = `${item.realName || ''} ${item.studentNo || ''} ${item.className || ''}`.toLowerCase()
  return (!keyword.value || text.includes(keyword.value.trim().toLowerCase()))
    && (!statusFilter.value || item.applyStatus === statusFilter.value)
}))

async function load() {
  loading.value = true
  errorMessage.value = ''
  try {
    applications.value = await getMemberApplications()
  } catch (error) {
    errorMessage.value = error.message || '招新报名信息加载失败'
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <section class="member-page member-recruitment">
    <header class="member-page__heading">
      <div>
        <p>RECRUITMENT APPLICATIONS</p>
        <h1>招新报名</h1>
        <span>查看本届新生报名信息（只读）。</span>
      </div>
    </header>

    <div class="member-recruitment__filters portal-card">
      <input v-model="keyword" type="search" placeholder="搜索姓名、学号或班级" aria-label="搜索候选人" />
      <select v-model="statusFilter" aria-label="筛选投递状态">
        <option value="">全部状态</option>
        <option v-for="item in statusOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
      </select>
    </div>

    <p v-if="errorMessage" class="member-feedback is-error" role="alert">{{ errorMessage }}</p>

    <div v-if="loading" class="member-loading portal-card">正在加载招新报名信息…</div>

    <div v-else-if="visibleRows.length" class="member-recruitment__table portal-card">
      <table>
        <thead>
          <tr>
            <th>姓名</th>
            <th>性别</th>
            <th>班级</th>
            <th>第一志愿</th>
            <th>一面</th>
            <th>二面</th>
            <th>第二志愿</th>
            <th>一面</th>
            <th>二面</th>
            <th>投递状态</th>
            <th>录用部门</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in visibleRows" :key="row.applicationId">
            <td>
              <div class="member-recruitment__name">
                <strong>{{ row.realName || '-' }}</strong>
                <span v-if="row.studentNo">{{ row.studentNo }}</span>
              </div>
            </td>
            <td>{{ row.gender === '0' ? '男' : row.gender === '1' ? '女' : '未知' }}</td>
            <td>{{ row.className || '-' }}</td>
            <td>{{ departmentLabels[row.firstChoice] || row.firstChoice || '-' }}</td>
            <td>
              <StatusTag v-if="row.firstChoiceFirstRoundStatus" :type="roundStatusType(row.firstChoiceFirstRoundStatus)">
                {{ roundStatusLabel(row.firstChoiceFirstRoundStatus) }}
              </StatusTag>
              <span v-else>―</span>
            </td>
            <td>
              <StatusTag v-if="row.firstChoiceSecondRoundStatus" :type="roundStatusType(row.firstChoiceSecondRoundStatus)">
                {{ roundStatusLabel(row.firstChoiceSecondRoundStatus) }}
              </StatusTag>
              <span v-else>―</span>
            </td>
            <td>{{ departmentLabels[row.secondChoice] || row.secondChoice || '-' }}</td>
            <td>
              <StatusTag v-if="row.secondChoiceFirstRoundStatus" :type="roundStatusType(row.secondChoiceFirstRoundStatus)">
                {{ roundStatusLabel(row.secondChoiceFirstRoundStatus) }}
              </StatusTag>
              <span v-else>―</span>
            </td>
            <td>
              <StatusTag v-if="row.secondChoiceSecondRoundStatus" :type="roundStatusType(row.secondChoiceSecondRoundStatus)">
                {{ roundStatusLabel(row.secondChoiceSecondRoundStatus) }}
              </StatusTag>
              <span v-else>―</span>
            </td>
            <td>
              <StatusTag :type="statusType(row.applyStatus)">
                {{ statusLabels[row.applyStatus] || row.applyStatus || '-' }}
              </StatusTag>
            </td>
            <td>{{ departmentLabels[row.offeredDepartment] || row.offeredDepartment || '-' }}</td>
          </tr>
        </tbody>
      </table>
    </div>

    <div v-else class="member-empty portal-card">暂无招新报名信息</div>
  </section>
</template>

<style src="../member.css"></style>
<style scoped>
.member-recruitment__filters {
  display: grid;
  padding: 16px;
  grid-template-columns: minmax(260px, 1fr) 180px;
  gap: 12px;
}
.member-recruitment__filters input,
.member-recruitment__filters select {
  box-sizing: border-box;
  width: 100%;
  padding: 11px 13px;
  border: 1px solid var(--portal-line);
  border-radius: 11px;
  background: #fff;
  color: var(--portal-ink);
  font: inherit;
}
.member-recruitment__table {
  padding: 0;
  overflow-x: auto;
}
.member-recruitment__table table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}
.member-recruitment__table thead th {
  position: sticky;
  top: 0;
  padding: 12px 14px;
  background: #f7f8fa;
  color: var(--portal-muted);
  font-size: 11px;
  font-weight: 750;
  text-align: left;
  white-space: nowrap;
  border-bottom: 1px solid var(--portal-line);
}
.member-recruitment__table tbody td {
  padding: 12px 14px;
  border-bottom: 1px solid var(--portal-line);
  white-space: nowrap;
  color: var(--portal-ink-soft);
}
.member-recruitment__table tbody tr:hover {
  background: #fafbfc;
}
.member-recruitment__name {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.member-recruitment__name strong {
  font-size: 14px;
}
.member-recruitment__name span {
  color: var(--portal-muted);
  font-size: 11px;
}
@media (max-width: 700px) {
  .member-recruitment__filters {
    grid-template-columns: 1fr;
  }
  .member-recruitment__table {
    max-width: calc(100vw - 28px);
    overscroll-behavior-inline: contain;
    -webkit-overflow-scrolling: touch;
  }
  .member-recruitment__table table { min-width: 1180px; }
}
</style>
