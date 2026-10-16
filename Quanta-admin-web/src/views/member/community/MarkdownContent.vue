<script setup>
import { computed, onBeforeUnmount } from 'vue'
import { markdownToHtml } from './markdown'

const props = defineProps({ source: { type: String, default: '' } })
const html = computed(() => markdownToHtml(props.source))
const timers = new Set()

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

onBeforeUnmount(() => timers.forEach(clearTimeout))
</script>

<template>
  <!-- The renderer escapes raw HTML and only accepts safe link/image schemes. -->
  <!-- eslint-disable-next-line vue/no-v-html -->
  <div class="community-markdown" @click="copyCode" v-html="html"></div>
</template>
