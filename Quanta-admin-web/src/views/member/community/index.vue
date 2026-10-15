<script setup>
import { onMounted, ref } from 'vue'
import { FORUM_CATEGORIES, listForumPosts, forumCategoryLabel } from '@/api/portal/forum'
import { markdownImagePreviews } from './markdown'
import './community.css'

const posts = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = 10
const scope = ref('latest')
const category = ref('')
const loading = ref(true)
const errorMessage = ref('')

async function load() {
  loading.value = true
  errorMessage.value = ''
  try {
    const result = await listForumPosts({
      pageNum: pageNum.value,
      pageSize,
      ...(scope.value === 'mine' ? { scope: 'mine' } : {}),
      ...(category.value ? { category: category.value } : {}),
    })
    posts.value = result.rows.map((post) => ({ ...post, previewImages: markdownImagePreviews(post.content) }))
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

function chooseCategory(value) {
  if (category.value === value) return
  category.value = value
  pageNum.value = 1
  load()
}

function displayTime(value) {
  if (!value) return ''
  const date = new Date(String(value).replace(' ', 'T'))
  if (Number.isNaN(date.getTime())) return value
  const minutes = Math.floor((Date.now() - date.getTime()) / 60000)
  if (minutes < 0) return value
  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes} 分钟前`
  if (minutes < 1440) return `${Math.floor(minutes / 60)} 小时前`
  if (minutes < 2880) return '昨天'
  return value
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
      <div><h1>社区论坛</h1><span>分享项目、提出问题，和塔友一起把想法变成作品。</span></div>
      <RouterLink class="member-primary-button community-hero__action" to="/member/community/new">＋ 发布帖子</RouterLink>
    </header>

    <nav class="community-categories" aria-label="讨论分类">
      <button type="button" data-category="all" :class="{ 'is-active': !category }" :aria-pressed="!category" @click="chooseCategory('')">全部讨论</button>
      <button v-for="item in FORUM_CATEGORIES" :key="item.value" type="button" :data-category="item.value" :class="{ 'is-active': category === item.value }" :aria-pressed="category === item.value" @click="chooseCategory(item.value)">{{ item.label }}</button>
    </nav>

    <div class="community-feed">
        <div class="community-feed__tabs" role="tablist" aria-label="帖子范围">
          <button type="button" role="tab" :aria-selected="scope === 'latest'" :class="{ 'is-active': scope === 'latest' }" @click="chooseScope('latest')">最新讨论</button>
          <button type="button" role="tab" :aria-selected="scope === 'mine'" :class="{ 'is-active': scope === 'mine' }" @click="chooseScope('mine')">我发布的 <span aria-hidden="true">→</span></button>
        </div>
        <p v-if="errorMessage" class="member-feedback is-error" role="alert">{{ errorMessage }} <button type="button" @click="load">重试</button></p>
        <div v-if="loading" class="member-loading portal-card">正在加载讨论…</div>
        <div v-else-if="posts.length" class="community-feed__items">
          <article v-for="post in posts" :key="post.postId" class="community-post-card portal-card">
            <div class="community-post-card__author"><span class="community-avatar">{{ post.authorName.slice(0, 1) }}</span><strong>{{ post.authorName }}</strong><span v-if="post.authorDepartment">· {{ post.authorDepartment }}</span><time :datetime="post.createdAt" :title="post.createdAt">{{ displayTime(post.createdAt) }}</time></div>
            <RouterLink class="community-post-card__title" :to="`/member/community/${post.postId}`">{{ post.title }}</RouterLink>
            <p>{{ post.summary || '打开帖子查看完整内容与讨论。' }}</p>
            <RouterLink v-if="post.previewImages.length" class="community-post-card__media" :to="`/member/community/${post.postId}`" :aria-label="`查看 ${post.title} 的图片与正文`"><img :src="post.previewImages[0].src" :alt="post.previewImages[0].alt" loading="lazy" /><span v-if="post.previewImages.length > 1" class="community-post-card__image-count">{{ post.previewImages.length }} 张</span></RouterLink>
            <div class="community-post-card__meta"><span class="community-tag">{{ forumCategoryLabel(post.category) }}</span><span>{{ post.commentCount }} 条评论</span></div>
          </article>
        </div>
        <div v-else-if="!errorMessage" class="member-empty portal-card">{{ scope === 'mine' ? '你还没有发布帖子' : '还没有讨论，来发布第一篇帖子吧' }}</div>
        <footer v-if="!loading && !errorMessage && total > pageSize" class="community-pagination"><span>第 {{ pageNum }} / {{ Math.ceil(total / pageSize) }} 页</span><div><button class="member-secondary-button" type="button" :disabled="pageNum <= 1" @click="goToPage(pageNum - 1)">上一页</button><button class="member-secondary-button" type="button" :disabled="pageNum * pageSize >= total" @click="goToPage(pageNum + 1)">下一页</button></div></footer>
    </div>
  </section>
</template>

<style src="../member.css"></style>
