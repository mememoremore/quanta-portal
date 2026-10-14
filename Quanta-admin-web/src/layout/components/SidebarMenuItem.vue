<script setup>
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

defineProps({
  route: {
    type: Object,
    required: true,
  },
})

function getIcon(name) {
  return ElementPlusIconsVue[name] || ElementPlusIconsVue.Menu
}
</script>

<template>
  <ElSubMenu v-if="route.children?.length" :index="route.path">
    <template #title>
      <ElIcon><component :is="getIcon(route.meta?.icon)" /></ElIcon>
      <span>{{ route.meta?.title }}</span>
    </template>
    <SidebarMenuItem v-for="child in route.children" :key="child.name" :route="child" />
  </ElSubMenu>

  <ElMenuItem v-else :index="route.path">
    <ElIcon><component :is="getIcon(route.meta?.icon)" /></ElIcon>
    <template #title>
      <span>{{ route.meta?.title }}</span>
    </template>
  </ElMenuItem>
</template>
