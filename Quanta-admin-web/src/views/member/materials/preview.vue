<script setup>
import { onBeforeUnmount, ref, shallowRef, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import MaterialPreview from '@/components/materials/MaterialPreview.vue'
import { downloadPortalMaterial, getPortalMaterial } from '@/api/portal/content'
import { saveBlob } from '@/utils/download'

const route = useRoute()
const material = shallowRef(null), loading = ref(false), errorMessage = ref(''), downloading = ref(false)
let sequence = 0, controller

async function load() {
  const current = ++sequence
  controller?.abort()
  controller = new AbortController()
  material.value = null
  loading.value = true
  errorMessage.value = ''
  try {
    const result = await getPortalMaterial(route.params.materialId, { signal: controller.signal })
    if (current !== sequence) return
    material.value = result
    document.title = `${result.fileName} - 资料预览 - Quanta`
  } catch (error) {
    if (current === sequence && !controller.signal.aborted) errorMessage.value = error.message || '资料加载失败'
  } finally {
    if (current === sequence) loading.value = false
  }
}

async function download(selected) {
  if (downloading.value) return
  downloading.value = true
  try {
    const result = await downloadPortalMaterial(selected)
    saveBlob(result.blob, result.fileName)
    ElMessage.success('资料下载已开始')
  } catch (error) { ElMessage.error(error.message || '资料下载失败') }
  finally { downloading.value = false }
}

watch(() => route.params.materialId, load, { immediate: true })
onBeforeUnmount(() => { sequence += 1; controller?.abort() })
</script>

<template>
  <section class="member-material-preview-page">
    <RouterLink class="member-material-preview-page__back" to="/member/materials">← 学习资料</RouterLink>
    <p v-if="loading" class="member-feedback" role="status">正在加载资料信息…</p>
    <div v-else-if="errorMessage" class="member-feedback is-error" role="alert">{{ errorMessage }} <button type="button" @click="load">重试</button></div>
    <MaterialPreview v-else-if="material" :key="material.materialId" :material="material" :downloading="downloading" @download="download" />
  </section>
</template>

<style scoped>
.member-material-preview-page { min-width: 0; }
.member-material-preview-page__back { display: inline-block; margin-bottom: 16px; color: var(--portal-muted); font-size: 13px; text-decoration: none; }
.member-material-preview-page__back:hover { color: var(--portal-orange-strong); }
</style>
