<script setup>
import { Filter } from '@element-plus/icons-vue'
import PermissionButton from '@/components/PermissionButton.vue'
import StatusTag from '@/components/StatusTag.vue'

defineProps({
  rows: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
  isCurrentCohort: { type: Boolean, default: false },
  departments: { type: Array, default: () => [] },
  roleOptions: { type: Array, default: () => [] },
  department: { type: String, default: '' },
  roleCategory: { type: String, default: '' },
  pendingAction: { type: String, default: '' },
})

const emit = defineEmits([
  'department-change',
  'role-change',
  'remove',
  'reset-password',
  'retain',
])

const statusMap = {
  active: { label: '在职', type: 'primary' },
  resigned: { label: '已卸任', type: 'info' },
  retained: { label: '已留任', type: 'success' },
  retention_pending: { label: '待确认', type: 'warning' },
}

function statusOf(status) {
  return statusMap[status] || { label: status || '-', type: 'info' }
}
</script>

<template>
  <ElTable
    v-loading="loading"
    class="member-table"
    :data="rows"
    size="small"
    row-key="id"
    empty-text="暂无符合条件的成员"
  >
    <ElTableColumn prop="name" label="姓名" min-width="120" />

    <ElTableColumn prop="department" min-width="140">
      <template #header>
        <div class="member-table__filter-title">
          <span>部门</span>
          <ElDropdown trigger="click" @command="emit('department-change', $event)">
            <button
              type="button"
              class="member-table__filter-button"
              :class="{ 'is-active': department }"
              aria-label="筛选部门"
            >
              <ElIcon><Filter /></ElIcon>
            </button>
            <template #dropdown>
              <ElDropdownMenu>
                <ElDropdownItem command="">全部部门</ElDropdownItem>
                <ElDropdownItem
                  v-for="option in departments"
                  :key="option.value"
                  :command="option.value"
                >
                  {{ option.label }}
                </ElDropdownItem>
              </ElDropdownMenu>
            </template>
          </ElDropdown>
        </div>
      </template>
    </ElTableColumn>

    <ElTableColumn prop="title" min-width="130">
      <template #header>
        <div class="member-table__filter-title">
          <span>角色</span>
          <ElDropdown trigger="click" @command="emit('role-change', $event)">
            <button
              type="button"
              class="member-table__filter-button"
              :class="{ 'is-active': roleCategory }"
              aria-label="筛选角色"
            >
              <ElIcon><Filter /></ElIcon>
            </button>
            <template #dropdown>
              <ElDropdownMenu>
                <ElDropdownItem command="">全部角色</ElDropdownItem>
                <ElDropdownItem
                  v-for="option in roleOptions"
                  :key="option.value"
                  :command="option.value"
                >
                  {{ option.label }}
                </ElDropdownItem>
              </ElDropdownMenu>
            </template>
          </ElDropdown>
        </div>
      </template>
    </ElTableColumn>

    <ElTableColumn min-width="110" label="届数">
      <template #default="{ row }">第{{ row.cohort }}届</template>
    </ElTableColumn>
    <ElTableColumn prop="phoneMasked" label="联系电话" min-width="160" />
    <ElTableColumn prop="joinedAt" label="加入时间" min-width="150" />
    <ElTableColumn label="状态" min-width="110">
      <template #default="{ row }">
        <StatusTag class="member-table__status" :type="statusOf(row.status).type">
          {{ statusOf(row.status).label }}
        </StatusTag>
      </template>
    </ElTableColumn>
    <ElTableColumn label="操作" min-width="190" fixed="right">
      <template #default="{ row }">
        <div class="member-table__actions">
          <template v-if="isCurrentCohort">
            <PermissionButton
              size="small"
              type="danger"
              class="member-table__action member-table__delete"
              :permissions="'system:user:remove'"
              :roles="'ceo'"
              :loading="pendingAction === `remove-${row.id}`"
              @click="emit('remove', row)"
            >
              删除
            </PermissionButton>
            <PermissionButton
              size="small"
              class="member-table__action member-table__reset"
              :permissions="'system:user:resetPwd'"
              :roles="'ceo'"
              :loading="pendingAction === `reset-${row.id}`"
              @click="emit('reset-password', row)"
            >
              重置密码
            </PermissionButton>
          </template>
          <PermissionButton
            v-else-if="row.canRetain"
            size="small"
            class="member-table__action member-table__retain"
            :permissions="'qt:member:retain'"
            :loading="pendingAction === `retain-${row.id}`"
            @click="emit('retain', row)"
          >
            是否留任
          </PermissionButton>
          <span v-else class="member-table__empty-action">—</span>
        </div>
      </template>
    </ElTableColumn>
  </ElTable>
</template>

<style scoped>
.member-table {
  --el-table-header-bg-color: #f7f8fb;
  --el-table-header-text-color: #747b8e;
  --el-table-row-hover-bg-color: #fffaf2;
  width: 100%;
  font-size: 12px;
}

:deep(.el-table__header-wrapper .el-table__cell) {
  height: 40px;
  font-size: 12px;
}

:deep(.el-table__body-wrapper .el-table__cell) {
  height: 46px;
  font-size: 12px;
}

:deep(.el-table__header-wrapper th:first-child .cell),
:deep(.el-table__body-wrapper td:first-child .cell) {
  padding-left: 28px;
}

.member-table__filter-title,
.member-table__actions {
  display: flex;
  align-items: center;
}

.member-table__filter-title {
  gap: 3px;
}

.member-table__filter-button {
  display: inline-flex;
  padding: 3px;
  color: #a1a7b6;
  cursor: pointer;
  background: transparent;
  border: 0;
  align-items: center;
}

.member-table__filter-button:hover,
.member-table__filter-button.is-active {
  color: var(--quanta-primary-dark);
}

.member-table__actions {
  min-height: 26px;
  gap: 7px;
}

:deep(.member-table__action.el-button) {
  min-height: 24px;
  padding: 4px 10px;
  margin: 0;
  color: #fff;
  font-size: 11px;
  line-height: 1;
  border-radius: 5px;
}

:deep(.member-table__delete.el-button) {
  --el-button-bg-color: #e81f1f;
  --el-button-border-color: #e81f1f;
  --el-button-text-color: #fff;
  --el-button-hover-bg-color: #d71b1b;
  --el-button-hover-border-color: #d71b1b;
  --el-button-hover-text-color: #fff;
  --el-button-active-bg-color: #c91818;
  --el-button-active-border-color: #c91818;
}

:deep(.member-table__reset.el-button),
:deep(.member-table__retain.el-button) {
  --el-button-bg-color: #fdaf32;
  --el-button-border-color: #fdaf32;
  --el-button-text-color: #fff;
  --el-button-hover-bg-color: #eda126;
  --el-button-hover-border-color: #eda126;
  --el-button-hover-text-color: #fff;
  --el-button-active-bg-color: #db9017;
  --el-button-active-border-color: #db9017;
}

:deep(.member-table__status.status-tag) {
  padding: 3px 8px;
  font-size: 11px;
}

:deep(.member-table__status.status-tag--primary) {
  color: rgb(29, 78, 216);
}

.member-table__empty-action {
  color: #b8bdc9;
}
</style>
