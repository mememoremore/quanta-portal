<script setup>
import { ChatDotRound, Document, Promotion, View } from '@element-plus/icons-vue'

defineProps({
  statistics: { type: Object, default: () => ({}) },
  loading: { type: Boolean, default: false },
})

const cards = [
  { key: 'submissions', label: '投递总数', icon: Document },
  { key: 'firstPassed', label: '一面通过（进入二面）', icon: View },
  { key: 'secondPassed', label: '二面通过（发 offer）', icon: ChatDotRound },
  { key: 'joined', label: '确认加入', icon: Promotion },
]
</script>

<template>
  <div v-loading="loading" class="recruitment-board">
    <article v-for="card in cards" :key="card.key" class="recruitment-board__card">
      <ElIcon class="recruitment-board__icon"><component :is="card.icon" /></ElIcon>
      <strong>{{ statistics[card.key] ?? '—' }}</strong>
      <span>{{ card.label }}</span>
    </article>
  </div>
</template>
