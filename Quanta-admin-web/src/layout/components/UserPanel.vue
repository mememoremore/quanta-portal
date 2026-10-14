<script setup>
import { computed } from 'vue'
import { storeToRefs } from 'pinia'
import { useRouter } from 'vue-router'
import { ArrowUp, SwitchButton } from '@element-plus/icons-vue'
import { useAppStore } from '@/stores/app'
import { useUserStore } from '@/stores/user'
import { usePermissionStore } from '@/stores/permission'

const router = useRouter()
const appStore = useAppStore()
const userStore = useUserStore()
const permissionStore = usePermissionStore()
const { sidebarCollapsed } = storeToRefs(appStore)
const { user, displayName } = storeToRefs(userStore)

const initial = computed(() => displayName.value.slice(0, 1).toUpperCase())

async function handleCommand(command) {
  if (command !== 'logout') return
  await userStore.logout()
  permissionStore.resetRoutes()
  await router.replace('/admin/login')
}
</script>

<template>
  <ElDropdown placement="top" trigger="click" @command="handleCommand">
    <button class="user-panel" type="button">
      <span class="user-panel__avatar">{{ initial }}</span>
      <span v-show="!sidebarCollapsed" class="user-panel__copy">
        <strong>{{ displayName }}</strong>
        <small>{{ user?.memberDepartment || 'Quanta' }}</small>
      </span>
      <ElIcon v-show="!sidebarCollapsed" class="user-panel__arrow"><ArrowUp /></ElIcon>
    </button>
    <template #dropdown>
      <ElDropdownMenu>
        <ElDropdownItem command="logout" :icon="SwitchButton">退出登录</ElDropdownItem>
      </ElDropdownMenu>
    </template>
  </ElDropdown>
</template>
