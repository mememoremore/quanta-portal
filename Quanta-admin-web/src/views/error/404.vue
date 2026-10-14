<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<template>
  <main class="error-page">
    <div class="error-page__logo">Q</div>
    <p class="error-page__code">404</p>
    <h1>页面走丢了</h1>
    <p>你访问的地址不存在，或者对应功能还没有加入系统。</p>
    <ElButton class="quanta-primary-button" type="primary" @click="router.push('/admin/dashboard')">
      返回控制台
    </ElButton>
  </main>
</template>

<style scoped src="./error.css"></style>
