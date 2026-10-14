<script setup>
import { computed } from 'vue'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  departmentName: { type: String, default: '' },
  submitting: { type: Boolean, default: false },
})
const emit = defineEmits(['update:modelValue', 'confirm'])
const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value),
})
</script>

<template>
  <ElDialog v-model="visible" width="440px" class="offer-acceptance-dialog" align-center :close-on-click-modal="!submitting" :show-close="!submitting">
    <template #header><strong>确认接受录用</strong></template>
    <p>你将接受 <strong>{{ departmentName }}</strong> 的录用。</p>
    <p class="offer-acceptance-dialog__warning">确认后只能加入该部门，另一志愿将不可再接受。</p>
    <template #footer>
      <ElButton :disabled="submitting" @click="visible = false">再想想</ElButton>
      <ElButton data-test="confirm-offer" type="primary" :loading="submitting" :disabled="submitting" @click="emit('confirm')">确认接受</ElButton>
    </template>
  </ElDialog>
</template>
