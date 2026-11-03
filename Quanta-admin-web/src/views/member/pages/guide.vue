<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import MarkdownIt from 'markdown-it'
import source from './personal-website-guide.md?raw'

const route = useRoute()
const slug = computed(() => typeof route.query.slug === 'string' && /^[a-z0-9](?:[a-z0-9-]{1,61}[a-z0-9])$/.test(route.query.slug) ? route.query.slug : 'your-site')
const documentSource = computed(() => source.replaceAll('YOUR_SITE_SLUG', slug.value))
const markdown = new MarkdownIt({ html: false, linkify: false })
markdown.renderer.rules.heading_open = (tokens, index, options, env, renderer) => {
  if (tokens[index].tag === 'h2') {
    const id = `guide-section-${env.headings.length + 1}`
    tokens[index].attrSet('id', id)
    env.headings.push({ id, title: tokens[index + 1].content })
  }
  return renderer.renderToken(tokens, index, options)
}
const document = computed(() => {
  const env = { headings: [] }
  return { html: markdown.render(documentSource.value.replace(/^# .*\n/, ''), env), headings: env.headings }
})

function downloadGuide() {
  const url = URL.createObjectURL(new Blob([documentSource.value], { type: 'text/markdown;charset=utf-8' }))
  const anchor = globalThis.document.createElement('a')
  anchor.href = url
  anchor.download = 'Quanta个人网站使用指南.md'
  anchor.click()
  setTimeout(() => URL.revokeObjectURL(url), 1000)
}
</script>

<template>
  <section class="member-page website-guide">
    <header class="website-guide__header">
      <RouterLink to="/member/pages">← 返回个人网站</RouterLink>
      <p>QUANTA PAGES · 使用指南</p>
      <h1>个人网站使用指南</h1>
      <span>先把网站建起来，再一步步让它变成你的样子。</span>
      <div class="website-guide__actions">
        <span>示例站点标识：<code>{{ slug }}</code>{{ slug === 'your-site' ? '（使用前换成你自己的标识）' : '' }}</span>
        <button class="member-secondary-button" type="button" @click="downloadGuide">下载 Markdown 指南</button>
      </div>
    </header>
    <div class="website-guide__layout">
      <nav class="website-guide__toc portal-card" aria-label="指南章节">
        <strong>阅读目录</strong>
        <a v-for="heading in document.headings" :key="heading.id" :href="`#${heading.id}`">{{ heading.title }}</a>
      </nav>
      <!-- Bundled editorial Markdown only; raw HTML is disabled in the renderer. -->
      <!-- eslint-disable-next-line vue/no-v-html -->
      <article class="website-guide__content portal-card" v-html="document.html"></article>
    </div>
  </section>
</template>

<style src="../member.css"></style>
<style src="./guide.css"></style>
