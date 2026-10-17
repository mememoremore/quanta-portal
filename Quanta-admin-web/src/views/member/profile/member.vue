<script setup>
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getPublicMemberProfile, getMemberPosts } from '@/api/portal/members'
import { forumCategoryLabel } from '@/api/portal/forum'
import { profileLink, safeProfileReturn } from '@/utils/member-profile'
import MemberAvatar from '@/components/MemberAvatar.vue'

const route = useRoute(), router = useRouter()
const profile = ref(null), loading = ref(true), errorMessage = ref('')
const posts = ref([]), total = ref(0), postsLoading = ref(false), postsError = ref('')
const tab = computed(() => route.query.tab === 'posts' ? 'posts' : 'about')
const page = computed(() => Math.max(1, Math.floor(Number(route.query.page)) || 1))
const returnPath = computed(() => safeProfileReturn(router.options.history.state?.memberReturn))
const returnLabel = computed(() => returnPath.value.startsWith('/member/directory') ? '返回通讯录' : returnPath.value === '/member/profile' ? '返回个人中心' : /\/community\/\d+/.test(returnPath.value) ? '返回刚才的文章' : '返回社区')
const links = computed(() => [{ title:'GitHub', url:profileLink(profile.value?.githubUrl) }, { title:'作品主页', url:profileLink(profile.value?.websiteUrl) }].filter((item) => item.url))
let generation = 0, postGeneration = 0
async function load() {
  const current = ++generation
  loading.value = true; errorMessage.value = ''; profile.value = null; posts.value = []; total.value = 0
  try {
    const value = await getPublicMemberProfile(route.params.userId)
    if (current === generation) profile.value = value
  } catch (error) { if (current === generation) errorMessage.value = Number(error.code) === 404 ? '成员不存在，或成员主页接口尚未接通。' : error.message || '成员资料加载失败' }
  finally { if (current === generation) loading.value = false }
}
async function loadPosts() {
  const current = ++postGeneration
  if (tab.value !== 'posts' || !profile.value) { postsLoading.value = false; return }
  postsLoading.value = true; postsError.value = ''; posts.value = []
  try {
    const result = await getMemberPosts(route.params.userId, { pageNum:page.value, pageSize:10 })
    if (current === postGeneration) { posts.value = result.rows; total.value = result.total }
  } catch (error) { if (current === postGeneration) postsError.value = error.message || '成员帖子加载失败' }
  finally { if (current === postGeneration) postsLoading.value = false }
}
function setQuery(query) { return router.replace({ query, state:{memberReturn:returnPath.value} }) }
function selectTab(value) { setQuery({ ...route.query, tab:value, page:undefined }) }
function goBack() {
  if (router.options.history.state?.back === returnPath.value) router.back()
  else router.replace(returnPath.value)
}
watch(() => route.params.userId, load, { immediate:true })
watch([() => profile.value?.id, tab, page], loadPosts)
onBeforeUnmount(() => { generation++; postGeneration++ })
</script>
<template>
  <section class="member-page member-public-page">
    <button class="member-public-back" type="button" @click="goBack">← {{ returnLabel }}</button>
    <p v-if="errorMessage" class="member-feedback is-error" role="alert">{{ errorMessage }} <button type="button" @click="load">重试</button></p>
    <div v-if="loading" class="member-loading portal-card">正在加载成员主页…</div>
    <article v-else-if="profile" class="member-public-card">
      <div class="member-public-cover"><span>QUANTA MEMBER</span><small>仅已登录塔员可见</small></div>
      <header class="member-public-intro"><MemberAvatar :member-id="profile.id" :src="profile.avatar" :name="profile.name" :size="88" eager /><div class="member-public-name"><h1>{{ profile.name }}</h1><div class="member-public-meta"><span>{{ profile.department }}</span>{{ profile.title }} · {{ profile.cohort }}</div></div><p v-if="profile.headline">{{ profile.headline }}</p></header>
      <div class="member-public-tabs" role="tablist" aria-label="成员主页内容"><button id="member-about-tab" type="button" role="tab" aria-controls="member-public-panel" :aria-selected="tab === 'about'" @click="selectTab('about')">个人介绍</button><button id="member-posts-tab" type="button" role="tab" aria-controls="member-public-panel" :aria-selected="tab === 'posts'" @click="selectTab('posts')">发布的帖子</button></div>
      <div id="member-public-panel" class="member-public-content" role="tabpanel" :aria-labelledby="tab === 'about' ? 'member-about-tab' : 'member-posts-tab'">
        <div v-if="tab === 'about'" class="member-public-about"><div><section><h2>关于我</h2><p>{{ profile.bio || '这位塔友暂未填写个人介绍，可以先看看 TA 发布的帖子。' }}</p></section><section><h2>技能与兴趣</h2><div v-if="profile.skills.length" class="member-public-skills"><span v-for="skill in profile.skills" :key="skill">{{ skill }}</span></div><p v-else>暂未填写技能与兴趣。</p></section></div><section><h2>找到我</h2><div class="member-public-links"><a v-for="link in links" :key="link.title" :href="link.url" target="_blank" rel="noopener noreferrer"><strong>{{ link.title }}</strong><span>{{ link.url }}</span><b aria-hidden="true">↗</b></a><a v-if="profile.showContactEmail && profile.contactEmail" :href="`mailto:${encodeURIComponent(profile.contactEmail)}`"><strong>联系邮箱</strong><span>{{ profile.contactEmail }}</span></a></div><p v-if="!links.length && !(profile.showContactEmail && profile.contactEmail)">暂未公开联系方式，可以在社区帖子下与 TA 交流。</p><small v-else>仅展示对方主动公开的链接与联系方式。</small></section></div>
        <template v-else><p v-if="postsError" class="member-feedback is-error" role="alert">{{ postsError }} <button type="button" @click="loadPosts">重试</button></p><div v-if="postsLoading" class="member-loading">正在加载帖子…</div><div v-else-if="posts.length" class="member-public-posts"><article v-for="post in posts" :key="post.postId"><RouterLink :to="{ path:`/member/community/${post.postId}`, state:{ postReturn:route.fullPath } }">{{ post.title }}</RouterLink><p>{{ post.summary }}</p><footer><span>{{ forumCategoryLabel(post.category) }}</span><span>{{ post.commentCount }} 条评论</span><span v-if="post.viewCount != null">{{ post.viewCount }} 次浏览</span></footer></article><nav v-if="total > 10" aria-label="成员帖子分页"><button type="button" class="member-secondary-button" :disabled="page <= 1" @click="setQuery({...route.query,page:page-1})">上一页</button><span>第 {{ page }} 页</span><button type="button" class="member-secondary-button" :disabled="page*10 >= total" @click="setQuery({...route.query,page:page+1})">下一页</button></nav></div><p v-else-if="!postsError" class="member-empty">这位塔友还没有发布帖子。</p></template>
      </div>
    </article>
  </section>
</template>
<style src="../member.css"></style>
<style scoped>
.member-public-back { justify-self:start; background:none; border:0; color:var(--portal-muted); font:inherit; font-size:13px; padding:8px 0; cursor:pointer; }.member-public-card { border:1px solid var(--portal-line); border-radius:24px; overflow:hidden; background:#fff; }.member-public-cover { height:98px; position:relative; overflow:hidden; padding:22px 28px; display:flex; align-items:flex-start; justify-content:space-between; gap:15px; background:linear-gradient(120deg,#202124,#35312e); color:#e4d2c5; }.member-public-cover::after { content:''; width:165px; height:165px; border:28px solid #ff7c242b; border-radius:44px; transform:rotate(25deg); position:absolute; right:50px; top:-45px; pointer-events:none; }.member-public-cover span { z-index:1; font-size:10px; letter-spacing:2px; }.member-public-cover small { z-index:1; font-size:11px; }
.member-public-intro { padding:0 28px 24px; }.member-public-intro > .member-avatar { position:relative; margin-top:-32px; outline:5px solid #fff; }.member-public-name { display:flex; justify-content:space-between; align-items:center; flex-wrap:wrap; gap:12px; margin:18px 0 10px; }.member-public-name h1 { margin:0; font-size:30px; overflow-wrap:anywhere; }.member-public-meta { display:flex; align-items:center; flex-wrap:wrap; gap:8px; color:var(--portal-muted); font-size:12px; }.member-public-meta > span { padding:4px 8px; border-radius:6px; background:var(--portal-orange-soft); color:var(--portal-orange-strong); }.member-public-intro p { font-size:15px; line-height:1.8; margin:16px 0 0; overflow-wrap:anywhere; }
.member-public-tabs { display:flex; gap:25px; padding:0 28px; border-bottom:1px solid var(--portal-line); }.member-public-tabs button { padding:15px 0 13px; background:none; border:0; border-bottom:3px solid transparent; color:var(--portal-muted); font:inherit; font-size:13px; cursor:pointer; }.member-public-tabs button[aria-selected=true] { color:var(--portal-orange-strong); border-bottom-color:var(--portal-orange); font-weight:700; }.member-public-content { padding:28px; }.member-public-about { display:grid; grid-template-columns:1.2fr 1fr; gap:32px; }.member-public-about section + section { margin-top:28px; }.member-public-about h2 { font-size:15px; margin:0 0 13px; }.member-public-about p { font-size:14px; color:var(--portal-muted); line-height:1.9; margin:0; white-space:pre-wrap; overflow-wrap:anywhere; }.member-public-about small { display:block; margin-top:13px; color:var(--portal-muted); font-size:11px; line-height:1.8; }.member-public-skills { display:flex; flex-wrap:wrap; gap:8px; }.member-public-skills span { background:#f6f7f9; color:var(--portal-muted); padding:7px 10px; border-radius:8px; font-size:12px; overflow-wrap:anywhere; }
.member-public-links { display:grid; gap:10px; }.member-public-links a { display:grid; grid-template-columns:minmax(0,1fr) auto; gap:5px 8px; padding:13px 15px; border:1px solid var(--portal-line); border-radius:12px; color:var(--portal-ink); text-decoration:none; }.member-public-links strong { font-size:12px; }.member-public-links span { grid-column:1; color:var(--portal-muted); font-size:11px; overflow-wrap:anywhere; }.member-public-links b { grid-column:2; grid-row:1/3; align-self:center; color:var(--portal-orange-strong); }
.member-public-posts { display:grid; gap:22px; }.member-public-posts article { border-bottom:1px solid var(--portal-line); padding-bottom:20px; }.member-public-posts article > a { color:var(--portal-ink); font-size:20px; font-weight:800; text-decoration:none; overflow-wrap:anywhere; }.member-public-posts article > a:hover { color:var(--portal-orange-strong); }.member-public-posts p { font-size:14px; line-height:1.8; color:var(--portal-muted); overflow-wrap:anywhere; }.member-public-posts footer,.member-public-posts nav { display:flex; align-items:center; flex-wrap:wrap; gap:14px; font-size:12px; color:var(--portal-muted); }.member-public-posts footer > span:first-child { padding:6px 9px; border-radius:7px; background:var(--portal-orange-soft); color:var(--portal-orange-strong); }
@media(max-width:650px) { .member-public-card { border-radius:19px; }.member-public-cover { padding:20px; height:91px; }.member-public-cover small { display:none; }.member-public-intro { padding:0 20px 22px; }.member-public-tabs { padding:0 20px; gap:24px; }.member-public-content { padding:24px 20px; }.member-public-about { grid-template-columns:minmax(0,1fr); gap:26px; }.member-public-name h1 { font-size:27px; }.member-public-name { align-items:flex-start; flex-direction:column; gap:10px; }.member-public-back,.member-public-tabs button { min-height:44px; } }
</style>
