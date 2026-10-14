<script setup>
import { computed } from 'vue'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  member: { type: Object, default: null },
  submitting: { type: Boolean, default: false },
})

const emit = defineEmits(['update:modelValue', 'confirm'])

const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value),
})

function close() {
  if (!props.submitting) visible.value = false
}
</script>

<template>
  <ElDialog
    v-model="visible"
    width="520"
    class="quanta-dialog retain-confirm-dialog"
    :close-on-click-modal="!submitting"
    :close-on-press-escape="!submitting"
    :show-close="!submitting"
    destroy-on-close
  >
    <template #header><strong>成员留任确认</strong></template>
    <p>确认 {{ member?.name }} 留任并同步到下一届名单吗？</p>
    <template #footer>
      <ElButton size="small" :disabled="submitting" @click="close">取消</ElButton>
      <ElButton
        size="small"
        class="retain-confirm-dialog__confirm"
        :loading="submitting"
        @click="emit('confirm')"
      >
        确认留任
      </ElButton>
    </template>
  </ElDialog>
</template>
