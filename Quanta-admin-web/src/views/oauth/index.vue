<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { CircleCheck } from '@element-plus/icons-vue'
import { getInteraction, decideInteraction, getOAuthIdentity } from '@/api/oauth'
import { challengeOrigin, interactionId, isOAuthMember, validateInteraction, validateApproval, validateDeniedRedirect, navigateOAuthContinuation, assertUnexpired } from '@/utils/oauth'
import { useUserStore } from '@/stores/user'
import { usePermissionStore } from '@/stores/permission'
import { isDualTokenEnabled } from '@/utils/request'
import { displayPersonName } from '@/utils/person-name'
import LoginBrand from '@/components/identity-login/LoginBrand.vue'
import MemberAvatar from '@/components/MemberAvatar.vue'
import MemberLogin from './MemberLogin.vue'
import './oauth.css'

const route = useRoute()
const userStore = useUserStore()
const permissionStore = usePermissionStore()
const context = ref(null)
const identity = ref(null)
const approval = ref(null)
const phase = ref('loading')
const busy = ref(false)
const message = ref('')
const clock = ref(Date.now())
const returnAt = ref(0)
const returnSeconds = computed(() => Math.max(0, Math.ceil((returnAt.value - clock.value) / 1000)))
let returnTimer
let generation = 0
let alive = true
const expired = computed(() => Boolean(context.value && Date.parse(approval.value?.expiresAt || context.value.expiresAt) <= clock.value))
const name = computed(() => displayPersonName(identity.value, '塔员'))
const returnOrigin = computed(() => context.value?.client.origin || challengeOrigin)
const id = () => interactionId(route.query.interaction)

async function identify(expectedGeneration = generation) {
  phase.value = 'loading'
  try {
    if (!userStore.token && isDualTokenEnabled()) await userStore.restoreSession()
    if (!userStore.token) { if (alive && expectedGeneration === generation) phase.value = 'login'; return }
    const result = await getOAuthIdentity()
    if (!alive || expectedGeneration !== generation) return
    identity.value = result.user
    phase.value = isOAuthMember(identity.value) ? 'consent' : 'ineligible'
    if (phase.value === 'consent' && context.value?.status === 'approved') {
      if (String(context.value.subjectUserId) !== String(identity.value.userId)) throw new Error('授权账号已变化，请重新从 Challenge 发起登录。')
      approval.value = validateApproval({ ...context.value, continuePath: `/oauth2/interaction/${id()}/continue` }, id())
      phase.value = 'complete'
    }
  } catch (failure) {
    if (!alive || expectedGeneration !== generation) return
    if (Number(failure.code || failure.response?.status) === 401) { userStore.reset(); phase.value = 'login' }
    else { phase.value = 'error'; message.value = failure.message || '身份验证失败，请重试。' }
  }
}
async function load() {
  const current = ++generation
  phase.value = 'loading'; message.value = ''; context.value = null; identity.value = null; approval.value = null
  try {
    const value = await getInteraction(id())
    if (!alive || current !== generation) return
    context.value = validateInteraction(value, id())
    await identify(current)
  } catch (failure) { if (alive && current === generation) { phase.value = 'error'; message.value = failure.message } }
}
async function decide(action) {
  if (busy.value || !context.value || expired.value) return
  if (action === 'approve' && (!isOAuthMember(identity.value) || phase.value !== 'consent')) return
  const current = generation
  busy.value = true; message.value = ''
  try {
    assertUnexpired(context.value.expiresAt)
    const result = await decideInteraction(id(), action, context.value.csrfToken)
    if (!alive || current !== generation) return
    if (action === 'deny') { window.location.assign(validateDeniedRedirect(result.redirectUrl, context.value.client.redirectUri)); return }
    approval.value = validateApproval(result, id())
    phase.value = 'complete'
  } catch (failure) {
    if (alive && current === generation) {
      message.value = failure.message
      if (failure.code === 401) { userStore.reset(); identity.value = null; phase.value = 'login' }
    }
  } finally { busy.value = false }
}
function continueToChallenge() {
  if (busy.value || !alive || phase.value !== 'complete') return
  try {
    validateApproval(approval.value, id())
    assertUnexpired(context.value.expiresAt)
    busy.value = true
    navigateOAuthContinuation(id())
  } catch (failure) { busy.value = false; message.value = failure.message; phase.value = 'error' }
}
watch([phase, approval, expired, () => route.query.interaction], () => {
  clearTimeout(returnTimer)
  returnAt.value = 0
  if (phase.value !== 'complete' || expired.value) return
  const current = generation
  clock.value = Date.now()
  returnAt.value = clock.value + 3000
  returnTimer = setTimeout(() => {
    if (alive && current === generation && !expired.value) continueToChallenge()
  }, 3000)
}, { flush: 'sync' })
async function switchAccount() {
  if (busy.value || !window.confirm('切换后将退出当前浏览器中的主站账号，是否继续？')) return
  busy.value = true
  try { await userStore.logout(); permissionStore.resetRoutes(); await load() }
  catch { message.value = '退出失败，请稍后重试。' }
  finally { busy.value = false }
}
watch(() => route.query.interaction, load)
const timer = setInterval(() => { clock.value = Date.now() }, 1000)
onBeforeUnmount(() => { alive = false; generation++; clearInterval(timer); clearTimeout(returnTimer) })
onMounted(load)
</script>

<template>
  <main class="oauth-page" :class="{ 'oauth-page--complete': phase === 'complete' && !expired }">
    <div class="oauth-shell">
      <LoginBrand :tone="phase === 'complete' && !expired ? 'light' : 'dark'" />
      <section class="oauth-panel" aria-live="polite">
        <template v-if="phase === 'loading'"><h1>正在验证授权请求</h1><p>请稍候…</p></template>
        <template v-else-if="expired"><h1>本次授权已过期</h1><p>为保护账号安全，请返回 Challenge 重新发起登录。</p><a class="oauth-primary" :href="`${returnOrigin}/auth/login`">返回 Challenge</a></template>
        <template v-else-if="phase === 'error'"><h1>暂时无法继续登录</h1><p role="alert">{{ message }}</p><button class="oauth-secondary" @click="load">重新检查</button><a class="oauth-primary" :href="`${returnOrigin}/auth/login`">返回 Challenge</a></template>
        <MemberLogin v-else-if="phase === 'login'" @signed-in="identify()" />
        <template v-else-if="phase === 'ineligible'"><h1>该登录方式仅向塔员开放</h1><p>当前账号没有有效塔员身份，请切换账号后重试。</p><button class="oauth-primary" :disabled="busy" @click="switchAccount">切换塔员账号</button><button class="oauth-secondary" :disabled="busy" @click="decide('deny')">取消并返回 Challenge</button></template>
        <template v-else-if="phase === 'complete'">
          <div class="oauth-success" aria-hidden="true"><CircleCheck /></div><h1>登录成功，欢迎回来</h1><p>Quanta 身份验证及 Challenge 授权已完成。<br><span data-testid="oauth-countdown">{{ busy ? '正在安全进入 Challenge…' : `${returnSeconds} 秒后自动进入 Challenge。` }}</span></p>
        </template>
        <template v-else>
          <span class="oauth-eyebrow">CONNECT WITH QUANTA</span><h1>登录 Quanta Challenge</h1><p>请确认使用以下塔员账号登录。</p>
          <div class="oauth-application"><strong>{{ context.client.name }}</strong><small>{{ context.client.origin.replace('https://', '') }}</small></div>
          <div class="oauth-identity"><MemberAvatar :src="identity.avatar || ''" :name="name" :size="44" /><span><strong>{{ name }}</strong><small>Quanta 塔员账号</small></span><button :disabled="busy" @click="switchAccount">切换账号</button></div>
          <div class="oauth-scopes"><strong>本次登录将提供</strong><p>✓ 用于识别账号的唯一标识</p><p>✓ 实名显示名称与头像</p><small>不会提供密码、手机号或主站管理权限。</small></div>
          <div class="oauth-actions"><button class="oauth-secondary" :disabled="busy" @click="decide('deny')">取消</button><button class="oauth-primary" :disabled="busy" data-testid="oauth-approve" @click="decide('approve')">{{ busy ? '正在处理…' : '同意并继续' }}</button></div>
        </template>
        <p v-if="message && phase !== 'error'" role="alert" class="oauth-error">{{ message }}</p>
      </section>
      <small class="oauth-footer">Quanta 统一身份认证 · 仅连接你信任的应用</small>
    </div>
  </main>
</template>
