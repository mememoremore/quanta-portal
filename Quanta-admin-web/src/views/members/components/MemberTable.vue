<script setup>
import { Filter } from '@element-plus/icons-vue'
import StatusTag from '@/components/StatusTag.vue'
import { useMediaQuery } from '@/composables/useMediaQuery'
import MemberActions from './MemberActions.vue'

const isMobile = useMediaQuery('(max-width: 600px)')

defineProps({
  rows: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
  isCurrentCohort: { type: Boolean, default: false },
  departments: { type: Array, default: () => [] },
  roleOptions: { type: Array, default: () => [] },
  department: { type: String, default: '' },
  roleCategory: { type: String, default: '' },
  pendingAction: { type: String, default: '' },
  canManageMembers: Boolean,
})

const emit = defineEmits([
  'department-change',
  'role-change',
  'remove',
  'reset-password',
  'retain',
  'edit-member',
  'assign-role',
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
  <section v-if="isMobile" v-loading="loading" class="member-mobile" aria-label="成员列表">
    <div class="member-mobile__filters">
      <label>
        <span>部门</span>
        <ElSelect :model-value="department" placeholder="全部部门" aria-label="筛选部门" @update:model-value="emit('department-change', $event)">
          <ElOption label="全部部门" value="" />
          <ElOption v-for="option in departments" :key="option.value" :label="option.label" :value="option.value" />
        </ElSelect>
      </label>
      <label>
        <span>角色</span>
        <ElSelect :model-value="roleCategory" placeholder="全部角色" aria-label="筛选角色" @update:model-value="emit('role-change', $event)">
          <ElOption label="全部角色" value="" />
          <ElOption v-for="option in roleOptions" :key="option.value" :label="option.label" :value="option.value" />
        </ElSelect>
      </label>
    </div>
    <p v-if="!rows.length" class="member-mobile__empty">{{ loading ? '正在加载成员…' : '暂无符合条件的成员' }}</p>
    <article v-for="row in rows" :key="row.id" class="member-mobile__card">
      <header>
        <h2>{{ row.name }}</h2>
        <StatusTag :type="statusOf(row.status).type">{{ statusOf(row.status).label }}</StatusTag>
      </header>
      <dl>
        <div><dt>部门</dt><dd>{{ row.department }}</dd></div>
        <div><dt>角色</dt><dd>{{ row.title }}</dd></div>
        <div><dt>届数</dt><dd>第{{ row.cohort }}届</dd></div>
        <div><dt>联系电话</dt><dd>{{ row.phoneMasked }}</dd></div>
        <div class="member-mobile__joined"><dt>加入时间</dt><dd>{{ row.joinedAt }}</dd></div>
      </dl>
      <MemberActions
        :member="row"
        :is-current-cohort="isCurrentCohort"
        :pending-action="pendingAction"
        :can-manage-members="canManageMembers"
        @edit-member="emit('edit-member', $event)"
        @assign-role="emit('assign-role', $event)"
        @remove="emit('remove', $event)"
        @reset-password="emit('reset-password', $event)"
        @retain="emit('retain', $event)"
      />
    </article>
  </section>
  <ElTable
    v-else
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
    <ElTableColumn label="操作" :min-width="canManageMembers ? 330 : 190" fixed="right">
      <template #default="{ row }">
        <MemberActions
          :member="row"
          :is-current-cohort="isCurrentCohort"
          :pending-action="pendingAction"
          :can-manage-members="canManageMembers"
          @edit-member="emit('edit-member', $event)"
          @assign-role="emit('assign-role', $event)"
          @remove="emit('remove', $event)"
          @reset-password="emit('reset-password', $event)"
          @retain="emit('retain', $event)"
        />
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

:deep(.member-table__empty-action) {
  color: #b8bdc9;
}

.member-mobile { min-width: 0; padding: 16px; }
.member-mobile__filters { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px; }
.member-mobile__filters label { display: grid; min-width: 0; gap: 8px; color: var(--quanta-text-secondary); font-size: 12px; }
.member-mobile__filters .el-select { width: 100%; min-width: 0; }
.member-mobile__card { margin-top: 14px; padding: 16px; border: 1px solid var(--quanta-border); border-radius: 16px; background: #fff; }
.member-mobile__card header { display: flex; align-items: flex-start; justify-content: space-between; gap: 12px; }
.member-mobile__card h2 { min-width: 0; margin: 0; font-size: 16px; line-height: 1.5; overflow-wrap: anywhere; }
.member-mobile__card header .status-tag { flex-shrink: 0; }
.member-mobile__card dl { display: grid; margin: 14px 0 0; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 14px 12px; }
.member-mobile__card dl > div { min-width: 0; }
.member-mobile__card dt { margin-bottom: 4px; color: var(--quanta-text-secondary); font-size: 11px; }
.member-mobile__card dd { margin: 0; font-size: 13px; line-height: 1.5; overflow-wrap: anywhere; }
.member-mobile__joined { grid-column: 1 / -1; }
.member-mobile .member-table__actions { margin-top: 14px; flex-wrap: wrap; }
.member-mobile .member-table__actions:empty { display: none; }
.member-mobile :deep(.member-table__action.el-button) { min-height: 36px; padding: 8px 12px; font-size: 12px; border-radius: 9px; }
.member-mobile__empty { margin: 28px 0; color: var(--quanta-text-secondary); text-align: center; line-height: 1.6; }
</style>
