<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<template>
  <main class="error-page">
    <div class="error-page__logo">Q</div>
    <p class="error-page__code">403</p>
    <h1>暂时没有访问权限</h1>
    <p>当前账号无法打开这个页面。如果你认为这是配置错误，请联系管理员。</p>
    <ElButton class="quanta-primary-button" type="primary" @click="router.push('/admin/dashboard')">
      返回控制台
    </ElButton>
  </main>
</template>

<style scoped src="./error.css"></style>
