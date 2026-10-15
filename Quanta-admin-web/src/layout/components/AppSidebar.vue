<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { storeToRefs } from 'pinia'
import { useRoute, useRouter } from 'vue-router'
import { useAppStore } from '@/stores/app'
import { usePermissionStore } from '@/stores/permission'
import SidebarMenuItem from './SidebarMenuItem.vue'
import UserPanel from './UserPanel.vue'

defineProps({ mobileMenuOpen: { type: Boolean, default: false } })

const route = useRoute()
const router = useRouter()
const appStore = useAppStore()
const permissionStore = usePermissionStore()
const { sidebarCollapsed } = storeToRefs(appStore)
const mobileMedia = window.matchMedia?.('(max-width: 900px)')
const isMobile = ref(mobileMedia?.matches ?? false)
const updateMobile = (event) => { isMobile.value = event.matches }
const navigate = (path) => { if (path && path !== route.path) router.push(path) }
onMounted(() => mobileMedia?.addEventListener('change', updateMobile))
onBeforeUnmount(() => mobileMedia?.removeEventListener('change', updateMobile))

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
  <aside class="app-sidebar" :aria-hidden="isMobile && !mobileMenuOpen" :inert="isMobile && !mobileMenuOpen">
    <div class="app-sidebar__brand">
      <div class="app-sidebar__logo">
        <img :src="'/quanta-logo.jpg'" alt="Quanta 社团 Logo" />
      </div>
      <div v-show="!sidebarCollapsed || isMobile" class="app-sidebar__brand-copy">
        <strong>Quanta</strong>
        <span>后台管理系统</span>
      </div>
    </div>

    <ElScrollbar class="app-sidebar__scrollbar">
      <ElMenu
        class="app-sidebar__menu"
        :default-active="route.path"
        :collapse="sidebarCollapsed && !isMobile"
        :collapse-transition="false"
        @select="navigate"
      >
        <div v-for="group in groupedRoutes" :key="group.title" class="menu-group">
          <p v-show="!sidebarCollapsed || isMobile" class="menu-group__title">{{ group.title }}</p>
          <SidebarMenuItem
            v-for="menuRoute in group.routes"
            :key="menuRoute.name"
            :route="menuRoute"
          />
        </div>
      </ElMenu>
    </ElScrollbar>

    <UserPanel :mobile="isMobile" />
  </aside>
</template>

