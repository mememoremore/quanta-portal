<script setup>
defineProps({
  deployment: { type: Object, default: null },
  open: { type: Boolean, default: false },
})
const emit = defineEmits(['close'])
</script>

<template>
  <div v-if="open && deployment" class="pages-log-dialog" role="dialog" aria-modal="true" data-testid="pages-log-dialog">
    <div class="pages-log-dialog__panel portal-card">
      <header>
        <h3>部署 #{{ deployment.deploymentId }} 阶段日志</h3>
        <button class="member-secondary-button" type="button" data-testid="pages-log-close" @click="emit('close')">关闭</button>
      </header>
      <!-- 纯文本渲染，绝不 v-html：日志内容来自后端脱敏文本 -->
      <pre data-testid="pages-log-text">{{ deployment.logText || deployment.errorMessage || '暂无日志' }}</pre>
    </div>
  </div>
</template>
