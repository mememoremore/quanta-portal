<script setup>
import { computed, defineAsyncComponent, onBeforeUnmount, onErrorCaptured, ref, shallowRef, watch } from 'vue'
import { downloadPortalMaterial } from '@/api/portal/content'
import { checkPreviewSize, materialPreviewKind, previewImageType, readPreviewBytes, readPreviewText, renderMaterialMarkdown } from '@/utils/material-preview'

const props = defineProps({
  material: { type: Object, required: true },
  downloading: Boolean,
})
const emit = defineEmits(['download'])
const PdfPreview = defineAsyncComponent({
  loader: () => import('./MaterialPdfPreview.vue'),
})
const kind = computed(() => materialPreviewKind(props.material.fileName))
const loading = ref(false), errorMessage = ref(''), text = ref(''), imageUrl = ref('')
const pdfBlob = shallowRef(null)
const html = computed(() => kind.value === 'markdown' ? renderMaterialMarkdown(text.value) : '')
let sequence = 0, controller

function clear() {
  controller?.abort()
  if (imageUrl.value) URL.revokeObjectURL(imageUrl.value)
  imageUrl.value = ''
  pdfBlob.value = null
  text.value = ''
}

async function load() {
  const current = ++sequence
  clear()
  errorMessage.value = ''
  loading.value = false
  const type = kind.value
  if (type === 'unsupported') return
  const selected = props.material
  controller = new AbortController()
  loading.value = true
  try {
    checkPreviewSize(selected.fileSize, type)
    // Use the canonical member-protected endpoint, not an upload-supplied URL.
    const result = await downloadPortalMaterial({ ...selected, downloadUrl: `/qt/materials/${selected.materialId}/download` }, {
      signal: controller.signal, timeout: 120000,
      onDownloadProgress(progress) {
        try { checkPreviewSize(progress.total || progress.loaded, type) }
        catch (error) { if (current === sequence) { errorMessage.value = error.message; controller.abort() } }
      },
    })
    if (current !== sequence) return
    checkPreviewSize(result.blob.size, type)
    if (type === 'text' || type === 'markdown') {
      const content = await readPreviewText(result.blob)
      if (current === sequence) text.value = content
    } else {
      const bytes = new Uint8Array(await readPreviewBytes(result.blob.slice(0, 1024)))
      if (current !== sequence) return
      if (type === 'image') {
        const mime = previewImageType(bytes)
        if (!mime) throw new Error('图片内容与支持的格式不符，请下载后查看。')
        imageUrl.value = URL.createObjectURL(new Blob([result.blob], { type: mime }))
      } else {
        if (!new TextDecoder('ascii').decode(bytes).includes('%PDF-')) throw new Error('文件不是有效的 PDF，请下载后查看。')
        pdfBlob.value = result.blob
      }
    }
  } catch (error) {
    if (current === sequence && !controller.signal.aborted) errorMessage.value = error.message || '预览加载失败，请重试或下载查看。'
  } finally {
    if (current === sequence) loading.value = false
  }
}

function imageFailed() {
  errorMessage.value = '图片无法显示，请下载后查看。'
  if (imageUrl.value) URL.revokeObjectURL(imageUrl.value)
  imageUrl.value = ''
}

watch(() => props.material, load, { immediate: true })
onErrorCaptured(() => {
  errorMessage.value = '阅读组件加载失败，请重试或下载后查看。'
  return false
})
onBeforeUnmount(() => { sequence += 1; clear() })
</script>

<template>
  <section class="material-preview" aria-label="资料预览">
    <header class="material-preview__header">
      <div class="material-preview__heading"><span>资料预览</span><h2>{{ material.fileName }}</h2></div>
      <button type="button" data-preview-download :disabled="downloading" @click="emit('download', material)">{{ downloading ? '下载中…' : '下载' }}</button>
    </header>
    <div class="material-preview__body" :aria-busy="loading">
      <div v-if="loading" class="material-preview__feedback" role="status">正在加载资料…</div>
      <div v-else-if="errorMessage" class="material-preview__feedback" role="alert">
        <h3>预览暂不可用</h3><p>{{ errorMessage }}</p>
        <button type="button" data-preview-retry @click="load">重新加载</button>
      </div>
      <div v-else-if="kind === 'unsupported'" class="material-preview__feedback">
        <h3>暂不支持在线预览</h3><p>这类文件请下载后使用对应软件查看。</p>
      </div>
      <!-- markdown-it disables HTML, validates links and never loads remote images. -->
      <!-- eslint-disable-next-line vue/no-v-html -->
      <div v-else-if="kind === 'markdown'" class="material-preview__markdown" v-html="html"></div>
      <pre v-else-if="kind === 'text'" class="material-preview__text">{{ text }}</pre>
      <div v-else-if="imageUrl" class="material-preview__image"><img :src="imageUrl" :alt="material.fileName" @error="imageFailed" /></div>
      <Suspense v-else-if="pdfBlob">
        <PdfPreview :blob="pdfBlob" />
        <template #fallback><p class="material-preview__feedback" role="status">正在加载 PDF 阅读器…</p></template>
      </Suspense>
    </div>
  </section>
</template>

<style>
.material-preview { overflow: hidden; border: 1px solid #e9ebef; border-radius: 18px; background: #fff; }
.material-preview__header { display: flex; padding: 20px 24px; border-bottom: 1px solid #e9ebef; align-items: center; justify-content: space-between; gap: 16px; }
.material-preview__heading { min-width: 0; }
.material-preview__heading span { color: #8b94a4; font-size: 11px; }
.material-preview__heading h2 { margin: 5px 0 0; overflow: hidden; font-size: 16px; line-height: 1.5; font-weight: 750; color: #202226; text-overflow: ellipsis; white-space: nowrap; }
.material-preview__header button, .material-preview__feedback button { flex-shrink: 0; min-height: 40px; padding: 8px 16px; border: 1px solid #ffd2b3; border-radius: 10px; background: #fff1e8; color: #e65c00; font: inherit; font-size: 13px; cursor: pointer; }
.material-preview__header button:disabled { opacity: .5; cursor: wait; }
.material-preview__body { height: max(420px, calc(100vh - 280px)); height: max(420px, calc(100dvh - 280px)); overflow: auto; overscroll-behavior: contain; color: #343a44; background: #fff; }
.material-preview__feedback { display: flex; min-height: 240px; margin: 0; padding: 40px 24px; flex-direction: column; align-items: center; justify-content: center; gap: 12px; text-align: center; color: #777f8c; font-size: 14px; }
.material-preview__feedback h3, .material-preview__feedback p { margin: 0; line-height: 1.8; }
.material-preview__feedback h3 { color: #343a44; font-size: 18px; }
.material-preview__markdown { max-width: 850px; margin: 0 auto; padding: 30px 36px 48px; font-size: 15px; line-height: 1.9; overflow-wrap: anywhere; }
.material-preview__markdown h1 { font-size: 28px; line-height: 1.5; }
.material-preview__markdown h2 { font-size: 23px; }
.material-preview__markdown h3 { font-size: 19px; }
.material-preview__markdown a { color: #e65c00; text-decoration: underline; }
.material-preview__markdown pre { padding: 18px; border-radius: 12px; background: #f4f5f7; white-space: pre-wrap; overflow-wrap: anywhere; }
.material-preview__markdown pre code { font-size: 13px; }
.material-preview__markdown blockquote { margin-inline: 0; padding-left: 16px; border-left: 3px solid #ff6600; color: #777f8c; }
.material-preview__markdown table { display: block; max-width: 100%; overflow-x: auto; border-collapse: collapse; }
.material-preview__markdown td, .material-preview__markdown th { padding: 8px 12px; border: 1px solid #e9ebef; }
.material-preview__text { margin: 0; padding: 28px; font: 14px/1.9 ui-monospace, SFMono-Regular, Consolas, monospace; white-space: pre-wrap; overflow-wrap: anywhere; }
.material-preview__image { display: grid; min-height: 100%; padding: 24px; place-items: center; background: #f4f5f7; }
.material-preview__image img { display: block; max-width: 100%; max-height: 68vh; object-fit: contain; }
@media (max-width: 700px) {
  .material-preview__header { padding: 14px 16px; gap: 8px; }
  .material-preview__header button { min-height: 44px; padding-inline: 12px; }
  .material-preview__body { height: max(360px, calc(100vh - 220px)); height: max(360px, calc(100dvh - 220px)); padding-bottom: env(safe-area-inset-bottom, 0px); }
  .material-preview__heading h2 { font-size: 14px; }
  .material-preview__markdown { padding: 22px 18px 36px; font-size: 15px; }
  .material-preview__markdown h1 { font-size: 25px; }
  .material-preview__text { padding: 20px 16px; }
  .material-preview__image { padding: 12px; }
}
</style>
