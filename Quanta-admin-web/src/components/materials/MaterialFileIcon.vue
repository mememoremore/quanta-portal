<script setup>
import { computed } from 'vue'

const props = defineProps({ name: { type: String, default: '' } })
const kinds = {
  pdf: 'pdf', doc: 'word', docx: 'word', md: 'markdown', ppt: 'slides', pptx: 'slides',
  xls: 'sheet', xlsx: 'sheet', txt: 'text', jpg: 'image', jpeg: 'image', png: 'image', webp: 'image',
}
const kind = computed(() => {
  const extension = String(props.name || '').split('.').at(-1)?.toLowerCase()
  return Object.hasOwn(kinds, extension) ? kinds[extension] : 'generic'
})
const letter = computed(() => ({ word: 'W', markdown: 'M', slides: 'P', sheet: 'X' })[kind.value] || '')
</script>

<template>
  <span class="material-file-icon" :data-kind="kind" aria-hidden="true">
    <svg viewBox="0 0 28 32" fill="none" xmlns="http://www.w3.org/2000/svg" focusable="false">
      <path d="M8 3h9l6 6v19a1 1 0 0 1-1 1H8a1 1 0 0 1-1-1V4a1 1 0 0 1 1-1Z" :fill="letter ? 'currentColor' : 'none'" stroke="currentColor" stroke-width="1.8" stroke-linejoin="round" />
      <path d="M17 3v6h6" :stroke="letter ? '#fff' : 'currentColor'" :opacity="letter ? .65 : 1" stroke-width="1.8" stroke-linejoin="round" />
      <template v-if="letter">
        <rect x="2" y="12" width="16" height="15" rx="2.5" fill="currentColor" stroke="#fff0e4" stroke-width="1" />
        <text x="10" y="23" text-anchor="middle" fill="#fff" font-family="Arial, sans-serif" font-size="10" font-weight="700">{{ letter }}</text>
      </template>
      <path v-else-if="kind === 'pdf'" d="M14.4 11.5c-1.5 4.3-2.9 7.9-5.4 10-1.5 1.3-2.3.2-1.4-1.1 1.5-2.1 7.4-3.7 11.3-3.1 2.3.4 2.6 2 1 2.2-2.7.4-6.8-5.7-6.3-8.4.2-1.1 1.3-.8.8.4Z" stroke="currentColor" stroke-width="1.4" stroke-linecap="round" stroke-linejoin="round" />
      <template v-else-if="kind === 'image'">
        <circle cx="12" cy="14" r="1.4" fill="currentColor" />
        <path d="m9.5 24 4-5 2.5 3 2-2 3 4h-11.5Z" fill="currentColor" />
      </template>
      <path v-else d="M11 14h8m-8 5h8m-8 5h5" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" />
    </svg>
  </span>
</template>

<style scoped>
.material-file-icon { display: inline-grid; width: 38px; height: 42px; border-radius: 9px; background: #fff0e4; color: #ff6700; place-items: center; flex: 0 0 auto; }
.material-file-icon svg { display: block; width: 25px; height: 29px; }
</style>
