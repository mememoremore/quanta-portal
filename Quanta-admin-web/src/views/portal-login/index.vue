<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getCaptcha, registerFreshman, sendRegisterEmailCode } from '@/api/auth'
import { useUserStore } from '@/stores/user'
import { safeAudienceRedirect } from '@/utils/redirect'
import LoginBrand from '@/components/identity-login/LoginBrand.vue'
import BackToIdentity from '@/components/identity-login/BackToIdentity.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const formRef = ref()
const registerFormRef = ref()
const loading = ref(false)
const registering = ref(false)
let registrationAttemptLocked = false
const mode = ref('login')
const captchaEnabled = ref(false)
const captchaImage = ref('')
const audience = computed(() => (route.meta.portalAudience === 'freshman' ? 'freshman' : 'member'))
const isFreshman = computed(() => audience.value === 'freshman')
const isRegisterMode = computed(() => isFreshman.value && mode.value === 'register')
const audienceLabel = computed(() => (audience.value === 'freshman' ? '新生' : '塔员'))
const audienceCopy = computed(() => audience.value === 'freshman'
  ? '了解 Quanta，提交你的招新报名，随时查看面试进展。'
  : '连接成员与社团服务，让每一次协作都有清晰入口。')
const form = reactive({ username: '', password: '', code: '', uuid: '' })
const registerForm = reactive({ studentNo: '', email: '', emailCode: '', password: '', confirmPassword: '', code: '', uuid: '' })
const registrationErrors = reactive({ studentNo: '', email: '', emailCode: '', password: '', confirmPassword: '' })
const emailCodeCooldown = ref(0)
const hasSentEmailCode = ref(false)
let emailCodeTimer

const rules = {
  username: [{ required: true, message: '请输入学号或用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  code: [{
    validator: (_, value, callback) => {
      if (captchaEnabled.value && !value) callback(new Error('请输入验证码'))
      else callback()
    },
    trigger: 'blur',
  }],
}

const registerRules = {
  studentNo: [
    { required: true, message: '请输入学号', trigger: 'blur' },
    {
      validator: (_, value, callback) => {
        if (!/^20\d{9}$/.test(value || '')) callback(new Error('请输入11位学号，例如 20241003193'))
        else callback()
      },
      trigger: ['blur', 'change'],
    },
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱', trigger: ['blur', 'change'] },
  ],
  emailCode: [
    { required: true, message: '请输入邮箱验证码', trigger: 'blur' },
    { pattern: /^\d{6}$/, message: '请输入6位邮箱验证码', trigger: ['blur', 'change'] },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 5, max: 20, message: '密码长度需为 5–20 位', trigger: ['blur', 'change'] },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (_, value, callback) => {
        if (value !== registerForm.password) callback(new Error('两次输入的密码不一致'))
        else callback()
      },
      trigger: ['blur', 'change'],
    },
  ],
  code: [{
    validator: (_, value, callback) => {
      if (captchaEnabled.value && !value) callback(new Error('请输入验证码'))
      else callback()
    },
    trigger: 'blur',
  }],
}

async function loadCaptcha() {
  try {
    const response = await getCaptcha()
    captchaEnabled.value = response.captchaEnabled !== false
    captchaImage.value = response.img ? `data:image/gif;base64,${response.img}` : ''
    form.uuid = response.uuid || ''
    registerForm.uuid = response.uuid || ''
  } catch {
    captchaEnabled.value = false
  }
}

async function switchMode(nextMode) {
  if (!isFreshman.value) return
  mode.value = nextMode
  await nextTick()
  formRef.value?.clearValidate()
  registerFormRef.value?.clearValidate()
}

async function sendEmailCode() {
  if (emailCodeCooldown.value > 0) return
  if (!/^20\d{9}$/.test(registerForm.studentNo)) {
    registrationErrors.studentNo = '请输入11位学号，例如 20241003193'
    return
  }
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(registerForm.email)) {
    registrationErrors.email = '请输入正确的邮箱'
    return
  }
  registrationErrors.studentNo = ''
  registrationErrors.email = ''
  try {
    await sendRegisterEmailCode({
      email: registerForm.email,
      studentNo: registerForm.studentNo,
    })
    hasSentEmailCode.value = true
    emailCodeCooldown.value = 60
    ElMessage.success('验证码已发送，5 分钟内有效')
    clearInterval(emailCodeTimer)
    emailCodeTimer = setInterval(() => {
      if (emailCodeCooldown.value <= 1) {
        emailCodeCooldown.value = 0
        clearInterval(emailCodeTimer)
        emailCodeTimer = undefined
      } else {
        emailCodeCooldown.value -= 1
      }
    }, 1000)
  } catch (error) {
    ElMessage.error(error.message || '验证码发送失败')
  }
}

function syncFieldFromDom(name, current, { trim = false } = {}) {
  const existing = trim ? String(current || '').trim() : String(current || '')
  if (existing) return existing
  const input = formRef.value?.$el?.querySelector?.(`input[name="${name}"]`)
  const value = String(input?.value || '')
  return trim ? value.trim() : value
}

function rejectedByServer(error) {
  return Boolean(error?.payload)
}

async function submit() {
  if (loading.value) return
  const requestedAudience = audience.value
  const intended = safeAudienceRedirect(route.query.redirect, requestedAudience)
  loading.value = true
  try {
    form.username = syncFieldFromDom('username', form.username, { trim: true })
    form.password = syncFieldFromDom('password', form.password)
    if (!form.username.trim() || !form.password) {
      ElMessage.warning(form.password ? '请输入学号或用户名' : '请输入密码')
      await formRef.value?.validate().catch(() => false)
      return
    }
    const valid = await formRef.value?.validate().catch(() => false)
    if (valid !== true) return

    await userStore.login({ ...form }, requestedAudience)
    await userStore.fetchUserInfo()
    if (userStore.serverAudience !== requestedAudience) {
      throw new Error(requestedAudience === 'member' ? '该账号不是塔员，请使用新生入口登录' : '该账号已是塔员，请使用塔员入口登录')
    }
    await router.replace(intended)
    ElMessage.success('登录成功')
  } catch (error) {
    userStore.reset()
    if (rejectedByServer(error)) {
      form.password = ''
      form.code = ''
    }
    ElMessage.error(error.message || '登录失败')
    if (captchaEnabled.value) await loadCaptcha()
  } finally {
    loading.value = false
  }
}

async function submitRegistration() {
  if (registrationAttemptLocked) return
  registrationAttemptLocked = true
  try {
    registrationErrors.studentNo = /^20\d{9}$/.test(registerForm.studentNo)
      ? ''
      : '请输入11位学号，例如 20241003193'
    registrationErrors.email = /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(registerForm.email)
      ? ''
      : '请输入正确的邮箱'
    registrationErrors.emailCode = /^\d{6}$/.test(registerForm.emailCode)
      ? ''
      : '请输入6位邮箱验证码'
    registrationErrors.password = registerForm.password.length >= 5 && registerForm.password.length <= 20
      ? ''
      : '密码长度需为 5–20 位'
    registrationErrors.confirmPassword = registerForm.confirmPassword === registerForm.password
      ? ''
      : '两次输入的密码不一致'
    if (Object.values(registrationErrors).some(Boolean)) return
    const valid = await registerFormRef.value?.validate().catch(() => false)
    if (valid !== true) return
    registering.value = true
    await registerFreshman({
      studentNo: registerForm.studentNo,
      email: registerForm.email,
      emailCode: registerForm.emailCode,
      password: registerForm.password,
      code: registerForm.code,
      uuid: registerForm.uuid,
    })
    form.username = registerForm.studentNo
    form.password = ''
    registerForm.password = ''
    registerForm.confirmPassword = ''
    registerForm.emailCode = ''
    registerForm.code = ''
    await switchMode('login')
    ElMessage.success('注册成功，请使用学号登录')
  } catch (error) {
    registerForm.password = ''
    registerForm.confirmPassword = ''
    registerForm.code = ''
    ElMessage.error(error.message || '注册失败')
    if (captchaEnabled.value) await loadCaptcha()
  } finally {
    registering.value = false
    registrationAttemptLocked = false
  }
}

onMounted(loadCaptcha)
onBeforeUnmount(() => clearInterval(emailCodeTimer))
</script>

<template>
  <main class="portal-login">
    <section class="portal-login__brand-panel">
      <LoginBrand class="portal-login__brand" link />
      <div class="portal-login__brand-copy">
        <p><i></i> {{ audience === 'freshman' ? 'JOIN QUANTA' : 'MEMBER SPACE' }}</p>
        <h1>{{ audience === 'freshman' ? '从这里，开始你的 Quanta 旅程。' : '欢迎回来，继续一起创造。' }}</h1>
        <span>{{ audienceCopy }}</span>
      </div>
      <div class="portal-login__brand-index" aria-hidden="true">{{ audience === 'freshman' ? '01' : '02' }}</div>
    </section>
    <section class="portal-login__form-panel">
      <BackToIdentity class="portal-login__back" />
      <div class="portal-login__card">
        <span class="portal-login__identity">{{ audienceLabel }} PORTAL</span>
        <div v-if="isFreshman" class="portal-login__mode-switch" aria-label="新生登录或注册">
          <button type="button" :class="{ active: !isRegisterMode }" @click="switchMode('login')">登录</button>
          <button data-testid="register-mode" type="button" :class="{ active: isRegisterMode }" @click="switchMode('register')">注册</button>
        </div>
        <h2>{{ audienceLabel }}{{ isRegisterMode ? '注册' : '登录' }}</h2>
        <p class="portal-login__welcome">
          {{ isRegisterMode ? '使用学号创建你的 Quanta 新生账号' : '使用你的 Quanta 账号继续访问' }}
        </p>
        <ElForm v-if="!isRegisterMode" key="login" ref="formRef" :model="form" :rules="rules" size="large" @submit.prevent="submit">
          <ElFormItem prop="username" :label="isFreshman ? '学号' : '学号/用户名'">
            <ElInput v-model.trim="form.username" name="username" autocomplete="username" :placeholder="isFreshman ? '请输入学号' : '请输入学号或用户名'" />
          </ElFormItem>
          <ElFormItem prop="password" label="密码">
            <ElInput v-model="form.password" name="password" autocomplete="current-password" type="password" show-password placeholder="请输入密码" @keyup.enter="submit" />
          </ElFormItem>
          <ElFormItem v-if="captchaEnabled" prop="code" label="验证码">
            <div class="portal-login__captcha">
              <ElInput v-model="form.code" placeholder="请输入验证码" @keyup.enter="submit" />
              <button type="button" aria-label="刷新验证码" @click="loadCaptcha">
                <img v-if="captchaImage" :src="captchaImage" alt="验证码" />
                <span v-else>刷新</span>
              </button>
            </div>
          </ElFormItem>
          <ElButton class="portal-login__submit" type="primary" native-type="submit" :loading="loading">
            登录并进入{{ audienceLabel }}门户
          </ElButton>
        </ElForm>
        <ElForm v-else key="register" ref="registerFormRef" :model="registerForm" :rules="registerRules" size="large" @submit.prevent="submitRegistration">
          <ElFormItem prop="studentNo" label="学号">
            <ElInput v-model.trim="registerForm.studentNo" name="studentNo" autocomplete="username" maxlength="11" placeholder="例如：20241003193" />
            <span v-if="registrationErrors.studentNo" class="portal-login__field-error">{{ registrationErrors.studentNo }}</span>
          </ElFormItem>
          <ElFormItem prop="email" label="邮箱">
            <ElInput v-model.trim="registerForm.email" name="email" autocomplete="email" placeholder="请输入常用邮箱" />
            <span v-if="registrationErrors.email" class="portal-login__field-error">{{ registrationErrors.email }}</span>
          </ElFormItem>
          <ElFormItem prop="emailCode" label="邮箱验证码">
            <div class="portal-login__email-code-row">
              <ElInput v-model.trim="registerForm.emailCode" name="emailCode" inputmode="numeric" maxlength="6" placeholder="请输入 6 位验证码" />
              <ElButton
                data-testid="send-email-code"
                class="portal-login__email-code-button"
                native-type="button"
                :disabled="emailCodeCooldown > 0"
                @click="sendEmailCode"
              >
                {{ emailCodeCooldown > 0 ? `${emailCodeCooldown} 秒后重新发送` : (hasSentEmailCode ? '重新发送' : '发送验证码') }}
              </ElButton>
            </div>
            <span v-if="registrationErrors.emailCode" class="portal-login__field-error">{{ registrationErrors.emailCode }}</span>
            <span v-else class="portal-login__field-help">验证码 5 分钟内有效</span>
          </ElFormItem>
          <ElFormItem prop="password" label="密码">
            <ElInput v-model="registerForm.password" name="registerPassword" autocomplete="new-password" type="password" show-password placeholder="请输入 5–20 位密码" />
            <span v-if="registrationErrors.password" class="portal-login__field-error">{{ registrationErrors.password }}</span>
          </ElFormItem>
          <ElFormItem prop="confirmPassword" label="确认密码">
            <ElInput v-model="registerForm.confirmPassword" name="confirmPassword" autocomplete="new-password" type="password" show-password placeholder="请再次输入密码" @keyup.enter="submitRegistration" />
            <span v-if="registrationErrors.confirmPassword" class="portal-login__field-error">{{ registrationErrors.confirmPassword }}</span>
          </ElFormItem>
          <ElFormItem v-if="captchaEnabled" prop="code" label="验证码">
            <div class="portal-login__captcha">
              <ElInput v-model="registerForm.code" placeholder="请输入验证码" @keyup.enter="submitRegistration" />
              <button type="button" aria-label="刷新验证码" @click="loadCaptcha">
                <img v-if="captchaImage" :src="captchaImage" alt="验证码" />
                <span v-else>刷新</span>
              </button>
            </div>
          </ElFormItem>
          <ElButton data-testid="register-submit" class="portal-login__submit" type="primary" native-type="button" :loading="registering" @click="submitRegistration">
            创建新生账号
          </ElButton>
        </ElForm>
        <p class="portal-login__hint">
          {{ isRegisterMode ? '注册成功后，请使用学号和密码登录' : '登录即代表你正在访问 Quanta 社团统一服务门户' }}
        </p>
      </div>
    </section>
  </main>
</template>

<style scoped src="./portal-login.css"></style>
