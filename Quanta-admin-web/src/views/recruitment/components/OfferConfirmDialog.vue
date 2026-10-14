<script setup>
import { computed } from 'vue'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  candidateName: { type: String, default: '' },
  department: { type: String, default: '' },
  options: { type: Array, default: () => [] },
  submitting: { type: Boolean, default: false },
})

const emit = defineEmits(['update:modelValue', 'confirm'])
const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value),
})
</script>

<template>
  <ElDialog v-model="visible" width="520" class="quanta-dialog offer-confirm-dialog" destroy-on-close>
    <template #header><strong>二面结果评定</strong></template>
    <p>确定向该同学({{ candidateName }})发送二面是否通过的录用通知？</p>
    <ElSelect
      :model-value="department"
      class="decision-dialog__department"
      disabled
      aria-label="录用志愿"
    >
      <ElOption
        v-for="option in options"
        :key="option.value"
        :label="option.label"
        :value="option.value"
      />
    </ElSelect>
    <template #footer>
      <ElButton
        size="small"
        class="offer-confirm-dialog__cancel"
        data-test="cancel-offer-step"
        :disabled="submitting"
        @click="visible = false"
      >
        取消
      </ElButton>
      <ElButton
        size="small"
        class="offer-confirm-dialog__confirm"
        :loading="submitting"
        data-test="confirm-offer-step"
        @click="emit('confirm')"
      >
        确认
      </ElButton>
    </template>
  </ElDialog>
</template>
