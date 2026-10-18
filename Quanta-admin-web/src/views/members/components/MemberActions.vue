<script setup>
import PermissionButton from '@/components/PermissionButton.vue'

defineProps({
  member: { type: Object, required: true },
  isCurrentCohort: { type: Boolean, default: false },
  pendingAction: { type: String, default: '' },
  canManageMembers: Boolean,
  canMaintainMembers: Boolean,
})

defineEmits(['remove', 'reset-password', 'retain', 'edit-member', 'assign-role'])
</script>

<template>
  <div class="member-table__actions">
    <template v-if="canMaintainMembers">
      <ElButton link type="primary" @click="$emit('edit-member', member)">编辑</ElButton>
      <ElButton link type="primary" @click="$emit('assign-role', member)">分配权限</ElButton>
    </template>
    <template v-if="isCurrentCohort">
      <PermissionButton
        size="small"
        type="danger"
        class="member-table__action member-table__delete"
        :permissions="'system:user:remove'"
        :roles="'ceo'"
        :loading="pendingAction === `remove-${member.id}`"
        @click="$emit('remove', member)"
      >
        删除
      </PermissionButton>
      <PermissionButton
        size="small"
        class="member-table__action member-table__reset"
        :permissions="'system:user:resetPwd'"
        :roles="'ceo'"
        :loading="pendingAction === `reset-${member.id}`"
        @click="$emit('reset-password', member)"
      >
        重置密码
      </PermissionButton>
    </template>
    <PermissionButton
      v-else-if="member.canRetain"
      size="small"
      class="member-table__action member-table__retain"
      :permissions="'qt:member:retain'"
      :loading="pendingAction === `retain-${member.id}`"
      @click="$emit('retain', member)"
    >
      是否留任
    </PermissionButton>
    <span v-else class="member-table__empty-action">—</span>
  </div>
</template>
