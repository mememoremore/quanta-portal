<script setup>
defineProps({
  title: { type: String, default: '' },
})
</script>

<template>
  <section class="page-card">
    <header v-if="title || $slots.header" class="page-card__header">
      <slot name="header"><h2>{{ title }}</h2></slot>
      <slot name="actions" />
    </header>
    <div class="page-card__body"><slot /></div>
  </section>
</template>

<style scoped>
.page-card__header {
  display: flex;
  min-height: 62px;
  padding: 0 22px;
  border-bottom: 1px solid var(--quanta-border);
  align-items: center;
  justify-content: space-between;
}

.page-card__header h2 {
  margin: 0;
  font-size: 16px;
}

.page-card__body {
  padding: 22px;
}
</style>

