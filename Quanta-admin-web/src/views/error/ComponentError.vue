<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const componentName = computed(() => route.meta?.title || route.name || '未知页面')
</script>

<template>
  <div class="component-error page-card">
    <div class="component-error__mark">!</div>
    <h2>页面组件尚未配置</h2>
    <p>{{ componentName }} 的组件映射不存在，请联系开发人员检查动态路由配置。</p>
  </div>
</template>

<style scoped>
.component-error {
  padding: 56px;
  text-align: center;
}

.component-error__mark {
  display: grid;
  width: 64px;
  height: 64px;
  margin: 0 auto 18px;
  color: white;
  font-size: 32px;
  font-weight: 700;
  background: var(--quanta-danger);
  border-radius: 50%;
  place-items: center;
}

p {
  color: var(--quanta-text-secondary);
}
</style>

