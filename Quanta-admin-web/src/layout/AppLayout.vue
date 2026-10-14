<script setup>
import { computed } from 'vue'
import { storeToRefs } from 'pinia'
import { useAppStore } from '@/stores/app'
import AppSidebar from './components/AppSidebar.vue'
import AppHeader from './components/AppHeader.vue'
import AppMain from './components/AppMain.vue'

const appStore = useAppStore()
const { sidebarCollapsed } = storeToRefs(appStore)
const layoutClass = computed(() => ({ 'app-layout--collapsed': sidebarCollapsed.value }))
</script>

<template>
  <div class="app-layout" :class="layoutClass">
    <AppSidebar />
    <div class="app-layout__body">
      <AppHeader />
      <AppMain />
    </div>
  </div>
</template>

<style src="@/styles/layout.css"></style>

