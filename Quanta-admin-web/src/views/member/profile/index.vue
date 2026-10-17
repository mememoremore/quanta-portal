<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { onBeforeRouteLeave } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Camera } from '@element-plus/icons-vue'
import { getCurrentProfile, getOwnMemberProfile, saveMemberProfile, uploadMemberAvatar } from '@/api/portal/members'
import { useUserStore } from '@/stores/user'
import { setMemberAvatar } from '@/utils/member-avatars'
import { normalizeProfile, validateProfile } from '@/utils/member-profile'
import MemberAvatar from '@/components/MemberAvatar.vue'
import AvatarUploadDialog from './components/AvatarUploadDialog.vue'
import MemberProfileEditor from './components/MemberProfileEditor.vue'
import ProfilePreviewCard from './components/ProfilePreviewCard.vue'
import './profile.css'

const userStore = useUserStore()
const profile = ref(null), draft = ref(normalizeProfile()), saved = ref(normalizeProfile())
const loading = ref(true), saving = ref(false), cloudReady = ref(false), errorMessage = ref(''), cloudError = ref('')
const avatarOpen = ref(false), uploading = ref(false), avatarError = ref('')
let generation = 0, active = true
const dirty = computed(() => JSON.stringify(normalizeProfile(draft.value)) !== JSON.stringify(saved.value))
const preview = computed(() => ({ ...profile.value, ...draft.value }))
const resetDraft = () => { draft.value = JSON.parse(JSON.stringify(saved.value)) }

async function load() {
  const current = ++generation
  loading.value = true; errorMessage.value = ''; cloudError.value = ''; cloudReady.value = false
  try {
    const identity = await getCurrentProfile()
    if (current !== generation) return
    profile.value = identity
    try {
      const data = await getOwnMemberProfile()
      if (current !== generation) return
      if (data.id !== identity.id) throw new Error('服务端返回的本人档案编号不匹配')
      profile.value = { ...identity, ...data }
      saved.value = normalizeProfile(data); resetDraft(); cloudReady.value = true
    } catch (error) {
      if (current === generation) cloudError.value = error.code === 404 ? '云端个人档案接口尚未接通，头像仍可独立设置。' : '云端档案加载失败：' + (error.message || '请稍后重试')
    }
  } catch (error) { if (current === generation) errorMessage.value = error.message || '个人资料加载失败' }
  finally { if (current === generation) loading.value = false }
}
async function save() {
  if (!cloudReady.value || saving.value) return
  const message = validateProfile(draft.value)
  if (message) { ElMessage.error(message); return }
  saving.value = true
  const value = normalizeProfile(draft.value)
  try {
    await saveMemberProfile(value)
    if (!active) return
    saved.value = value; resetDraft(); ElMessage.success('个人档案已保存')
  } catch (error) { if (active) ElMessage.error(error.message || '保存失败，修改内容已保留') }
  finally { saving.value = false }
}
async function upload(file) {
  if (uploading.value) return
  uploading.value = true; avatarError.value = ''
  try {
    const avatar = await uploadMemberAvatar(file)
    if (!active) return
    profile.value.avatar = avatar
    if (userStore.user && Number(userStore.user.userId) === profile.value.id) userStore.user = { ...userStore.user, avatar }
    setMemberAvatar(profile.value.id, avatar); avatarOpen.value = false; ElMessage.success('头像已更新')
  } catch (error) { if (active) avatarError.value = error.message || '上传失败，请重试' }
  finally { uploading.value = false }
}
onBeforeRouteLeave(async () => {
  if (uploading.value || saving.value) { ElMessage.warning('正在保存，请稍候'); return false }
  if (!dirty.value) return true
  try { await ElMessageBox.confirm('个人档案有未保存的修改，确定离开吗？', '离开个人中心', { confirmButtonText: '离开', cancelButtonText: '继续编辑', type: 'warning' }); return true } catch { return false }
})
onMounted(load)
onBeforeUnmount(() => { active = false; generation++ })
</script>

<template>
  <section class="member-page member-profile-page">
    <header class="member-page__heading"><div><p>MEMBER PROFILE</p><h1>个人中心</h1><span>完善个人档案，让塔友更容易认识你。</span></div><RouterLink v-if="profile" class="member-secondary-button" :to="{ path:'/member/people/' + profile.id, state:{ memberReturn:'/member/profile' } }">查看我的成员主页</RouterLink></header>
    <p v-if="errorMessage" class="member-feedback is-error" role="alert">{{ errorMessage }} <button type="button" @click="load">重试</button></p>
    <div v-if="loading" class="member-loading portal-card">正在加载个人资料…</div>
    <template v-else-if="profile">
      <article class="member-profile-identity portal-card"><button type="button" class="member-profile-avatar-action" aria-label="更换头像" @click="avatarError = ''; avatarOpen = true"><MemberAvatar :member-id="profile.id" :src="profile.avatar" :name="profile.name" :size="76" eager /><span aria-hidden="true"><Camera /></span></button><div class="member-profile-identity__text"><h2>{{ profile.name }}</h2><div class="member-profile-meta"><span>{{ profile.department }}</span><span>{{ profile.title }} · {{ profile.cohort }}</span></div><small>成员编号 {{ profile.memberNo || '暂无' }} · 身份信息由社团维护</small></div><button type="button" class="member-secondary-button" @click="avatarError = ''; avatarOpen = true">更换头像</button></article>
      <p v-if="cloudError" class="member-feedback is-error" role="alert">{{ cloudError }} <button type="button" @click="load">重新加载</button></p>
      <div class="member-profile-columns"><article class="member-profile-editor portal-card"><header><h2>个人档案</h2><span>{{ cloudReady ? '云端同步' : '尚未连接' }}</span></header><MemberProfileEditor v-model="draft" :disabled="!cloudReady" :saving="saving" :dirty="dirty" @save="save" @cancel="resetDraft" /></article><aside class="member-profile-aside"><div class="member-profile-preview-heading"><strong>塔员名片</strong><span>预览</span></div><ProfilePreviewCard :profile="preview" /><RouterLink class="member-secondary-button" :to="{ path:'/member/people/' + profile.id, state:{ memberReturn:'/member/profile' } }">预览已保存的成员主页</RouterLink><p class="member-profile-note"><strong>同一份档案，同一个你</strong>头像用于顶部菜单、首页、通讯录、帖子和评论。成员主页仅向已登录塔员开放。</p><p class="member-profile-note portal-card"><strong>身份资料保持真实</strong>姓名、部门、角色和届数由社团维护，不能通过名片编辑改变。手机号与登录邮箱不会展示。</p></aside></div>
    </template>
    <AvatarUploadDialog v-model="avatarOpen" :uploading="uploading" :error="avatarError" @confirm="upload" />
  </section>
</template>

<style src="../member.css"></style>
