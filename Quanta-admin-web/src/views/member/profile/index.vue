<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getCurrentProfile } from '@/api/portal/members'
import { loadBusinessCard, saveBusinessCard } from '@/utils/business-card-storage'
import BusinessCardEditor from './components/BusinessCardEditor.vue'
import BusinessCardPreview from './components/BusinessCardPreview.vue'

const profile = ref(null)
const card = ref({ memberId: '', name: '', title: '', department: '', phone: '', email: '', bio: '' })
const loading = ref(true)
const errorMessage = ref('')
const memberKey = computed(() => profile.value?.memberNo || String(profile.value?.id || ''))

async function load() {
  try {
    profile.value = await getCurrentProfile()
    const stored = loadBusinessCard(memberKey.value)
    card.value = stored || {
      memberId: memberKey.value,
      name: profile.value.name,
      title: profile.value.title,
      department: profile.value.department,
      phone: profile.value.phone,
      email: '',
      bio: '',
    }
  } catch (error) {
    errorMessage.value = error.message || '个人资料加载失败'
  } finally {
    loading.value = false
  }
}

function save(value) {
  try {
    card.value = saveBusinessCard({ ...value, memberId: memberKey.value })
    ElMessage.success('电子名片已保存到当前浏览器')
  } catch (error) {
    ElMessage.error(error.message || '电子名片保存失败')
  }
}

onMounted(load)
</script>

<template>
  <section class="member-page member-profile">
    <header class="member-page__heading">
      <div><p>MEMBER PROFILE</p><h1>个人中心</h1><span>管理你的门户资料与个人电子名片。</span></div>
    </header>
    <p v-if="errorMessage" class="member-feedback is-error" role="alert">{{ errorMessage }}</p>
    <div v-if="loading" class="member-loading portal-card">正在加载个人资料…</div>
    <template v-else-if="profile">
      <article class="member-profile__identity portal-card">
        <div class="member-profile__avatar">{{ profile.name?.slice(0, 1) || 'Q' }}</div>
        <div><p>{{ profile.memberNo || profile.userName }}</p><h2>{{ profile.name }}</h2><span>{{ profile.department }} · {{ profile.title }}</span></div>
      </article>
      <div class="member-profile__card-grid">
        <BusinessCardPreview :card="card" />
        <article class="member-profile__editor portal-card">
          <div class="member-profile__local-note">
            <strong>浏览器本地名片</strong>
            <span>该电子名片仅保存在当前浏览器，不会同步到其他设备或后台。</span>
          </div>
          <BusinessCardEditor v-model="card" @save="save" />
        </article>
      </div>
    </template>
  </section>
</template>

<style src="../member.css"></style>
<style scoped>
.member-profile__identity { display: flex; padding: 24px; align-items: center; gap: 18px; }
.member-profile__avatar { display: grid; width: 72px; height: 72px; border-radius: 20px; background: linear-gradient(135deg, var(--portal-orange), #ff9f47); color: #fff; font-size: 28px; font-weight: 900; place-items: center; }
.member-profile__identity p, .member-profile__identity h2 { margin: 0; }
.member-profile__identity p { color: var(--portal-orange-strong); font-size: 12px; font-weight: 800; }
.member-profile__identity h2 { margin-block: 5px; font-size: 25px; }
.member-profile__identity span { color: var(--portal-muted); }
.member-profile__card-grid { display: grid; margin-top: 20px; grid-template-columns: minmax(300px, .8fr) minmax(480px, 1.2fr); gap: 20px; align-items: start; }
.member-profile__editor { padding: 24px; }
.member-profile__local-note { display: grid; margin-bottom: 20px; padding: 14px 16px; border-radius: 14px; background: var(--portal-orange-soft); gap: 4px; }
.member-profile__local-note strong { color: var(--portal-orange-strong); }
.member-profile__local-note span { color: var(--portal-muted); font-size: 12px; line-height: 1.6; }
@media (max-width: 900px) { .member-profile__card-grid { grid-template-columns: 1fr; } }
@media (max-width: 520px) { .member-profile__identity { padding: 20px 18px; align-items: flex-start; } .member-profile__avatar { width: 58px; height: 58px; border-radius: 17px; font-size: 23px; flex: 0 0 auto; } .member-profile__identity h2 { font-size: 22px; overflow-wrap: anywhere; } .member-profile__identity span { overflow-wrap: anywhere; } .member-profile__editor { padding: 20px 18px; } }
</style>
