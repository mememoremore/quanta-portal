<script setup>
import { computed } from 'vue'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  candidateName: { type: String, default: '' },
  roundId: { type: Number, default: 1 },
  result: { type: String, default: 'PASS' },
  department: { type: String, default: '' },
  options: { type: Array, default: () => [] },
  submitting: { type: Boolean, default: false },
})
const emit = defineEmits(['update:modelValue', 'update:department', 'confirm'])
const visible = computed({ get: () => props.modelValue, set: (value) => emit('update:modelValue', value) })
const resultLabel = computed(() => (props.result === 'PASS' ? 'Pass' : 'Out'))
</script>

<template>
  <ElDialog v-model="visible" width="520" class="quanta-dialog decision-dialog" destroy-on-close>
    <template #header><strong>{{ roundId === 1 ? '一面' : '二面' }}结果评定</strong></template>
    <p>请确认 {{ candidateName }} 当前志愿轨道的评定结果为 {{ resultLabel }}。</p>
    <ElSelect
      :model-value="department"
      class="decision-dialog__department"
      aria-label="评定部门"
      @update:model-value="emit('update:department', $event)"
    >
      <ElOption v-for="option in options" :key="option.value" :label="option.label" :value="option.value" />
    </ElSelect>
    <template #footer>
      <ElButton size="small" :disabled="submitting" @click="visible = false">取消</ElButton>
      <ElButton
        type="success"
        size="small"
        :loading="submitting"
        data-test="confirm-decision"
        @click="emit('confirm')"
      >
        确认
      </ElButton>
    </template>
  </ElDialog>
</template>
