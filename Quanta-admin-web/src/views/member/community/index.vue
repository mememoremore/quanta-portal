<script setup>
import { onMounted, ref } from 'vue'
import { listForumPosts, forumCategoryLabel } from '@/api/portal/forum'
import './community.css'

const posts = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = 10
const scope = ref('latest')
const loading = ref(true)
const errorMessage = ref('')

async function load() {
  loading.value = true
  errorMessage.value = ''
  try {
    const result = await listForumPosts({ pageNum: pageNum.value, pageSize, ...(scope.value === 'mine' ? { scope: 'mine' } : {}) })
    posts.value = result.rows
    total.value = result.total
  } catch (error) {
    posts.value = []
    errorMessage.value = error.message || '社区内容加载失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

function chooseScope(value) {
  if (scope.value === value) return
  scope.value = value
  pageNum.value = 1
  load()
}

function goToPage(value) {
  pageNum.value = value
  load()
}

onMounted(load)
</script>

<template>
  <section class="member-page community-page">
    <header class="member-page__heading community-hero">
      <div><p>QUANTA COMMUNITY</p><h1>社区论坛</h1><span>分享项目、提出问题，和塔友一起把想法变成作品。</span></div>
      <RouterLink class="member-primary-button community-hero__action" to="/member/community/new">＋ 发布帖子</RouterLink>
    </header>

    <div class="community-layout">
      <main class="community-feed">
        <div class="community-feed__tabs" role="tablist" aria-label="帖子范围">
          <button type="button" role="tab" :aria-selected="scope === 'latest'" :class="{ 'is-active': scope === 'latest' }" @click="chooseScope('latest')">最新讨论</button>
          <button type="button" role="tab" :aria-selected="scope === 'mine'" :class="{ 'is-active': scope === 'mine' }" @click="chooseScope('mine')">我发布的</button>
        </div>
        <p v-if="errorMessage" class="member-feedback is-error" role="alert">{{ errorMessage }} <button type="button" @click="load">重试</button></p>
        <div v-if="loading" class="member-loading portal-card">正在加载讨论…</div>
        <div v-else-if="posts.length" class="community-feed__items">
          <article v-for="post in posts" :key="post.postId" class="community-post-card portal-card">
            <div class="community-post-card__author"><span class="community-avatar">{{ post.authorName.slice(0, 1) }}</span><strong>{{ post.authorName }}</strong><span v-if="post.authorDepartment">· {{ post.authorDepartment }}</span><time>{{ post.createdAt }}</time></div>
            <RouterLink class="community-post-card__title" :to="`/member/community/${post.postId}`">{{ post.title }}</RouterLink>
            <p>{{ post.summary || '打开帖子查看完整内容与讨论。' }}</p>
            <div class="community-post-card__meta"><span class="community-tag">{{ forumCategoryLabel(post.category) }}</span><span>{{ post.commentCount }} 条评论</span><RouterLink :to="`/member/community/${post.postId}`">参与讨论 →</RouterLink></div>
          </article>
        </div>
        <div v-else-if="!errorMessage" class="member-empty portal-card">{{ scope === 'mine' ? '你还没有发布帖子' : '还没有讨论，来发布第一篇帖子吧' }}</div>
        <footer v-if="!loading && !errorMessage && total > pageSize" class="community-pagination"><span>第 {{ pageNum }} / {{ Math.ceil(total / pageSize) }} 页</span><div><button class="member-secondary-button" type="button" :disabled="pageNum <= 1" @click="goToPage(pageNum - 1)">上一页</button><button class="member-secondary-button" type="button" :disabled="pageNum * pageSize >= total" @click="goToPage(pageNum + 1)">下一页</button></div></footer>
      </main>
      <aside class="community-guide portal-card"><p>COMMUNITY GUIDE</p><h2>一起把讨论变得有价值</h2><span>所有塔员都可以发帖与回复。技术帖可以插入代码和图片，讨论内容仅塔员可见。</span><ul><li>项目协作 · 交流正在做的事</li><li>技术分享 · 记录所学所想</li><li>日常交流 · 认识更多塔友</li></ul></aside>
    </div>
  </section>
</template>

<style src="../member.css"></style>
