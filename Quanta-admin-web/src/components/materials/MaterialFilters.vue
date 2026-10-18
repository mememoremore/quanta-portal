<script setup>
import { Search } from '@element-plus/icons-vue'
import { ElButton, ElInput } from 'element-plus'
import 'element-plus/theme-chalk/base.css'
import 'element-plus/theme-chalk/el-button.css'
import 'element-plus/theme-chalk/el-input.css'
import { MATERIAL_CATEGORIES } from '@/utils/material-categories'
import { MATERIAL_TYPES } from '@/utils/material-types'

defineProps({ category: { type: String, default: 'FRONTEND' }, materialType: { type: String, default: '' }, searchText: { type: String, default: '' }, keyword: { type: String, default: '' }, disabled: Boolean })
const emit = defineEmits(['category', 'type', 'update:searchText', 'search', 'clear'])
</script>

<template>
  <div class="material-filters">
    <div class="material-filters__top">
      <div class="material-filters__departments" role="group" aria-label="资料所属部门">
        <button v-for="item in MATERIAL_CATEGORIES" :key="item.value" type="button" :disabled="disabled" :aria-pressed="category === item.value" @click="emit('category', item.value)">{{ item.label }}</button>
      </div>
      <form class="material-filters__search" @submit.prevent="!disabled && emit('search')">
        <ElInput :model-value="searchText" :disabled="disabled" clearable :prefix-icon="Search" placeholder="搜索当前部门全部资料" aria-label="搜索资料名称" @update:model-value="emit('update:searchText', $event)" />
        <ElButton type="primary" native-type="submit" :disabled="disabled">搜索</ElButton>
      </form>
    </div>
    <div class="material-filters__types" role="group" aria-label="资料类型">
      <span>资料类型</span>
      <div>
        <button type="button" class="material-filters__reset" :disabled="disabled" :aria-pressed="!materialType && !keyword" @click="emit('type', '')">全部资料</button>
        <button v-for="item in MATERIAL_TYPES" :key="item.value" type="button" :disabled="disabled" :aria-pressed="materialType === item.value && !keyword" @click="emit('type', materialType === item.value ? '' : item.value)">{{ item.label }}</button>
      </div>
    </div>
    <div v-if="keyword" class="material-filters__search-state" role="status">
      <span>“{{ keyword }}” · 当前部门全部类型的搜索结果</span>
      <button type="button" :disabled="disabled" @click="emit('clear')">清除搜索</button>
    </div>
  </div>
</template>

<style scoped>
.material-filters { --material-line: #e9ecf1; --el-color-primary: #ff6700; --el-color-primary-light-3: #ff954d; --el-color-primary-light-9: #fff1e7; --el-color-primary-dark-2: #df5900; color: #22272f; }
.material-filters__top { display: flex; padding: 20px 22px; align-items: center; justify-content: space-between; gap: 16px; border-bottom: 1px solid var(--material-line); }
.material-filters__departments { display: flex; gap: 5px; }
.material-filters__departments button { padding: 10px 15px; border: 0; border-radius: 9px; background: transparent; color: #768191; font: inherit; font-size: 13px; cursor: pointer; }
.material-filters__departments button[aria-pressed=true] { color: #e95d00; background: #fff1e7; font-weight: 650; }
.material-filters__search { display: flex; width: min(370px, 50%); gap: 9px; }
.material-filters__search :deep(.el-input) { min-width: 0; }
.material-filters__types { display: flex; padding: 21px 22px; align-items: center; gap: 17px; flex-wrap: wrap; }
.material-filters__types > span { color: #7e8796; font-size: 12px; }
.material-filters__types > div { display: flex; gap: 8px; }
.material-filters__types button { padding: 9px 17px; border: 1px solid var(--material-line); border-radius: 9px; background: #fff; color: #778292; font: inherit; font-size: 12px; cursor: pointer; }
.material-filters__types button[aria-pressed=true] { border-color: #ffcea9; background: #fff1e7; color: #e95d00; font-weight: 650; }
.material-filters button:disabled { cursor: not-allowed; opacity: .6; }
.material-filters button:focus-visible { outline: 2px solid #ff6700; outline-offset: 2px; }
.material-filters__search-state { display: flex; padding: 10px 22px 16px; gap: 12px; justify-content: space-between; align-items: center; color: #778292; font-size: 12px; overflow-wrap: anywhere; }
.material-filters__search-state button { flex-shrink: 0; padding: 5px; border: 0; background: transparent; color: #e95d00; font: inherit; font-size: 12px; cursor: pointer; }
@media (max-width: 700px) {
  .material-filters__top { padding: 16px; flex-direction: column; align-items: stretch; gap: 14px; }
  .material-filters__departments { justify-content: space-between; }
  .material-filters__departments button { min-height: 42px; flex: 1; padding-inline: 8px; }
  .material-filters__search { width: 100%; }
  .material-filters__search :deep(input) { font-size: 16px; }
  .material-filters__types { padding: 17px 16px; gap: 10px; }
  .material-filters__types > span { width: 100%; }
  .material-filters__types > div { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); width: 100%; gap: 7px; }
  .material-filters__types > div button { padding: 10px 3px; min-height: 42px; flex: 1; }
  .material-filters__search-state { padding-inline: 16px; }
}
</style>
