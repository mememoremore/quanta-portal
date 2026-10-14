<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Upload } from '@element-plus/icons-vue'
import PermissionButton from '@/components/PermissionButton.vue'
import {
  getMemberCohorts,
  getMemberList,
  memberRoleOptions,
  removeMember,
  resetMemberPassword,
  retainMember,
} from '@/api/members'
import CohortTabs from './components/CohortTabs.vue'
import ImportMembersDialog from './components/ImportMembersDialog.vue'
import MemberFilters from './components/MemberFilters.vue'
import MemberTable from './components/MemberTable.vue'
import RetainConfirmDialog from './components/RetainConfirmDialog.vue'
import './members.css'

const cohorts = ref([])
const rows = ref([])
const departments = ref([])
const loading = ref(false)
const loadFailed = ref(false)
const importVisible = ref(false)
const retainVisible = ref(false)
const selectedRetainMember = ref(null)
const pendingAction = ref('')
const total = ref(0)
let requestSequence = 0

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  memberCohort: '',
  nickName: '',
  memberDepartment: '',
  roleCategory: '',
})

const selectedCohort = computed({
  get: () => query.memberCohort,
  set: (value) => {
    query.memberCohort = value
    query.pageNum = 1
    loadMembers()
  },
})

const isCurrentCohort = computed(
  () => cohorts.value.find((item) => item.value === query.memberCohort)?.isCurrent === true,
)

const hasFilters = computed(
  () => Boolean(query.nickName || query.memberDepartment || query.roleCategory),
)

async function loadCohorts() {
  cohorts.value = await getMemberCohorts()
  if (!query.memberCohort || !cohorts.value.some((item) => item.value === query.memberCohort)) {
    query.memberCohort =
      cohorts.value.find((item) => item.isCurrent)?.value || cohorts.value[0]?.value || ''
  }
}

async function loadMembers() {
  if (!query.memberCohort) return
  const currentRequest = ++requestSequence
  loading.value = true
  loadFailed.value = false
  try {
    const result = await getMemberList({ ...query })
    if (currentRequest !== requestSequence) return
    rows.value = result.rows
    total.value = result.total
    if (result.departments.length) departments.value = result.departments
  } catch (error) {
    if (currentRequest !== requestSequence) return
    loadFailed.value = true
    ElMessage.error(error.message || '成员名单加载失败')
  } finally {
    if (currentRequest === requestSequence) loading.value = false
  }
}

async function initialize() {
  loading.value = true
  try {
    await loadCohorts()
    await loadMembers()
  } catch (error) {
    loadFailed.value = true
    loading.value = false
    ElMessage.error(error.message || '成员管理初始化失败')
  }
}

function updateQuery(field, value) {
  query[field] = value
  query.pageNum = 1
  loadMembers()
}

function clearFilters() {
  query.nickName = ''
  query.memberDepartment = ''
  query.roleCategory = ''
  query.pageNum = 1
  loadMembers()
}

async function confirmRemove(member) {
  try {
    await ElMessageBox.confirm(
      `删除后将无法在当前名单中查看“${member.name}”，确认删除吗？`,
      '确认删除成员',
      {
        confirmButtonText: '确认删除',
        cancelButtonText: '取消',
        type: 'warning',
        confirmButtonClass: 'el-button--danger',
      },
    )
  } catch {
    return
  }

  pendingAction.value = `remove-${member.id}`
  try {
    await removeMember(member.id)
    if (rows.value.length === 1 && query.pageNum > 1) query.pageNum -= 1
    ElMessage.success('成员已删除')
    await loadMembers()
  } catch (error) {
    ElMessage.error(error.message || '删除失败')
  } finally {
    pendingAction.value = ''
  }
}

async function resetPassword(member) {
  let password
  try {
    const prompt = await ElMessageBox.prompt(`请输入 ${member.name} 的新密码`, '重置密码', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputType: 'password',
      inputPlaceholder: '5–20 位新密码',
      inputValidator: (value) => {
        if (!value || value.length < 5 || value.length > 20) return '密码长度需为 5–20 位'
        return true
      },
    })
    password = prompt.value
  } catch {
    return
  }
  pendingAction.value = `reset-${member.id}`
  try {
    const response = await resetMemberPassword(member.id, password)
    ElMessage.success(response.msg || '密码重置成功')
  } catch (error) {
    ElMessage.error(error.message || '密码重置失败')
  } finally {
    pendingAction.value = ''
  }
}

function openRetainDialog(member) {
  selectedRetainMember.value = member
  retainVisible.value = true
}

async function submitRetain() {
  const member = selectedRetainMember.value
  if (!member) return
  pendingAction.value = `retain-${member.id}`
  try {
    const response = await retainMember(member.id, { sourceCohortId: member.cohort })
    ElMessage.success(response.msg || '留任成功')
    await loadCohorts()
    await loadMembers()
    retainVisible.value = false
    selectedRetainMember.value = null
  } catch (error) {
    if (Number(error.code) === 409) await loadMembers()
    ElMessage.error(error.message || '留任操作失败')
  } finally {
    pendingAction.value = ''
  }
}

function changePage(page) {
  query.pageNum = page
  loadMembers()
}

onMounted(initialize)
</script>

<template>
  <div class="members-view">
    <section class="page-card members-card">
      <div class="members-card__toolbar">
        <MemberFilters :name="query.nickName" @search="updateQuery('nickName', $event)" />
        <PermissionButton
          type="primary"
          size="small"
          class="quanta-primary-button"
          :icon="Upload"
          :permissions="'system:user:import'"
          :roles="'ceo'"
          @click="importVisible = true"
        >
          导入名单
        </PermissionButton>
      </div>

      <CohortTabs v-model="selectedCohort" :cohorts="cohorts" />

      <div v-if="loadFailed && !loading" class="members-card__error">
        <span>成员名单暂时无法加载。</span>
        <ElButton type="primary" link @click="loadMembers">重新加载</ElButton>
      </div>

      <MemberTable
        v-else
        :rows="rows"
        :loading="loading"
        :is-current-cohort="isCurrentCohort"
        :departments="departments"
        :role-options="memberRoleOptions"
        :department="query.memberDepartment"
        :role-category="query.roleCategory"
        :pending-action="pendingAction"
        @department-change="updateQuery('memberDepartment', $event)"
        @role-change="updateQuery('roleCategory', $event)"
        @remove="confirmRemove"
        @reset-password="resetPassword"
        @retain="openRetainDialog"
      />

      <div v-if="!loading && !loadFailed" class="members-card__footer">
        <ElButton v-if="hasFilters && total === 0" link @click="clearFilters">清空筛选条件</ElButton>
        <span v-else></span>
        <ElPagination
          v-if="total > query.pageSize"
          background
          small
          layout="prev, pager, next"
          :current-page="query.pageNum"
          :page-size="query.pageSize"
          :total="total"
          @current-change="changePage"
        />
      </div>
    </section>

    <ImportMembersDialog
      v-model="importVisible"
      @success="loadMembers"
    />

    <RetainConfirmDialog
      v-model="retainVisible"
      :member="selectedRetainMember"
      :submitting="pendingAction.startsWith('retain-')"
      @confirm="submitRetain"
    />
  </div>
</template>
