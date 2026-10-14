<script setup>
import { useRoute } from 'vue-router'

const route = useRoute()
</script>

<template>
  <section class="portal-placeholder">
    <p>QUANTA PORTAL</p>
    <h1>{{ route.meta.title }}</h1>
    <span>页面内容正在迁移中</span>
  </section>
</template>

<style scoped>
.portal-placeholder {
  padding: 48px;
  border: 1px solid #ececef;
  border-radius: 24px;
  background: #fff;
  box-shadow: 0 18px 50px rgb(31 41 55 / 6%);
}

.portal-placeholder p {
  color: #ff6600;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.14em;
}

.portal-placeholder h1 {
  margin: 8px 0 12px;
  font-size: 36px;
}

.portal-placeholder span {
  color: #6b7280;
}
</style>
