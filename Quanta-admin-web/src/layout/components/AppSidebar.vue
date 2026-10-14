<script setup>
import { computed } from 'vue'
import { storeToRefs } from 'pinia'
import { useRoute } from 'vue-router'
import { useAppStore } from '@/stores/app'
import { usePermissionStore } from '@/stores/permission'
import SidebarMenuItem from './SidebarMenuItem.vue'
import UserPanel from './UserPanel.vue'

const route = useRoute()
const appStore = useAppStore()
const permissionStore = usePermissionStore()
const { sidebarCollapsed } = storeToRefs(appStore)

const groupedRoutes = computed(() => {
  const groups = []
  permissionStore.routes
    .filter((item) => !item.meta?.hidden)
    .forEach((item) => {
      const title = item.meta?.group || '其他'
      let group = groups.find((entry) => entry.title === title)
      if (!group) {
        group = { title, routes: [] }
        groups.push(group)
      }
      group.routes.push(item)
    })
  return groups
})
</script>

<template>
  <aside class="app-sidebar">
    <div class="app-sidebar__brand">
      <div class="app-sidebar__logo">
        <img :src="'/quanta-logo.jpg'" alt="Quanta 社团 Logo" />
      </div>
      <div v-show="!sidebarCollapsed" class="app-sidebar__brand-copy">
        <strong>Quanta</strong>
        <span>后台管理系统</span>
      </div>
    </div>

    <ElScrollbar class="app-sidebar__scrollbar">
      <ElMenu
        class="app-sidebar__menu"
        :default-active="route.path"
        :collapse="sidebarCollapsed"
        :collapse-transition="false"
        router
      >
        <div v-for="group in groupedRoutes" :key="group.title" class="menu-group">
          <p v-show="!sidebarCollapsed" class="menu-group__title">{{ group.title }}</p>
          <SidebarMenuItem
            v-for="menuRoute in group.routes"
            :key="menuRoute.name"
            :route="menuRoute"
          />
        </div>
      </ElMenu>
    </ElScrollbar>

    <UserPanel />
  </aside>
</template>

