<script setup>
defineProps({ modelValue: { type: String, default: 'board' } })
const emit = defineEmits(['update:modelValue'])

const tabs = [
  { value: 'board', label: '招聘看板' },
  { value: '1', label: '一面' },
  { value: '2', label: '二面' },
]
</script>

<template>
  <div class="recruitment-tabs" role="tablist" aria-label="招新流程">
    <button
      v-for="tab in tabs"
      :key="tab.value"
      type="button"
      class="recruitment-tabs__item"
      :class="{ 'is-active': modelValue === tab.value }"
      :aria-selected="modelValue === tab.value"
      role="tab"
      @click="emit('update:modelValue', tab.value)"
    >
      {{ tab.label }}
    </button>
  </div>
</template>
