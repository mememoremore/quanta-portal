<script setup>
import { computed, ref, watch } from 'vue'
import { storeToRefs } from 'pinia'
import { useRoute } from 'vue-router'
import { useAppStore } from '@/stores/app'
import AppSidebar from './components/AppSidebar.vue'
import AppHeader from './components/AppHeader.vue'
import AppMain from './components/AppMain.vue'

const appStore = useAppStore()
const route = useRoute()
const { sidebarCollapsed } = storeToRefs(appStore)
const mobileMenuOpen = ref(false)
const layoutClass = computed(() => ({
  'app-layout--collapsed': sidebarCollapsed.value,
  'app-layout--mobile-menu-open': mobileMenuOpen.value,
}))

watch(() => route.fullPath, () => { mobileMenuOpen.value = false })
</script>

<template>
  <div class="app-layout" :class="layoutClass" @keydown.esc="mobileMenuOpen = false">
    <button v-if="mobileMenuOpen" class="app-layout__backdrop" type="button" aria-label="关闭导航菜单" @click="mobileMenuOpen = false" />
    <AppSidebar :mobile-menu-open="mobileMenuOpen" />
    <div class="app-layout__body">
      <AppHeader :mobile-menu-open="mobileMenuOpen" @toggle-mobile-menu="mobileMenuOpen = !mobileMenuOpen" />
      <AppMain />
    </div>
  </div>
</template>

<style src="@/styles/layout.css"></style>
<style src="@/styles/admin-mobile.css"></style>

