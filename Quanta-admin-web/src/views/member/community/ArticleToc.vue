<script setup>
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { useMediaQuery } from '@/composables/useMediaQuery'

const props = defineProps({ headings: { type: Array, default: () => [] } })
const wide = useMediaQuery('(min-width: 1200px)')
const activeId = ref('')
const baseLevel = computed(() => Math.min(...props.headings.map(h => h.level)))
let observer, generation = 0
function stop() { observer?.disconnect(); observer = null }
watch([() => props.headings, wide], async () => {
  const current = ++generation
  stop()
  activeId.value = props.headings[0]?.id || ''
  if (!wide.value || props.headings.length < 2) return
  await nextTick()
  if (current !== generation || !window.IntersectionObserver) return
  const visible = new Set()
  observer = new IntersectionObserver(entries => {
    entries.forEach(entry => { if (entry.isIntersecting) visible.add(entry.target.id); else visible.delete(entry.target.id) })
    const first = props.headings.find(h => visible.has(h.id))
    if (first) activeId.value = first.id
  }, { rootMargin: '-80px 0px -55% 0px', threshold: 0 })
  props.headings.forEach(h => { const el = document.getElementById(h.id); if (el) observer.observe(el) })
}, { immediate: true })
function goTo(heading) {
  const el = document.getElementById(heading.id)
  if (!el) return
  activeId.value = heading.id
  el.scrollIntoView({ behavior: window.matchMedia?.('(prefers-reduced-motion: reduce)').matches ? 'auto' : 'smooth', block: 'start' })
}
onBeforeUnmount(() => { generation += 1; stop() })
</script>

<template>
  <aside v-if="wide && headings.length > 1" class="community-toc" aria-label="文章目录">
    <strong>文章目录</strong>
    <nav><a v-for="heading in headings" :key="heading.id" :href="`#${heading.id}`" :aria-current="activeId === heading.id ? 'location' : undefined" :style="{ paddingLeft: `${12 + Math.min(3, heading.level - baseLevel) * 12}px` }" @click.prevent="goTo(heading)">{{ heading.text }}</a></nav>
    <button type="button" @click="headings[0] && goTo(headings[0])">↑ 回到正文顶部</button>
  </aside>
</template>

<style scoped>
.community-toc { position: sticky; top: 98px; min-width: 0; align-self: start; padding: 20px 0; }
.community-toc > strong { display: block; margin-bottom: 17px; color: var(--portal-ink); font-size: 13px; }
.community-toc nav { display: grid; max-height: calc(100dvh - 240px); overflow-y: auto; border-left: 1px solid var(--portal-line); }
.community-toc a { padding: 8px 12px; margin-left: -1px; border-left: 2px solid transparent; color: var(--portal-muted); font-size: 12px; line-height: 1.7; overflow-wrap: anywhere; }
.community-toc a[aria-current] { color: var(--portal-orange-strong); border-color: var(--portal-orange); background: var(--portal-orange-soft); }
.community-toc button { margin-top: 18px; padding: 6px 0; border: 0; background: none; color: var(--portal-muted); font: inherit; font-size: 12px; cursor: pointer; }
</style>
