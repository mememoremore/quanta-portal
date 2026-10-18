<script setup>
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { useMediaQuery } from '@/composables/useMediaQuery'

const props = defineProps({ headings: { type: Array, default: () => [] } })
const wide = useMediaQuery('(min-width: 1200px)')
const activeId = ref('')
const navigation = ref(null)
const baseLevel = computed(() => Math.min(...props.headings.map(h => h.level)))
let targets = [], generation = 0, frame = 0, settleTimer = 0, pendingTop = null
const scrollKeys = new Set(['ArrowUp', 'ArrowDown', 'PageUp', 'PageDown', 'Home', 'End', ' '])
function offset() {
  return Math.max(96, Math.ceil((document.querySelector('.portal-header')?.getBoundingClientRect().height || 0) + 20))
}
function releaseNavigation() {
  pendingTop = null
  window.clearTimeout(settleTimer)
  settleTimer = 0
}
function userIntent(event) {
  if (event.type !== 'keydown' || scrollKeys.has(event.key)) releaseNavigation()
}
function updateActive() {
  const edge = offset() + 2
  let current = targets[0]
  for (const target of targets) {
    if (target.getBoundingClientRect().top > edge) break
    current = target
  }
  if (current) activeId.value = current.id
}
function onScroll() {
  if (pendingTop !== null) {
    if (Math.abs(window.scrollY - pendingTop) < 2) releaseNavigation()
    return
  }
  if (frame) return
  frame = window.requestAnimationFrame(() => {
    frame = 0
    if (pendingTop === null) updateActive()
  })
}
function onResize() { releaseNavigation(); onScroll() }
function stop() {
  releaseNavigation()
  window.cancelAnimationFrame(frame)
  frame = 0
  targets = []
  window.removeEventListener('scroll', onScroll)
  window.removeEventListener('resize', onResize)
  window.removeEventListener('scrollend', releaseNavigation)
  for (const type of ['wheel', 'touchstart', 'pointerdown', 'keydown']) window.removeEventListener(type, userIntent)
}
watch([() => props.headings, wide], async () => {
  const current = ++generation
  stop()
  activeId.value = props.headings[0]?.id || ''
  if (!wide.value || props.headings.length < 2) return
  await nextTick()
  if (current !== generation) return
  targets = props.headings.map(h => document.getElementById(h.id)).filter(Boolean)
  window.addEventListener('scroll', onScroll, { passive: true })
  window.addEventListener('resize', onResize, { passive: true })
  window.addEventListener('scrollend', releaseNavigation)
  for (const type of ['wheel', 'touchstart', 'pointerdown', 'keydown']) window.addEventListener(type, userIntent, { passive: true })
  onScroll()
}, { immediate: true })
watch(activeId, async () => {
  await nextTick()
  const nav = navigation.value, link = nav?.querySelector('[aria-current]')
  if (!link) return
  const box = link.getBoundingClientRect(), viewport = nav.getBoundingClientRect()
  // Scroll this list only: scrollIntoView can also move the article/document.
  if (box.top < viewport.top) nav.scrollTop += box.top - viewport.top
  else if (box.bottom > viewport.bottom) nav.scrollTop += box.bottom - viewport.bottom
})
function goTo(heading) {
  const el = document.getElementById(heading.id)
  if (!el) return
  window.cancelAnimationFrame(frame)
  frame = 0
  releaseNavigation()
  activeId.value = heading.id
  const maximum = Math.max(0, document.documentElement.scrollHeight - window.innerHeight)
  pendingTop = Math.max(0, Math.min(maximum, window.scrollY + el.getBoundingClientRect().top - offset()))
  // Keep the clicked entry stable while passing intermediate headings.
  settleTimer = window.setTimeout(releaseNavigation, 1600)
  window.scrollTo({ top: pendingTop, behavior: window.matchMedia?.('(prefers-reduced-motion: reduce)').matches ? 'instant' : 'smooth' })
}
onBeforeUnmount(() => { generation += 1; stop() })
</script>

<template>
  <aside v-if="wide && headings.length > 1" class="community-toc" aria-label="文章目录">
    <strong>文章目录</strong>
    <nav ref="navigation"><a v-for="heading in headings" :key="heading.id" :href="`#${heading.id}`" :aria-current="activeId === heading.id ? 'location' : undefined" :style="{ paddingLeft: `${12 + Math.min(3, heading.level - baseLevel) * 12}px` }" @click.prevent="goTo(heading)">{{ heading.text }}</a></nav>
    <button type="button" @click="headings[0] && goTo(headings[0])">↑ 回到正文顶部</button>
  </aside>
</template>

<style scoped>
.community-toc { position: sticky; top: 98px; min-width: 0; align-self: start; padding: 20px 0; }
.community-toc > strong { display: block; margin-bottom: 17px; color: var(--portal-ink); font-size: 13px; }
.community-toc nav { display: grid; max-height: calc(100dvh - 240px); overflow-x: hidden; overflow-y: auto; overscroll-behavior-y: contain; scrollbar-width: thin; scrollbar-color: #c9cfd8 transparent; border-left: 1px solid var(--portal-line); }
.community-toc a { padding: 8px 12px; margin-left: -1px; border-left: 2px solid transparent; color: var(--portal-muted); font-size: 12px; line-height: 1.7; overflow-wrap: anywhere; }
.community-toc a[aria-current] { color: var(--portal-orange-strong); border-color: var(--portal-orange); background: var(--portal-orange-soft); }
.community-toc button { margin-top: 18px; padding: 6px 0; border: 0; background: none; color: var(--portal-muted); font: inherit; font-size: 12px; cursor: pointer; }
</style>
