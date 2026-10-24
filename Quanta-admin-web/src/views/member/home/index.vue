<script setup>
import { onMounted, ref } from 'vue'
import MemberAvatar from '@/components/MemberAvatar.vue'
import { ElMessage } from 'element-plus'
import { getCurrentProfile } from '@/api/portal/members'
import { getTopNotices, markNoticeRead } from '@/api/portal/content'

const profile = ref(null)
const notices = ref([])
const unreadCount = ref(0)
const loading = ref(true)
const errorMessage = ref('')
const selectedNotice = ref(null)

async function load() {
  try {
    const [profileData, noticeData] = await Promise.all([getCurrentProfile(), getTopNotices()])
    profile.value = profileData
    notices.value = noticeData.notices
    unreadCount.value = noticeData.unreadCount
  } catch (error) {
    errorMessage.value = error.message || '塔员首页加载失败'
  } finally {
    loading.value = false
  }
}

async function openNotice(notice) {
  selectedNotice.value = notice
  if (notice.isRead) return
  try {
    await markNoticeRead(notice.id)
    notice.isRead = true
    unreadCount.value = Math.max(0, unreadCount.value - 1)
  } catch (error) {
    ElMessage.error(error.message || '通知状态更新失败')
  }
}

onMounted(load)
</script>

<template>
  <section class="member-page member-home">
    <div class="member-home__hero">
      <div class="member-home__hero-copy"><p>QUANTA MEMBER</p><h1>欢迎回来，{{ profile?.name || 'Quanta 成员' }}</h1><span>从这里继续你的学习、协作和创造。</span></div>
      <img class="member-home__brand-mark" :src="'/quanta-logo-small.jpg'" alt="" aria-hidden="true" width="172" height="172" />
      <RouterLink class="member-home__profile-link" to="/member/profile" aria-label="进入个人中心"><MemberAvatar :member-id="profile?.id" :src="profile?.avatar || ''" :name="profile?.name || '塔员'" :size="88" eager /></RouterLink>
    </div>
    <p v-if="errorMessage" class="member-feedback is-error" role="alert">{{ errorMessage }}</p>
    <div v-if="loading" class="member-loading portal-card">正在加载门户信息…</div>
    <div v-else class="member-home__grid">
      <article class="member-home__notices portal-card">
        <header><div><p>NOTICE BOARD</p><h2>通知公告</h2></div><span v-if="unreadCount">{{ unreadCount }} 条未读</span></header>
        <div v-if="notices.length" class="member-home__notice-list">
          <button v-for="notice in notices" :key="notice.id" type="button" @click="openNotice(notice)">
            <i :class="{ 'is-read': notice.isRead }" /><span><strong>{{ notice.title }}</strong><small>{{ notice.createdAt || '发布时间待定' }}</small></span><b>→</b>
          </button>
        </div>
        <p v-else class="member-empty">暂无通知</p>
        <div v-if="selectedNotice" class="member-home__notice-detail">
          <strong>{{ selectedNotice.title }}</strong>
          <p>{{ selectedNotice.content.replace(/<[^>]+>/g, '') || '暂无正文' }}</p>
        </div>
      </article>
      <aside class="member-home__quick portal-card">
        <p>QUICK ACCESS</p><h2>常用功能</h2>
        <nav>
          <RouterLink to="/member/directory">成员通讯录 <span>→</span></RouterLink>
          <RouterLink to="/member/community">社区论坛 <span>→</span></RouterLink>
          <RouterLink to="/member/materials">学习资料 <span>→</span></RouterLink>
          <RouterLink to="/member/services">我的服务 <span>→</span></RouterLink>
          <RouterLink to="/member/pages">个人网站 <span>→</span></RouterLink>
          <RouterLink to="/member/profile">个人中心 <span>→</span></RouterLink>
        </nav>
      </aside>
    </div>
  </section>
</template>

<style src="../member.css"></style>
<style scoped>
.member-home__hero { display: flex; min-height: 220px; padding: 38px 44px; border-radius: 28px; background: linear-gradient(115deg, #242529, #3a3c42); color: #fff; box-shadow: var(--portal-shadow); align-items: center; justify-content: space-between; }
.member-home__hero { position: relative; overflow: hidden; isolation: isolate; gap: 24px; }
.member-home__hero-copy { position: relative; z-index: 1; min-width: 0; }
.member-home__profile-link { position: relative; z-index: 1; display: inline-flex; flex: 0 0 auto; line-height: 0; }
.member-home__brand-mark { position: absolute; top: 50%; right: clamp(148px, 17%, 240px); width: clamp(112px, 13vw, 172px); height: auto; border-radius: 24%; opacity: .18; filter: blur(.6px); mix-blend-mode: screen; transform: translateY(-50%) rotate(-12deg); mask-image: radial-gradient(ellipse, #000 35%, transparent 73%); pointer-events: none; }
.member-home__hero p { margin: 0; color: #ffad72; font-size: 11px; font-weight: 850; letter-spacing: .16em; }
.member-home__hero h1 { max-width: 680px; margin: 10px 0; font-size: clamp(30px, 4vw, 48px); letter-spacing: -.04em; }
.member-home__hero span { color: #d4d5d8; }
.member-home__avatar { display: grid; width: 108px; height: 108px; border: 8px solid rgb(255 255 255 / 12%); border-radius: 34px; background: var(--portal-orange); font-size: 42px; font-weight: 900; place-items: center; }
.member-home__grid { display: grid; grid-template-columns: minmax(0, 1.5fr) minmax(280px, .65fr); gap: 20px; }
.member-home__notices, .member-home__quick { padding: 25px; }
.member-home__notices header { display: flex; align-items: center; justify-content: space-between; }
.member-home__notices header p, .member-home__quick > p { margin: 0 0 6px; color: var(--portal-orange-strong); font-size: 10px; font-weight: 850; letter-spacing: .14em; }
.member-home__notices h2, .member-home__quick h2 { margin: 0; }
.member-home__notices header > span { padding: 6px 10px; border-radius: 999px; background: var(--portal-orange-soft); color: var(--portal-orange-strong); font-size: 11px; font-weight: 750; }
.member-home__notice-list { display: grid; margin-top: 18px; }
.member-home__notice-list button { display: grid; padding: 15px 2px; border: 0; border-bottom: 1px solid var(--portal-line); background: transparent; grid-template-columns: 10px 1fr auto; align-items: center; gap: 12px; text-align: left; cursor: pointer; }
.member-home__notice-list i { width: 8px; height: 8px; border-radius: 50%; background: var(--portal-orange); }
.member-home__notice-list i.is-read { background: #c8cbd0; }
.member-home__notice-list span { display: grid; gap: 4px; }
.member-home__notice-list small { color: var(--portal-muted); }
.member-home__notice-list b { color: var(--portal-orange); }
.member-home__notice-detail { margin-top: 18px; padding: 16px; border-radius: 14px; background: #f7f7f8; }
.member-home__notice-detail p { margin: 8px 0 0; color: var(--portal-muted); line-height: 1.7; }
.member-home__quick nav { display: grid; margin-top: 18px; gap: 8px; }
.member-home__quick a { display: flex; padding: 14px 15px; border-radius: 12px; background: #f7f7f8; color: var(--portal-ink-soft); font-size: 13px; font-weight: 700; text-decoration: none; justify-content: space-between; }
.member-home__quick a:hover { background: var(--portal-orange-soft); color: var(--portal-orange-strong); }
@media (max-width: 800px) { .member-home__grid { grid-template-columns: 1fr; } .member-home__hero { padding: 30px 26px; } .member-home__avatar { width: 76px; height: 76px; border-radius: 24px; font-size: 30px; } }
@media (max-width: 520px) { .member-home__hero { min-height: 0; padding: 28px 22px; align-items: flex-start; flex-direction: column-reverse; gap: 24px; } .member-home__hero h1 { font-size: 34px; } .member-home__avatar { width: 62px; height: 62px; border-width: 6px; border-radius: 20px; font-size: 25px; } .member-home__notices, .member-home__quick { padding: 22px 18px; } .member-home__notices header { align-items: flex-start; gap: 12px; } .member-home__notice-list button { grid-template-columns: 8px minmax(0, 1fr) auto; gap: 10px; } .member-home__notice-list strong { overflow-wrap: anywhere; } }
@media (max-width: 520px) { .member-home__brand-mark { top: 15px; right: 25px; width: 112px; transform: rotate(-12deg); opacity: .16; } }
</style>
