<script setup>
import { computed, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import PortalHeader from './components/PortalHeader.vue'

const route = useRoute()
const audience = computed(() => route.meta.portalAudience === 'freshman' ? 'freshman' : 'member')

onMounted(() => document.body.classList.add('portal-scroll-active'))
onUnmounted(() => document.body.classList.remove('portal-scroll-active'))
</script>

<template>
  <div class="portal-layout">
    <PortalHeader :audience="audience" />
    <main class="portal-layout__main portal-container">
      <RouterView />
    </main>
  </div>
</template>
