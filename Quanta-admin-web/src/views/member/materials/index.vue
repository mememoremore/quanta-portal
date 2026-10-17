<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { downloadPortalMaterial, getPortalMaterials } from '@/api/portal/content'
import { saveBlob } from '@/utils/download'
import { MATERIAL_CATEGORIES, materialCategoryLabel } from '@/utils/material-categories'

import { useMaterialLibrary } from '@/composables/useMaterialLibrary'
import { MATERIAL_TYPES } from '@/utils/material-types'

const tabs = [{ value: '', label: '全部' }, ...MATERIAL_CATEGORIES]
const { rows: materials, total, pageNum, pageSize, category, materialType, keyword, searchText, loading, errorMessage, load, chooseCategory, chooseType, clearSearch, search, goToPage } = useMaterialLibrary(getPortalMaterials, { allowAll: true })
const downloadingId = ref(null)

async function download(material) {
  downloadingId.value = material.materialId
  try {
    const result = await downloadPortalMaterial(material)
    saveBlob(result.blob, result.fileName)
    ElMessage.success('资料下载已开始')
  } catch (error) {
    ElMessage.error(error.message || '资料下载失败')
  } finally {
    downloadingId.value = null
  }
}

function fileSize(value) {
  const bytes = Number(value || 0)
  if (!bytes) return '未知大小'
  if (bytes < 1024 * 1024) return `${Math.max(1, Math.round(bytes / 1024))} KB`
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`
}

function fileType(name) {
  return String(name || '').split('.').pop()?.slice(0, 4).toUpperCase() || 'DOC'
}

onMounted(load)
</script>

<template>
  <section class="member-page member-materials">
    <header class="member-page__heading">
      <div><p>LEARNING CENTER</p><h1>学习资料</h1><span>浏览并下载 Quanta 四个方向共享的学习资源。</span></div>
      <form class="member-materials__search" @submit.prevent="search"><input v-model="searchText" type="search" placeholder="搜索当前分类的资料" aria-label="搜索资料名称" /><button class="member-primary-button">搜索</button></form>
    </header>

    <div class="member-materials__tabs" role="tablist" aria-label="学习资料分类">
      <button v-for="tab in tabs" :key="tab.value" type="button" role="tab" :aria-selected="category === tab.value" :class="{ 'is-active': category === tab.value }" @click="chooseCategory(tab.value)">{{ tab.label }}</button>
    </div>

    <div v-if="category" class="member-materials__types" role="group" aria-label="资料类型">
      <button type="button" :aria-pressed="!materialType && !keyword" @click="chooseType('')">全部类型</button>
      <button v-for="type in MATERIAL_TYPES" :key="type.value" type="button" :data-material-type="type.value" :aria-pressed="materialType === type.value && !keyword" @click="chooseType(type.value)">{{ type.label }}</button>
    </div>
    <div v-if="keyword" class="member-materials__search-state" role="status"><span>“{{ keyword }}” · {{ category ? '当前部门全部类型的搜索结果' : '全部部门的搜索结果' }}</span><button type="button" @click="clearSearch">清除搜索</button></div>

    <p v-if="errorMessage" class="member-feedback is-error" role="alert">{{ errorMessage }} <button type="button" @click="load">重试</button><button v-if="materialType" type="button" @click="chooseType('')">查看全部资料</button></p>
    <div v-if="loading" class="member-loading portal-card">正在加载学习资料…</div>
    <div v-else-if="materials.length && !errorMessage" class="member-materials__list portal-card">
      <article v-for="material in materials" :key="material.materialId">
        <div class="member-materials__icon">{{ fileType(material.fileName) }}</div>
        <div class="member-materials__details"><h2>{{ material.fileName }}</h2><p><span class="member-materials__category">{{ materialCategoryLabel(material.category) }}</span>{{ fileSize(material.fileSize) }} · {{ material.uploader }} · {{ material.uploadedAt || '时间待确认' }}</p></div>
        <button class="member-secondary-button" type="button" :disabled="downloadingId === material.materialId" @click="download(material)">{{ downloadingId === material.materialId ? '下载中…' : '下载' }}</button>
      </article>
    </div>
    <div v-else-if="!errorMessage" class="member-empty portal-card">这个分类暂时没有可下载的学习资料</div>

    <footer v-if="!loading && !errorMessage && total" class="member-materials__pagination">
      <span>共 {{ total }} 份 · 第 {{ pageNum }} / {{ Math.ceil(total / pageSize) }} 页</span>
      <div><button class="member-secondary-button" type="button" :disabled="pageNum <= 1" @click="goToPage(pageNum - 1)">上一页</button><button class="member-secondary-button" type="button" :disabled="pageNum * pageSize >= total" @click="goToPage(pageNum + 1)">下一页</button></div>
    </footer>
  </section>
</template>

<style src="../member.css"></style>
<style scoped>
.member-materials__types { display: flex; flex-wrap: wrap; gap: 8px; }
.member-materials__types button { padding: 8px 13px; border: 1px solid var(--portal-line); border-radius: 9px; background: #fff; color: var(--portal-muted); font: inherit; font-size: 12px; cursor: pointer; }
.member-materials__types button[aria-pressed=true] { border-color: #ffd2b3; background: var(--portal-orange-soft); color: var(--portal-orange-strong); }
.member-materials__search-state { display: flex; align-items: center; justify-content: space-between; gap: 12px; font-size: 12px; color: var(--portal-muted); overflow-wrap: anywhere; }
.member-materials__search-state button { flex-shrink: 0; padding: 7px 0; border: 0; background: transparent; color: var(--portal-orange-strong); font: inherit; cursor: pointer; }
@media (max-width: 700px) { .member-materials__types button { min-height: 44px; } }
@media (max-width: 480px) { .member-materials__search input { font-size: 16px; } .member-materials__list p { overflow-wrap: anywhere; line-height: 1.8; } }
</style>
<style scoped>
.member-materials__search { display: flex; width: min(430px, 100%); gap: 10px; }
.member-materials__search input { min-width: 0; padding: 11px 14px; border: 1px solid var(--portal-line); border-radius: 12px; background: #fff; font: inherit; flex: 1; }
.member-materials__tabs { display: flex; overflow-x: auto; width: fit-content; max-width: 100%; padding: 5px; border: 1px solid var(--portal-line); border-radius: 14px; background: #fff; gap: 4px; scrollbar-width: thin; }
.member-materials__tabs button { padding: 10px 18px; border: 0; border-radius: 10px; background: transparent; color: var(--portal-muted); font: inherit; font-size: 13px; font-weight: 750; white-space: nowrap; cursor: pointer; }
.member-materials__tabs button.is-active { background: var(--portal-orange-soft); color: var(--portal-orange-strong); }
.member-materials__list { padding-inline: 22px; }
.member-materials__list article { display: grid; padding: 19px 2px; border-bottom: 1px solid var(--portal-line); grid-template-columns: auto minmax(0, 1fr) auto; align-items: center; gap: 16px; }
.member-materials__list article:last-child { border-bottom: 0; }
.member-materials__icon { display: grid; width: 48px; height: 48px; border-radius: 14px; background: var(--portal-orange-soft); color: var(--portal-orange-strong); font-size: 10px; font-weight: 900; place-items: center; }
.member-materials__details { min-width: 0; }
.member-materials__list h2, .member-materials__list p { margin: 0; }
.member-materials__list h2 { overflow: hidden; font-size: 15px; text-overflow: ellipsis; white-space: nowrap; }
.member-materials__list p { margin-top: 5px; color: var(--portal-muted); font-size: 11px; }
.member-materials__category { margin-right: 8px; padding: 3px 7px; border-radius: 6px; background: var(--portal-orange-soft); color: var(--portal-orange-strong); font-weight: 750; }
.member-materials__pagination { display: flex; align-items: center; justify-content: space-between; gap: 12px; color: var(--portal-muted); font-size: 12px; }
.member-materials__pagination > div { display: flex; gap: 8px; }
@media (max-width: 700px) { .member-materials__search { width: 100%; } .member-materials__list { padding-inline: 16px; } }
@media (max-width: 480px) { .member-materials__search { display: grid; grid-template-columns: 1fr; } .member-materials__search button { width: 100%; } .member-materials__list article { grid-template-columns: auto minmax(0, 1fr); gap: 11px; } .member-materials__list article > button { width: 100%; grid-column: 1 / -1; } .member-materials__list h2 { white-space: normal; overflow-wrap: anywhere; } .member-materials__pagination { align-items: stretch; flex-direction: column; } .member-materials__pagination > div > button { flex: 1; } }
@media (max-width: 700px) {
  .member-materials__tabs { display: grid; width: 100%; grid-template-columns: repeat(5, minmax(0, 1fr)); }
  .member-materials__tabs button { min-width: 0; padding-inline: 4px; text-align: center; }
  .member-materials__types { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); }
  .member-materials__types button { min-width: 0; padding-inline: 4px; text-align: center; }
}
</style>
