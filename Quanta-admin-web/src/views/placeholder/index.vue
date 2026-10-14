<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { Tools } from '@element-plus/icons-vue'
import PermissionButton from '@/components/PermissionButton.vue'

const route = useRoute()
const title = computed(() => route.meta?.title || '当前功能')
const group = computed(() => route.meta?.group || '业务模块')
</script>

<template>
  <section class="placeholder-view page-card">
    <ElIcon class="placeholder-view__icon" :size="42"><Tools /></ElIcon>
    <span class="placeholder-view__group">{{ group }}</span>
    <h2>{{ title }}正在开发中</h2>
    <p>基础框架已经接入该动态路由，后续开发批次会直接替换当前占位页面。</p>
    <PermissionButton
      class="quanta-primary-button"
      type="primary"
      :permissions="['system:demo:manage']"
    >
      管理员权限演示
    </PermissionButton>
  </section>
</template>

<style scoped>
.placeholder-view {
  display: flex;
  min-height: 360px;
  padding: 56px 32px;
  text-align: center;
  align-items: center;
  flex-direction: column;
  justify-content: center;
}

.placeholder-view__icon {
  display: grid;
  width: 76px;
  height: 76px;
  margin-bottom: 18px;
  color: var(--quanta-primary);
  background: #fff7e8;
  border-radius: 22px;
  place-items: center;
}

.placeholder-view__group {
  color: var(--quanta-primary-dark);
  font-size: 13px;
  font-weight: 700;
}

h2 {
  margin: 10px 0 8px;
}

p {
  max-width: 560px;
  margin: 0 0 24px;
  color: var(--quanta-text-secondary);
}
</style>
