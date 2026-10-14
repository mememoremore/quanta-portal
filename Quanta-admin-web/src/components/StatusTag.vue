<script setup>
import { computed } from 'vue'

const props = defineProps({
  type: { type: String, default: 'info' },
})

const className = computed(() => `status-tag--${props.type}`)
</script>

<template>
  <span class="status-tag" :class="className"><slot /></span>
</template>

<style scoped>
.status-tag {
  display: inline-flex;
  padding: 4px 10px;
  color: #59708c;
  font-size: 12px;
  background: #f0f4f8;
  border-radius: 999px;
  align-items: center;
}

.status-tag--success {
  color: #159b56;
  background: #eaf9f0;
}

.status-tag--warning {
  color: #d98713;
  background: #fff6e6;
}

.status-tag--danger {
  color: #d9363e;
  background: #fff0f0;
}

.status-tag--primary {
  color: #3f76d3;
  background: #edf4ff;
}
</style>
