<script setup>
import { nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { getDocument, GlobalWorkerOptions } from 'pdfjs-dist/legacy/build/pdf.mjs'
import workerUrl from 'pdfjs-dist/legacy/build/pdf.worker.mjs?url'
import { LocalPdfDataFactory } from './pdf-assets'
import { readPreviewBytes } from '@/utils/material-preview'

const props = defineProps({ blob: { type: Blob, required: true } })
const canvas = ref(null), container = ref(null)
const pageNumber = ref(1), pageCount = ref(0), loading = ref(true), errorMessage = ref('')
let documentTask, pdf, renderTask, observer, stopped = false, renderSequence = 0
let renderedWidth = 0
GlobalWorkerOptions.workerSrc = workerUrl

async function renderPage() {
  if (!pdf || stopped) return
  const current = ++renderSequence
  renderTask?.cancel()
  loading.value = true
  errorMessage.value = ''
  try {
    const page = await pdf.getPage(pageNumber.value)
    if (stopped || current !== renderSequence) return
    const natural = page.getViewport({ scale: 1 })
    const width = Math.max(1, container.value.clientWidth - 24)
    renderedWidth = container.value.clientWidth
    const scale = Math.min(width / natural.width, 2)
    const viewport = page.getViewport({ scale })
    // Render only one page; bound backing pixels even for malicious page sizes.
    const ratio = Math.min(window.devicePixelRatio || 1, 2, Math.sqrt(4_000_000 / (viewport.width * viewport.height)))
    await nextTick()
    const element = canvas.value
    element.width = Math.max(1, Math.floor(viewport.width * ratio))
    element.height = Math.max(1, Math.floor(viewport.height * ratio))
    element.style.width = `${viewport.width}px`
    element.style.height = `${viewport.height}px`
    renderTask = page.render({
      canvasContext: element.getContext('2d'), viewport,
      transform: [ratio, 0, 0, ratio, 0, 0],
      annotationMode: 0,
    })
    await renderTask.promise
    if (current === renderSequence && !stopped) page.cleanup()
  } catch (error) {
    if (current === renderSequence && !stopped && error.name !== 'RenderingCancelledException') {
      errorMessage.value = 'PDF 页面无法显示，请下载后查看。'
    }
  } finally {
    if (current === renderSequence && !stopped) loading.value = false
  }
}

function turnPage(offset) {
  if (loading.value) return
  pageNumber.value = Math.max(1, Math.min(pageCount.value, pageNumber.value + offset))
  renderPage()
  container.value.scrollTop = 0
}

onMounted(async () => {
  try {
    const bytes = await readPreviewBytes(props.blob)
    if (stopped) return
    documentTask = getDocument({
      data: new Uint8Array(bytes), BinaryDataFactory: LocalPdfDataFactory,
      useWorkerFetch: false, enableXfa: false, useSystemFonts: true,
      maxImageSize: 16_000_000, canvasMaxAreaInBytes: 16_000_000,
    })
    // Password-protected documents get a download fallback, never a stuck loader.
    documentTask.onPassword = () => {
      errorMessage.value = '这份 PDF 需要密码，请下载后查看。'
      loading.value = false
      documentTask.destroy().catch(() => {})
    }
    pdf = await documentTask.promise
    if (stopped) return
    pageCount.value = pdf.numPages
    await renderPage()
    observer = new ResizeObserver(() => {
      if (Math.abs(container.value.clientWidth - renderedWidth) > 2) renderPage()
    })
    observer.observe(container.value)
  } catch {
    if (!stopped) {
      errorMessage.value ||= 'PDF 无法读取，文件可能已损坏，请下载后查看。'
      loading.value = false
    }
  }
})

onBeforeUnmount(() => {
  stopped = true
  renderSequence += 1
  observer?.disconnect()
  renderTask?.cancel()
  documentTask?.destroy().catch(() => {})
  if (canvas.value) { canvas.value.width = 0; canvas.value.height = 0 }
})
</script>

<template>
  <div class="material-pdf">
    <nav v-if="pageCount" class="material-pdf__navigation" aria-label="PDF 翻页">
      <button type="button" :disabled="pageNumber <= 1 || loading" @click="turnPage(-1)">上一页</button>
      <span aria-live="polite">{{ pageNumber }} / {{ pageCount }} 页</span>
      <button type="button" :disabled="pageNumber >= pageCount || loading" @click="turnPage(1)">下一页</button>
    </nav>
    <p v-if="loading" role="status" class="material-pdf__feedback">正在渲染 PDF…</p>
    <p v-if="errorMessage" role="alert" class="material-pdf__feedback">{{ errorMessage }}</p>
    <div ref="container" class="material-pdf__page" :aria-busy="loading">
      <canvas v-show="!errorMessage" ref="canvas" role="img" :aria-label="`PDF 第 ${pageNumber} 页`">PDF 画布无法显示，请使用上方下载按钮查看原文件。</canvas>
    </div>
  </div>
</template>

<style scoped>
.material-pdf { display: flex; min-height: 0; height: 100%; flex-direction: column; }
.material-pdf__navigation { display: flex; padding: 10px 12px; align-items: center; justify-content: center; gap: 16px; border-bottom: 1px solid #e9ebef; background: #fff; font-size: 13px; }
.material-pdf__navigation button { min-height: 40px; padding: 7px 12px; border: 1px solid #e9ebef; border-radius: 9px; background: #fff; color: #343a44; font: inherit; cursor: pointer; }
.material-pdf__navigation button:disabled { opacity: .45; cursor: default; }
.material-pdf__feedback { margin: 0; padding: 12px; text-align: center; color: #777f8c; font-size: 13px; }
.material-pdf__feedback[role=alert] { color: #be4e31; }
.material-pdf__page { min-height: 0; padding: 12px; overflow: auto; background: #f3f4f6; flex: 1; text-align: center; }
.material-pdf__page canvas { display: block; max-width: 100%; margin: 0 auto; background: #fff; }
</style>
