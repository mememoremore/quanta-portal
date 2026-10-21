<script setup>
import { computed, onBeforeUnmount, ref, useId, watch } from 'vue'
import { markdownToDocument, markdownToHtml } from './markdown'
import { reconcileMarkdown } from './markdown-dom'

const props = defineProps({ source: { type: String, default: '' }, outline: Boolean })
const emit = defineEmits(['outline'])
const prefix = `quanta-article-${useId()}`
const rendered = computed(() => props.outline ? markdownToDocument(props.source, prefix) : { html: markdownToHtml(props.source), headings: [] })
const html = computed(() => rendered.value.html)
watch(() => rendered.value.headings, headings => emit('outline', headings), { immediate: true, flush: 'post' })
const timers = new Set()
const content = ref(null)
let diagramCleanup, diagramVersion = 0

async function attachDiagrams() {
  const current = ++diagramVersion
  const root = content.value
  if (!root) return
  reconcileMarkdown(root, html.value)
  if (!root.querySelector('[data-mermaid-block]')) {
    diagramCleanup?.()
    diagramCleanup = null
    return
  }
  if (diagramCleanup) { diagramCleanup.sync(); return }
  try {
    const { mountMermaidDiagrams } = await import('./mermaid-diagrams')
    if (current === diagramVersion) diagramCleanup = mountMermaidDiagrams(root, prefix)
  } catch {
    if (current !== diagramVersion) return
    root.querySelectorAll('[data-mermaid-status]').forEach(element => { element.textContent = '图表阅读器加载失败，请刷新重试；下方保留了源码。' })
    root.querySelectorAll('[data-mermaid-code]').forEach(element => { element.hidden = false })
  }
}
watch([html, content], attachDiagrams, { flush: 'sync' })

async function copyCode(event) {
  const button = event.target.closest?.('button[data-copy-code]')
  if (!button || !event.currentTarget.contains(button)) return
  const code = button.closest('.community-code-block')?.querySelector('pre code')
  if (!code) return
  const text = code.textContent
  try {
    let copied = false
    if (navigator.clipboard?.writeText) {
      try { await navigator.clipboard.writeText(text); copied = true } catch { /* Try the browser selection fallback below. */ }
    }
    if (!copied) {
      const previousFocus = document.activeElement
      const field = document.createElement('textarea')
      field.value = text
      field.readOnly = true
      field.style.cssText = 'position:fixed;left:-9999px;top:0;'
      document.body.appendChild(field)
      try {
        field.select()
        if (!document.execCommand('copy')) throw new Error('Copy failed')
      } finally {
        field.remove()
        previousFocus?.focus?.({ preventScroll: true })
      }
    }
    button.textContent = '已复制'
  } catch {
    button.textContent = '复制失败，请手动选择'
  }
  const timer = setTimeout(() => { button.textContent = '复制代码'; timers.delete(timer) }, 2000)
  timers.add(timer)
}

onBeforeUnmount(() => {
  diagramVersion += 1
  diagramCleanup?.()
  timers.forEach(clearTimeout)
})
</script>

<template>
  <!-- Reconcile only safe renderer output; preserve live Mermaid islands. -->
  <div ref="content" class="community-markdown" @click="copyCode"></div>
</template>
