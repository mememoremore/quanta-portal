<script setup>
import { onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Lock, User } from '@element-plus/icons-vue'
import { getCaptcha } from '@/api/auth'
import { useUserStore } from '@/stores/user'
import { usePermissionStore } from '@/stores/permission'
import { safeAudienceRedirect } from '@/utils/redirect'
import PortalNoticeDialog from '@/components/PortalNoticeDialog.vue'
import LoginBrand from '@/components/identity-login/LoginBrand.vue'
import BackToIdentity from '@/components/identity-login/BackToIdentity.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const permissionStore = usePermissionStore()
const formRef = ref()
const loading = ref(false)
const accessDeniedVisible = ref(false)
const captchaEnabled = ref(false)
const captchaImage = ref('')
const isDev = import.meta.env.DEV
const isMockMode = String(import.meta.env.VITE_USE_MOCK ?? 'false') === 'true'

const form = reactive({
  username: '',
  password: '',
  code: '',
  uuid: '',
  loginType: '1',
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  code: [
    {
      validator: (_, value, callback) => {
        if (captchaEnabled.value && !value) callback(new Error('请输入验证码'))
        else callback()
      },
      trigger: 'blur',
    },
  ],
}

async function loadCaptcha() {
  try {
    const response = await getCaptcha()
    captchaEnabled.value = response.captchaEnabled !== false
    captchaImage.value = response.img ? `data:image/gif;base64,${response.img}` : ''
    form.uuid = response.uuid || ''
  } catch {
    captchaEnabled.value = false
  }
}

async function submit() {
  if (loading.value) return
  if (!form.username.trim() || !form.password) {
    await formRef.value?.validate().catch(() => false)
    return
  }
  const valid = await formRef.value?.validate().catch(() => false)
  if (valid !== true) return

  loading.value = true
  try {
    await userStore.login({ ...form }, 'admin')
    await userStore.fetchUserInfo()
    if (!userStore.canAccessAdmin) {
      await userStore.logout().catch(() => {})
      permissionStore.resetRoutes()
      form.password = ''
      form.code = ''
      accessDeniedVisible.value = true
      return
    }
    ElMessage.success('登录成功')
    await router.replace(safeAudienceRedirect(route.query.redirect, 'admin'))
  } catch (error) {
    userStore.reset()
    permissionStore.resetRoutes()
    form.password = ''
    form.code = ''
    ElMessage.error(error.message || '登录失败')
    if (captchaEnabled.value) await loadCaptcha()
  } finally {
    loading.value = false
  }
}

watch(
  () => route.query.denied,
  (denied) => {
    if (denied === '1') accessDeniedVisible.value = true
  },
  { immediate: true },
)

onMounted(loadCaptcha)
</script>

<template>
  <main class="login-page">
    <PortalNoticeDialog
      v-model="accessDeniedVisible"
      type="error"
      title="无访问权限"
      message="当前用户无法登录后台管理系统"
    />
    <section class="login-page__brand-panel">
      <LoginBrand class="login-page__brand" />
      <div class="login-page__intro">
        <span class="login-page__eyebrow"><i></i> QUANTA ADMIN CONSOLE</span>
        <h1>聚合每一份热爱，<br />让协作自然发生。</h1>
        <p>连接成员、招新与日常事务，让每一次创造都有迹可循。</p>
        <div class="login-page__capabilities" aria-label="平台能力">
          <span>成员共建</span>
          <span>活动协同</span>
          <span>知识沉淀</span>
        </div>
      </div>
      <div class="login-page__orbit login-page__orbit--large"></div>
      <div class="login-page__orbit login-page__orbit--small"></div>
      <div class="login-page__glow"></div>
    </section>

    <section class="login-page__form-panel">
      <BackToIdentity class="login-page__back" data-testid="back-to-identity" />
      <div class="login-card">
        <LoginBrand class="login-card__mobile-brand" tone="dark" />
        <div v-if="isDev" class="login-card__environment" :class="{ 'is-mock': isMockMode }">
          <i></i>
          {{ isMockMode ? 'Mock 数据' : '真实后端' }}
        </div>
        <div class="login-card__heading">
          <span>WELCOME BACK</span>
          <h2>登录后台管理系统</h2>
          <p>使用你的 Quanta 账号继续访问</p>
        </div>

        <ElForm ref="formRef" :model="form" :rules="rules" size="large" @submit.prevent="submit">
          <ElFormItem prop="username">
            <ElInput v-model="form.username" :prefix-icon="User" placeholder="用户名" autocomplete="username" />
          </ElFormItem>
          <ElFormItem prop="password">
            <ElInput
              v-model="form.password"
              :prefix-icon="Lock"
              placeholder="密码"
              type="password"
              autocomplete="current-password"
              show-password
              @keyup.enter="submit"
            />
          </ElFormItem>
          <ElFormItem v-if="captchaEnabled" prop="code">
            <div class="login-card__captcha">
              <ElInput v-model="form.code" placeholder="验证码" @keyup.enter="submit" />
              <button type="button" aria-label="刷新验证码" @click="loadCaptcha">
                <img v-if="captchaImage" :src="captchaImage" alt="验证码" />
                <span v-else>刷新</span>
              </button>
            </div>
          </ElFormItem>
          <ElButton
            class="login-card__submit quanta-primary-button"
            type="primary"
            native-type="button"
            :loading="loading"
            @click="submit"
          >
            登录
          </ElButton>
        </ElForm>

        <div v-if="isDev" class="login-card__demo">
          <div>
            <span>联调测试账号</span>
            <small>仅开发环境可见</small>
          </div>
          <code>admin / admin123</code>
        </div>
      </div>
      <p class="login-page__copyright">© 2026 Quanta Digital Club</p>
    </section>
  </main>
</template>

<style scoped src="./login.css"></style>
