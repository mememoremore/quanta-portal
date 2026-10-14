<script setup>
import { reactive, watch } from 'vue'

const props = defineProps({ modelValue: { type: Object, required: true } })
const emit = defineEmits(['update:modelValue', 'save'])
const form = reactive({ ...props.modelValue })

watch(() => props.modelValue, (value) => Object.assign(form, value || {}), { deep: true })
watch(form, (value) => emit('update:modelValue', { ...value }), { deep: true })
</script>

<template>
  <form class="business-card-editor" @submit.prevent="emit('save', { ...form })">
    <label>姓名<input v-model.trim="form.name" maxlength="120" /></label>
    <label>职位<input v-model.trim="form.title" maxlength="120" /></label>
    <label>部门<input v-model.trim="form.department" maxlength="120" /></label>
    <label>联系电话<input v-model.trim="form.phone" maxlength="40" /></label>
    <label>邮箱<input v-model.trim="form.email" maxlength="160" type="email" /></label>
    <label class="business-card-editor__wide">
      个人简介
      <textarea v-model.trim="form.bio" maxlength="800" rows="5" />
      <small>{{ form.bio?.length || 0 }} / 800</small>
    </label>
    <button class="member-primary-button" type="submit">保存到当前浏览器</button>
  </form>
</template>

<style scoped>
.business-card-editor { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.business-card-editor label { display: grid; gap: 7px; color: var(--portal-ink-soft); font-size: 13px; font-weight: 700; }
.business-card-editor input, .business-card-editor textarea { box-sizing: border-box; width: 100%; padding: 12px 14px; border: 1px solid var(--portal-line); border-radius: 12px; background: #fff; color: var(--portal-ink); font: inherit; resize: vertical; }
.business-card-editor input:focus, .business-card-editor textarea:focus { border-color: var(--portal-orange); outline: 3px solid rgb(255 103 0 / 12%); }
.business-card-editor__wide { grid-column: 1 / -1; }
.business-card-editor small { justify-self: end; color: var(--portal-muted); font-weight: 500; }
@media (max-width: 700px) { .business-card-editor { grid-template-columns: 1fr; } .business-card-editor__wide { grid-column: auto; } .business-card-editor button { width: 100%; } }
</style>
