<script setup>
const quantaLogoUrl = '/quanta-logo-small.jpg'
defineProps({ size: { type: Number, default: 48 }, decorative: { type: Boolean, default: false } })
</script>

<template>
  <img class="quanta-brand-logo" :src="quantaLogoUrl" :alt="decorative ? '' : 'Quanta 社团 Logo'" :width="size" :height="size" :style="{ width: `${size}px`, height: `${size}px` }" draggable="false" />
</template>

<style scoped>
.quanta-brand-logo { display: block; max-width: 100%; object-fit: contain; border-radius: 22%; user-select: none; -webkit-user-drag: none; }
</style>
