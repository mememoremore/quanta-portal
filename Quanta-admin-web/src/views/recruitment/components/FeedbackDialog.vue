<script setup>
import { computed } from 'vue'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  mode: { type: String, default: 'view' },
  candidateName: { type: String, default: '' },
  roundId: { type: Number, default: 1 },
  department: { type: String, default: '' },
  options: { type: Array, default: () => [] },
  content: { type: String, default: '' },
  evaluations: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
  submitting: { type: Boolean, default: false },
  canSwitch: { type: Boolean, default: false },
})
const emit = defineEmits([
  'update:modelValue',
  'update:department',
  'update:content',
  'submit',
])
const visible = computed({ get: () => props.modelValue, set: (value) => emit('update:modelValue', value) })
</script>

<template>
  <ElDialog v-model="visible" width="640" class="quanta-dialog feedback-dialog" destroy-on-close>
    <template #header><strong>{{ mode === 'edit' ? '编辑面评' : `${roundId === 1 ? '一面' : '二面'}面评` }} - {{ candidateName }}</strong></template>
    <div v-loading="loading">
      <ElSelect
        :model-value="department"
        class="feedback-dialog__department"
        :disabled="!canSwitch"
        aria-label="评审部门"
        @update:model-value="emit('update:department', $event)"
      >
        <ElOption v-for="option in options" :key="option.value" :label="option.label" :value="option.value" />
      </ElSelect>
      <div v-if="evaluations.length" class="feedback-dialog__history">
        <article v-for="evaluation in evaluations" :key="evaluation.evaluationId">
          <span>{{ evaluation.evaluatorUserName || evaluation.evaluatorName || evaluation.interviewerName }}</span><p>{{ evaluation.content }}</p>
        </article>
      </div>
      <ElInput
        v-if="mode === 'edit'"
        :model-value="content"
        type="textarea"
        :rows="7"
        maxlength="500"
        show-word-limit
        placeholder="请输入你的面评"
        @update:model-value="emit('update:content', $event)"
      />
    </div>
    <template v-if="mode === 'edit'" #footer>
      <ElButton class="feedback-dialog__cancel" size="small" @click="visible = false">取消</ElButton>
      <ElButton class="feedback-dialog__submit" type="success" size="small" :loading="submitting" @click="emit('submit')">提交</ElButton>
    </template>
  </ElDialog>
</template>
