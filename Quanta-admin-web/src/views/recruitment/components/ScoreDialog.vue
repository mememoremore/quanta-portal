<script setup>
import { computed, ref, watch } from 'vue'
const props = defineProps({ modelValue: { type: Boolean, default: false }, candidateName: { type: String, default: '' }, department: { type: String, default: '' }, options: { type: Array, default: () => [] }, score: { type: Number, default: null }, canEdit: { type: Boolean, default: false }, updatedBy: { type: String, default: '' }, updatedTime: { type: String, default: '' }, submitting: { type: Boolean, default: false } })
const emit = defineEmits(['update:modelValue', 'update:department', 'submit'])
const visible = computed({ get: () => props.modelValue, set: (value) => emit('update:modelValue', value) })
const draft = ref(null)
watch(() => [props.modelValue, props.score], () => { draft.value = props.score }, { immediate: true })
</script>
<template>
  <ElDialog v-model="visible" width="520" class="quanta-dialog score-dialog" destroy-on-close>
    <template #header><strong>面试评分 · {{ candidateName }}</strong></template>
    <ElSelect :model-value="department" aria-label="评分部门" @update:model-value="emit('update:department', $event)"><ElOption v-for="option in options" :key="option.value" :label="option.label" :value="option.value" /></ElSelect>
    <div class="score-dialog__field"><span>二面分数</span><ElInputNumber v-model="draft" aria-label="二面分数" :min="0" :max="100" :precision="0" :disabled="!canEdit" /><em>/ 100</em></div>
    <p v-if="updatedBy || updatedTime" class="score-dialog__meta">最后修改：{{ updatedBy || '—' }} {{ updatedTime }}</p>
    <p v-if="!canEdit" class="score-dialog__hint">你可以查看该部门分数，但只能修改自己部门的分数。</p>
    <template #footer><ElButton @click="visible = false">关闭</ElButton><ElButton v-if="canEdit" type="primary" class="quanta-primary-button" :loading="submitting" :disabled="draft == null" @click="emit('submit', draft)">保存评分</ElButton></template>
  </ElDialog>
</template>
