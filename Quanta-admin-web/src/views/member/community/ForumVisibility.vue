<script setup>
defineProps({ visibility: { type: String, default: 'MEMBERS' } })
</script>

<template>
  <span v-if="visibility === 'MANAGERS'" class="community-visibility-badge"><svg viewBox="0 0 24 24" aria-hidden="true"><rect x="5" y="10" width="14" height="11" rx="2" /><path d="M8 10V7a4 4 0 0 1 8 0v3M12 14v3" /></svg>经理及以上可见</span>
</template>

<style scoped>
.community-visibility-badge { display: inline-flex; gap: 5px; align-items: center; color: #8d97a4; font-size: 12px; font-weight: 400; margin-left: 10px; }
.community-visibility-badge svg { width: 14px; height: 14px; fill: none; stroke: currentColor; stroke-width: 1.6; }
</style>
