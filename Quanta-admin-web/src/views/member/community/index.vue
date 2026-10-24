<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import MemberAuthor from '@/components/MemberAuthor.vue'
import { FORUM_CATEGORIES, listForumPosts, listMyForumReactions, forumCategoryLabel } from '@/api/portal/forum'
import { useUserStore } from '@/stores/user'
import { useForumReactions } from './reactions'
import ForumReactions from './ForumReactions.vue'
import { markdownImagePreviews } from './image-previews'
import './community.css'

const posts = ref([])
const total = ref(0)
const route = useRoute(), router = useRouter()
const query = route?.query || {}
const pageNum = ref(Math.max(1, Math.floor(Number(query.page)) || 1))
const pageSize = 10
const validScope = value => ['mine', 'liked', 'favorites'].includes(value) ? value : 'latest'
const scope = ref(validScope(query.scope))
const category = ref(FORUM_CATEGORIES.some((item) => item.value === query.category) ? query.category : '')
const loading = ref(true)
const errorMessage = ref('')
const userStore = useUserStore()
const reactions = useForumReactions()
const personal = computed(() => ['liked', 'favorites'].includes(scope.value))
const privateLabel = computed(() => scope.value === 'liked' ? '赞过' : '收藏')
let generation = 0

async function load() {
  const current = ++generation, startedAt = reactions.revision.value
  router.replace({ query:{ page:pageNum.value > 1 ? pageNum.value : undefined, scope:scope.value !== 'latest' ? scope.value : undefined, category:category.value || undefined } })
  loading.value = true
  errorMessage.value = ''
  try {
    const params = {
      pageNum: pageNum.value,
      pageSize,
      ...(scope.value === 'mine' ? { scope: 'mine' } : {}),
      ...(category.value ? { category: category.value } : {}),
    }
    const result = personal.value ? await listMyForumReactions(scope.value, params) : await listForumPosts(params)
    if (current !== generation) return
    if (!result.rows.length && result.total > 0 && pageNum.value > Math.max(1, Math.ceil(result.total / pageSize))) {
      pageNum.value = Math.max(1, Math.ceil(result.total / pageSize)); return load()
    }
    reactions.ingest(result.rows, startedAt)
    posts.value = result.rows.map((post) => ({ ...post, previewImages: markdownImagePreviews(post.content) }))
    total.value = result.total
  } catch (error) {
    if (current !== generation) return
    posts.value = []
    errorMessage.value = error.message || '社区内容加载失败，请稍后重试'
  } finally {
    if (current === generation) loading.value = false
  }
}

function chooseScope(value) {
  const next = value === 'mine' && scope.value === 'mine' ? 'latest' : value
  if (scope.value === next) return
  scope.value = next
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

function changed(kind) {
  if (personal.value && kind === scope.value) load()
}

function openComments(post) {
  router.push({ path: `/member/community/${post.postId}`, hash: '#discussion', state: { postReturn: route.fullPath } })
}

function resetFilter() { category.value = ''; pageNum.value = 1; load() }
function onVisible() { if (document.visibilityState === 'visible') load() }
watch(() => route.fullPath, () => {
  const next = route.query || {}
  const page = Math.max(1, Math.floor(Number(next.page)) || 1)
  const nextScope = validScope(next.scope)
  const nextCategory = FORUM_CATEGORIES.some(item => item.value === next.category) ? next.category : ''
  if (page === pageNum.value && nextScope === scope.value && nextCategory === category.value) return
  pageNum.value = page; scope.value = nextScope; category.value = nextCategory; load()
})
watch(() => userStore.user?.userId, load)

onMounted(() => { load(); document.addEventListener('visibilitychange', onVisible) })
onBeforeUnmount(() => { generation++; document.removeEventListener('visibilitychange', onVisible) })
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
        <button v-for="tab in [['latest','最新讨论'],['mine','我发布的'],['liked','我赞过的'],['favorites','我的收藏']]" :id="`community-tab-${tab[0]}`" :key="tab[0]" type="button" role="tab" aria-controls="community-posts-panel" :aria-selected="scope === tab[0]" :class="{ 'is-active': scope === tab[0] }" @click="chooseScope(tab[0])">{{ tab[1] }}</button>
      </div>
      <p v-if="personal" class="community-private-note">仅你可见 · {{ scope === 'liked' ? '按点赞时间排序' : '按收藏时间排序' }}</p>
      <div id="community-posts-panel" role="tabpanel" :aria-labelledby="`community-tab-${scope}`">
        <p v-if="errorMessage" class="member-feedback is-error" role="alert">{{ errorMessage }} <button type="button" @click="load">重试</button></p>
        <div v-if="loading" class="member-loading portal-card">正在加载讨论…</div>
        <div v-else-if="posts.length" class="community-feed__items">
          <article v-for="post in posts" :key="post.postId" class="community-post-card portal-card">
            <div class="community-post-card__author"><MemberAuthor :member-id="post.authorId" :name="post.authorName" :avatar="post.authorAvatar" :department="post.authorDepartment" /><div class="community-post-card__time"><time :datetime="post.createdAt" :title="post.createdAt">{{ displayTime(post.createdAt) }}</time><span v-if="post.viewCount != null"> · {{ post.viewCount }} 次浏览</span></div></div>
            <RouterLink class="community-post-card__title" :to="{path:`/member/community/${post.postId}`, state:{postReturn:route.fullPath}}">{{ post.title }}</RouterLink>
            <p>{{ post.summary || '打开帖子查看完整内容与讨论。' }}</p>
            <RouterLink v-if="post.previewImages.length" class="community-post-card__media" :to="{path:`/member/community/${post.postId}`, state:{postReturn:route.fullPath}}" :aria-label="`查看 ${post.title} 的图片与正文`"><img :src="post.previewImages[0].src" :alt="post.previewImages[0].alt" loading="lazy" /><span v-if="post.previewImages.length > 1" class="community-post-card__image-count">{{ post.previewImages.length }} 张</span></RouterLink>
            <div class="community-post-card__meta"><span class="community-tag">{{ forumCategoryLabel(post.category) }}</span><ForumReactions :post="post" @changed="changed" @comments="openComments(post)" /></div>
          </article>
        </div>
        <div v-else-if="!errorMessage" class="member-empty portal-card"><template v-if="personal"><p>{{ category ? `当前分类没有${privateLabel}的帖子` : `还没有${privateLabel}的帖子` }}</p><button v-if="category" type="button" class="member-secondary-button" @click="resetFilter">{{ scope === 'liked' ? '查看全部赞过的帖子' : '查看全部收藏' }}</button><button v-else type="button" class="member-secondary-button" @click="chooseScope('latest')">去看看最新讨论</button></template><template v-else>{{ scope === 'mine' ? '你还没有发布帖子' : '还没有讨论，来发布第一篇帖子吧' }}</template></div>
      </div>
      <footer v-if="!loading && !errorMessage && total > pageSize" class="community-pagination"><span>第 {{ pageNum }} / {{ Math.ceil(total / pageSize) }} 页</span><div><button class="member-secondary-button" type="button" :disabled="pageNum <= 1" @click="goToPage(pageNum - 1)">上一页</button><button class="member-secondary-button" type="button" :disabled="pageNum * pageSize >= total" @click="goToPage(pageNum + 1)">下一页</button></div></footer>
    </div>
  </section>
</template>

<style src="../member.css"></style>
