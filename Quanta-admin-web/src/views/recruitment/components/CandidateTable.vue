<script setup>
import { computed } from 'vue'
import { storeToRefs } from 'pinia'
import StatusTag from '@/components/StatusTag.vue'
import { departmentLabels, resultLabels } from '@/api/recruitment'
import { useUserStore } from '@/stores/user'

const props = defineProps({ rows: { type: Array, default: () => [] }, loading: { type: Boolean, default: false }, roundId: { type: Number, default: 1 }, pendingAction: { type: String, default: '' } })
const emit = defineEmits(['resume', 'view-feedback', 'edit-feedback', 'select-result', 'score', 'notice'])
const userStore = useUserStore()
const { permissions, roles, departmentCode } = storeToRefs(userStore)
const hasPermission = (permission) => permissions.value.includes('*:*:*') || permissions.value.includes(permission)
const canEvaluate = computed(() => hasPermission('qt:interview:admin:evaluate'))
const canDecide = computed(() => hasPermission('qt:interview:admin:offer'))
const canViewAllDepartments = computed(
  () => hasPermission('*:*:*') || (roles.value || []).some((role) => role === 'ceo' || role === 'admin'),
)
const choice = (row, order) => row.choices?.find((item) => item.choiceOrder === order)
function roundState(track) {
  if (!track || (props.roundId === 2 && !track.rounds?.[2]?.advanced)) return null
  return track.rounds?.[props.roundId] || null
}
const statusType = (status) => ({ PASS: 'success', OUT: 'danger', WAITING: 'warning', PENDING: 'info' })[status] || 'info'
function availableTracks(row) {
  return (row.choices || []).filter((track) => {
    if (props.roundId === 2 && !track.rounds?.[2]?.advanced) return false
    return canViewAllDepartments.value || track.department === departmentCode.value
  })
}
const canEditDepartment = (row) => canEvaluate.value && availableTracks(row).some((track) => track.department === departmentCode.value)
function noticeReady(row) {
  const tracks = (row.choices || []).filter((track) => track.rounds?.[2]?.advanced)
  const visiblePassed = availableTracks(row).filter((track) => track.rounds?.[2]?.status === 'PASS')
  return tracks.length > 0 && tracks.every((track) => ['PASS', 'OUT'].includes(track.rounds?.[2]?.status)) && visiblePassed.some((track) => row.noticeStatuses?.[track.department] === 'UNSENT')
}
function allVisibleNoticesSent(row) {
  const passed = availableTracks(row).filter((track) => track.rounds?.[2]?.status === 'PASS')
  return passed.length > 0 && passed.every((track) => row.noticeStatuses?.[track.department] === 'SENT')
}
function noticeLabel(row) {
  if (allVisibleNoticesSent(row)) return '邮件已发送'
  const passed = availableTracks(row).filter((track) => track.rounds?.[2]?.status === 'PASS')
  if (passed.some((track) => !['SENT', 'UNSENT'].includes(row.noticeStatuses?.[track.department]))) return '邮件状态待同步'
  return '发送邮件'
}
</script>

<template>
  <ElTable v-loading="loading" class="recruitment-table" :data="rows" size="small" row-key="applicationId" empty-text="暂无符合条件的候选人">
    <ElTableColumn label="姓名" min-width="135"><template #default="{ row }"><div class="candidate-identity"><strong>{{ row.name }}</strong><span>{{ row.studentNo }}</span></div></template></ElTableColumn>
    <ElTableColumn label="专业/班级" min-width="165"><template #default="{ row }"><div class="candidate-identity"><strong>{{ row.major }}</strong><span>{{ row.className }}</span></div></template></ElTableColumn>
    <ElTableColumn v-for="order in [1, 2]" :key="order" :label="order === 1 ? '第一志愿' : '第二志愿'" min-width="170">
      <template #default="{ row }">
        <div v-if="choice(row, order)" class="candidate-choice">
          <span class="candidate-choice__department" :class="`choice-${order}`">{{ departmentLabels[choice(row, order).department] || choice(row, order).department }}</span>
          <StatusTag v-if="roundState(choice(row, order))" class="candidate-choice__status" :class="{ 'is-plain-result': ['PASS', 'OUT'].includes(roundState(choice(row, order)).status) }" :type="statusType(roundState(choice(row, order)).status)">{{ resultLabels[roundState(choice(row, order)).status] }}</StatusTag>
          <span v-else class="candidate-choice__inactive">未晋级</span>
          <small v-if="roundId === 2 && roundState(choice(row, order))" class="candidate-choice__score">{{ roundState(choice(row, order)).score == null ? '未评分' : `${roundState(choice(row, order)).score} 分` }}</small>
        </div>
      </template>
    </ElTableColumn>
    <ElTableColumn prop="appliedAt" label="投递时间" min-width="155" />
    <ElTableColumn label="操作" min-width="430" fixed="right">
      <template #default="{ row }">
        <div class="recruitment-table__actions">
          <ElButton class="recruitment-action is-neutral" @click="emit('resume', row)">阅览简历</ElButton>
          <template v-if="roundId === 1 && availableTracks(row).length">
            <ElButton class="recruitment-action is-neutral" @click="emit('view-feedback', row)">查看面评</ElButton>
            <ElButton v-if="canEditDepartment(row)" class="recruitment-action is-primary" @click="emit('edit-feedback', row)">编辑面评</ElButton>
          </template>
          <ElButton v-if="roundId === 2 && availableTracks(row).length" class="recruitment-action is-primary" data-test="candidate-score" @click="emit('score', row)">面试评分</ElButton>
          <template v-if="canDecide && availableTracks(row).length">
            <ElButton class="recruitment-action is-pass" data-test="candidate-result-pass" @click="emit('select-result', row, 'PASS')">Pass</ElButton>
            <ElButton class="recruitment-action is-out" data-test="candidate-result-out" @click="emit('select-result', row, 'OUT')">Out</ElButton>
          </template>
          <ElButton v-if="roundId === 2 && canDecide" class="recruitment-action is-mail" data-test="candidate-notice" :disabled="!noticeReady(row) || pendingAction === `notice-${row.applicationId}`" @click="emit('notice', row)">{{ noticeLabel(row) }}</ElButton>
        </div>
      </template>
    </ElTableColumn>
  </ElTable>
</template>
