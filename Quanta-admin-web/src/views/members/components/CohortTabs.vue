<script setup>
defineProps({
  cohorts: { type: Array, default: () => [] },
  modelValue: { type: String, default: '' },
})

defineEmits(['update:modelValue'])
</script>

<template>
  <div class="cohort-tabs" role="tablist" aria-label="成员届次">
    <button
      v-for="cohort in cohorts"
      :key="cohort.value"
      type="button"
      role="tab"
      class="cohort-tabs__item"
      :class="{ 'is-active': cohort.value === modelValue }"
      :aria-selected="cohort.value === modelValue"
      @click="$emit('update:modelValue', cohort.value)"
    >
      {{ cohort.label }}<span v-if="cohort.isCurrent">（当前）</span>
    </button>
  </div>
</template>

<style scoped>
.cohort-tabs {
  display: flex;
  padding: 12px 18px;
  overflow-x: auto;
  border-top: 1px solid var(--quanta-border);
  border-bottom: 1px solid var(--quanta-border);
  gap: 10px;
}

.cohort-tabs__item {
  min-height: 30px;
  padding: 0 14px;
  color: var(--quanta-text-secondary);
  font-size: 12px;
  white-space: nowrap;
  cursor: pointer;
  background: #fff;
  border: 1px solid var(--quanta-border);
  border-radius: 999px;
  transition: 0.2s ease;
}

.cohort-tabs__item:hover {
  color: var(--quanta-primary-dark);
  border-color: #ffd18a;
}

.cohort-tabs__item.is-active {
  color: #fff;
  font-weight: 600;
  background: var(--quanta-primary);
  border-color: var(--quanta-primary);
  box-shadow: 0 4px 10px rgba(255, 173, 47, 0.2);
}
</style>
