<script setup>
import { RouterLink } from 'vue-router'
import quantaLogoUrl from '@/assets/quanta-logo.jpg'

defineProps({
  link: { type: Boolean, default: false },
  tone: { type: String, default: 'light' },
})
</script>

<template>
  <component
    :is="link ? RouterLink : 'div'"
    :to="link ? '/' : undefined"
    class="login-brand"
    :class="`login-brand--${tone}`"
  >
    <img :src="quantaLogoUrl" alt="Quanta 社团 Logo" />
    <span><strong>Quanta</strong><small>DIGITAL CLUB</small></span>
  </component>
</template>

<style scoped>
.login-brand {
  display: flex;
  width: fit-content;
  align-items: center;
  gap: 13px;
  color: #fff;
  text-decoration: none;
  user-select: none;
}

.login-brand img {
  width: 52px;
  height: 52px;
  border-radius: 15px;
  object-fit: cover;
  -webkit-user-drag: none;
}

.login-brand span { display: grid; }
.login-brand strong { font-size: 24px; line-height: 1.2; }
.login-brand small { color: #9fa3aa; font-size: 10px; letter-spacing: .16em; }
.login-brand--dark strong { color: #202124; }
.login-brand--dark small { color: #70757d; }
</style>
