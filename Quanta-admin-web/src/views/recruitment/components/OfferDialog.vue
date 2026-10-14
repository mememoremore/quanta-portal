<script setup>
import { computed, ref, watch } from 'vue'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  candidate: { type: Object, default: null },
  department: { type: String, default: '' },
  options: { type: Array, default: () => [] },
  decision: { type: String, default: 'PASS' },
  content: { type: String, default: '' },
  evaluations: { type: Array, default: () => [] },
  evaluationsLoading: { type: Boolean, default: false },
  submitting: { type: Boolean, default: false },
})
const emit = defineEmits(['update:modelValue', 'update:decision', 'update:content', 'submit'])
const visible = computed({ get: () => props.modelValue, set: (value) => emit('update:modelValue', value) })
const activeTab = ref('decision')

watch(
  () => props.modelValue,
  (value) => {
    if (value) activeTab.value = 'decision'
  },
)
</script>

<template>
  <ElDialog v-model="visible" width="640" class="quanta-dialog offer-dialog" destroy-on-close>
    <template #header>
      <nav class="offer-dialog__tabs" aria-label="二面录用操作">
        <button
          type="button"
          class="offer-dialog__tab"
          :class="{ 'is-active': activeTab === 'decision' }"
          data-test="offer-tab-decision"
          @click="activeTab = 'decision'"
        >
          是否录用
        </button>
        <button
          type="button"
          class="offer-dialog__tab"
          :class="{ 'is-active': activeTab === 'feedback' }"
          data-test="offer-tab-feedback"
          @click="activeTab = 'feedback'"
        >
          查看面评
        </button>
      </nav>
    </template>

    <div v-if="activeTab === 'decision'" class="offer-dialog__decision-panel">
      <div class="offer-dialog__decisions">
        <button
          type="button"
          class="offer-dialog__decision"
          data-test="offer-pass"
          :class="{ 'is-pass': decision === 'PASS' }"
          @click="emit('update:decision', 'PASS')"
        >
          Pass
        </button>
        <button
          type="button"
          class="offer-dialog__decision"
          data-test="offer-out"
          :class="{ 'is-out': decision === 'FAIL' }"
          @click="emit('update:decision', 'FAIL')"
        >
          Out
        </button>
      </div>
      <label>接收人</label>
      <ElInput :model-value="candidate?.realName || candidate?.name" disabled />
      <label>录用志愿</label>
      <ElSelect :model-value="department" disabled aria-label="录用志愿">
        <ElOption
          v-for="option in options"
          :key="option.value"
          :label="option.label"
          :value="option.value"
        />
      </ElSelect>
      <label>通知内容</label>
      <ElInput
        :model-value="content"
        type="textarea"
        :rows="6"
        @update:model-value="emit('update:content', $event)"
      />
    </div>

    <div v-else v-loading="evaluationsLoading" class="offer-dialog__feedback-panel">
      <div v-if="evaluations.length" class="offer-dialog__evaluations">
        <article
          v-for="evaluation in evaluations"
          :key="evaluation.evaluationId"
          class="offer-dialog__evaluation"
        >
          <span>{{ evaluation.evaluatorUserName || evaluation.evaluatorName || evaluation.interviewerName }}</span>
          <p>{{ evaluation.content }}</p>
        </article>
      </div>
      <ElEmpty
        v-else-if="!evaluationsLoading"
        description="暂无二面面评"
        :image-size="52"
      />
    </div>
    <template #footer>
      <template v-if="activeTab === 'decision'">
        <ElButton size="small" @click="visible = false">取消</ElButton>
        <ElButton
          type="primary"
          size="small"
          class="quanta-primary-button"
          data-test="submit-offer"
          :loading="submitting"
          @click="emit('submit')"
        >
          发送通知
        </ElButton>
      </template>
    </template>
  </ElDialog>
</template>
