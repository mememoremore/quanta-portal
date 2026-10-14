import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

const SIDEBAR_KEY = 'quanta_sidebar_collapsed'

export const useAppStore = defineStore('app', () => {
  const sidebarCollapsed = ref(localStorage.getItem(SIDEBAR_KEY) === 'true')
  const sidebarWidth = computed(() => (sidebarCollapsed.value ? 78 : 255))

  function toggleSidebar() {
    sidebarCollapsed.value = !sidebarCollapsed.value
    localStorage.setItem(SIDEBAR_KEY, String(sidebarCollapsed.value))
  }

  return { sidebarCollapsed, sidebarWidth, toggleSidebar }
})

