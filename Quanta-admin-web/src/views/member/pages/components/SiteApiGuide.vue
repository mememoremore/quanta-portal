<script setup>
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'

const props = defineProps({
  site: { type: Object, required: true },
})

const baseUrl = computed(() => `${window.location.origin}/open/pages/${props.site.slug}`)

const endpoints = computed(() => [
  { method: 'GET', url: `${baseUrl.value}/profile`, desc: '站主公开资料（昵称 / 头像 / 简介 / 技能 / 公开链接）' },
  { method: 'GET', url: `${baseUrl.value}/posts`, desc: '文章列表（分页：pageNum / pageSize，最大 50）' },
  { method: 'GET', url: `${baseUrl.value}/posts/{postId}`, desc: '文章详情（content 为 Markdown 原文）' },
  { method: 'GET', url: `${baseUrl.value}/posts/{postId}/comments`, desc: '文章留言（社区昵称 + 头像，分页）' },
])

const snippet = computed(() => `// 在你的站点页面里直接 fetch（服务端已允许 ${props.site.slug}.page.quantacenter.com 跨域读取）
fetch('${baseUrl.value}/posts?pageNum=1&pageSize=20')
  .then((response) => response.json())
  .then(({ data }) => {
    data.items.forEach((post) => {
      console.log(post.title, post.createdAt, post.excerpt)
    })
  })`)

const copied = ref(false)

async function copySnippet() {
  try {
    await navigator.clipboard.writeText(snippet.value)
    copied.value = true
    ElMessage.success('示例代码已复制')
  } catch {
    ElMessage.error('复制失败，请手动选中复制')
  }
}
</script>

<template>
  <section class="pages-api portal-card" data-testid="pages-api-guide">
    <header>
      <div><p>OPEN API</p><h2>站点数据 API</h2></div>
    </header>
    <p class="pages-api__hint">
      你的站点页面可以直接读取你在论坛发布的<b>文章</b>、这些文章的<b>留言</b>，以及你的<b>公开资料</b>，自行渲染成博客列表、归档页等。
      只包含你本人「塔员可见」的文章（经理可见的文章不会公开）；留言只显示社区昵称与公开头像，不含真实姓名；
      公开资料只含社区昵称、头像、简介、技能与公开链接，联系邮箱需在个人资料中勾选公开；
      论坛里上传的图片在公网页面无法加载，建议使用外链图片；接口限流每 IP 每分钟 120 次。
    </p>
    <ul class="pages-api__list">
      <li v-for="endpoint in endpoints" :key="endpoint.url" data-testid="pages-api-endpoint">
        <code>{{ endpoint.method }}</code>
        <span class="pages-api__url">{{ endpoint.url }}</span>
        <small>{{ endpoint.desc }}</small>
      </li>
    </ul>
    <pre class="pages-api__snippet" data-testid="pages-api-snippet">{{ snippet }}</pre>
    <button class="member-secondary-button" type="button" data-testid="pages-api-copy" @click="copySnippet">
      {{ copied ? '已复制' : '复制示例代码' }}
    </button>
  </section>
</template>
