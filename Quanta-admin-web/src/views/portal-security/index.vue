<script setup>
import { computed, onBeforeUnmount, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Hide, View } from '@element-plus/icons-vue'
import { changePassword, sendFreshmanEmailCode, updateFreshmanEmail } from '@/api/portal/account'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const userStore = useUserStore()
const isFreshman = computed(() => route.meta.portalAudience === 'freshman')
const form = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
const passwordVisible = reactive({ oldPassword: false, newPassword: false, confirmPassword: false })
const emailForm = reactive({ email: '', emailCode: '' })
const submitting = ref(false)
const emailSubmitting = ref(false)
const emailCodeSending = ref(false)
const emailCodeCooldown = ref(0)
const emailCodeTarget = ref('')
const errorMessage = ref('')
const emailError = ref('')
const emailCodeError = ref('')
const currentEmail = computed(() => userStore.user?.email || '')
const emailPattern = /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/
let emailCodeTimer

watch(currentEmail, (value) => { emailForm.email = value }, { immediate: true })

function validateNewEmail(email) {
  if (!email) return '请输入邮箱'
  if (email.length > 50 || !emailPattern.test(email)) return '请输入正确的邮箱'
  if (email === currentEmail.value) return '新邮箱与当前邮箱相同'
  return ''
}

function startEmailCodeCooldown() {
  emailCodeCooldown.value = 60
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
}

async function sendEmailCode() {
  if (emailCodeSending.value || emailCodeCooldown.value > 0) return
  const email = emailForm.email.trim()
  emailError.value = validateNewEmail(email)
  if (emailError.value) return

  emailCodeSending.value = true
  try {
    await sendFreshmanEmailCode(email)
    emailCodeTarget.value = email
    emailCodeError.value = ''
    startEmailCodeCooldown()
    ElMessage.success('验证码已发送，5 分钟内有效')
  } catch (error) {
    ElMessage.error(error.message || '验证码发送失败')
  } finally {
    emailCodeSending.value = false
  }
}

function validate() {
  if (!form.oldPassword || !form.newPassword || !form.confirmPassword) return '请完整填写密码信息'
  if (form.newPassword.length < 6) return '新密码至少需要 6 位'
  if (form.newPassword !== form.confirmPassword) return '两次输入的新密码不一致'
  if (form.oldPassword === form.newPassword) return '新密码不能与旧密码相同'
  return ''
}

async function submit() {
  if (submitting.value) return
  errorMessage.value = validate()
  if (errorMessage.value) return
  submitting.value = true
  try {
    await changePassword({ oldPassword: form.oldPassword, newPassword: form.newPassword })
    form.oldPassword = ''
    form.newPassword = ''
    form.confirmPassword = ''
    ElMessage.success('密码修改成功')
  } catch (error) {
    errorMessage.value = error.message || '密码修改失败'
    ElMessage.error(errorMessage.value)
  } finally {
    submitting.value = false
  }
}

async function submitEmail() {
  if (emailSubmitting.value) return
  const email = emailForm.email.trim()
  const emailCode = emailForm.emailCode.trim()
  emailError.value = validateNewEmail(email)
  emailCodeError.value = /^\d{6}$/.test(emailCode) ? '' : '请输入6位邮箱验证码'
  if (!emailCodeError.value && emailCodeTarget.value !== email) emailCodeError.value = '请先向该邮箱发送验证码'
  if (emailError.value || emailCodeError.value) return

  emailSubmitting.value = true
  try {
    await updateFreshmanEmail({ email, emailCode })
    await userStore.fetchUserInfo()
    emailForm.emailCode = ''
    emailCodeTarget.value = ''
    emailCodeCooldown.value = 0
    clearInterval(emailCodeTimer)
    emailCodeTimer = undefined
    ElMessage.success('邮箱修改成功')
  } catch (error) {
    emailError.value = error.message || '邮箱修改失败'
    ElMessage.error(emailError.value)
  } finally {
    emailSubmitting.value = false
  }
}

onBeforeUnmount(() => clearInterval(emailCodeTimer))
</script>

<template>
  <section class="portal-security">
    <header>
      <p>ACCOUNT SECURITY</p>
      <h1>账号与安全</h1>
      <span>{{ isFreshman ? '维护你的登录密码与联系邮箱，确保招新通知能够及时送达。' : '定期更新密码，并避免在多个平台使用相同密码。' }}</span>
    </header>
    <div class="portal-security__grid" :class="{ 'has-email-card': isFreshman }">
      <form class="portal-security__card portal-card" @submit.prevent="submit">
        <div class="portal-security__intro">
          <span aria-hidden="true">•••</span>
          <div><h2>修改登录密码</h2><p>修改后，下次登录请使用新密码。</p></div>
        </div>
        <p v-if="errorMessage" class="portal-security__error" role="alert">{{ errorMessage }}</p>
        <label>当前密码<span class="portal-security__password-field"><input v-model="form.oldPassword" name="oldPassword" :type="passwordVisible.oldPassword ? 'text' : 'password'" autocomplete="current-password" required /><button data-testid="toggle-old-password" type="button" :aria-label="passwordVisible.oldPassword ? '隐藏当前密码' : '显示当前密码'" @click="passwordVisible.oldPassword = !passwordVisible.oldPassword"><component :is="passwordVisible.oldPassword ? Hide : View" /></button></span></label>
        <label>新密码<span class="portal-security__password-field"><input v-model="form.newPassword" name="newPassword" :type="passwordVisible.newPassword ? 'text' : 'password'" autocomplete="new-password" minlength="6" required /><button data-testid="toggle-new-password" type="button" :aria-label="passwordVisible.newPassword ? '隐藏新密码' : '显示新密码'" @click="passwordVisible.newPassword = !passwordVisible.newPassword"><component :is="passwordVisible.newPassword ? Hide : View" /></button></span></label>
        <label>确认新密码<span class="portal-security__password-field"><input v-model="form.confirmPassword" name="confirmPassword" :type="passwordVisible.confirmPassword ? 'text' : 'password'" autocomplete="new-password" minlength="6" required /><button data-testid="toggle-confirm-password" type="button" :aria-label="passwordVisible.confirmPassword ? '隐藏确认密码' : '显示确认密码'" @click="passwordVisible.confirmPassword = !passwordVisible.confirmPassword"><component :is="passwordVisible.confirmPassword ? Hide : View" /></button></span></label>
        <button class="portal-security__submit" type="submit" :disabled="submitting">
          {{ submitting ? '提交中…' : '确认修改' }}
        </button>
      </form>

      <form v-if="isFreshman" class="portal-security__card portal-card" data-testid="email-card" @submit.prevent="submitEmail">
        <div class="portal-security__intro portal-security__intro--email">
          <span aria-hidden="true">@</span>
          <div><h2>修改联系邮箱</h2><p>面试安排和重要通知将发送到此邮箱。</p></div>
        </div>
        <div class="portal-security__current-email">
          <span>当前邮箱</span>
          <strong>{{ currentEmail || '暂未设置' }}</strong>
        </div>
        <p v-if="emailError" class="portal-security__error" role="alert">{{ emailError }}</p>
        <label>新邮箱<input v-model.trim="emailForm.email" name="email" type="email" autocomplete="email" maxlength="50" placeholder="name@example.com" required /></label>
        <label>
          新邮箱验证码
          <span class="portal-security__email-code-row">
            <input v-model.trim="emailForm.emailCode" name="emailCode" type="text" inputmode="numeric" autocomplete="one-time-code" maxlength="6" placeholder="请输入 6 位验证码" required />
            <button
              data-testid="send-new-email-code"
              class="portal-security__email-code-button"
              type="button"
              :disabled="emailCodeSending || emailCodeCooldown > 0"
              @click="sendEmailCode"
            >
              {{ emailCodeCooldown > 0 ? `${emailCodeCooldown} 秒后重新发送` : (emailCodeSending ? '发送中…' : '发送验证码') }}
            </button>
          </span>
          <small v-if="emailCodeError" class="portal-security__field-error">{{ emailCodeError }}</small>
          <small v-else class="portal-security__field-help">验证码 5 分钟内有效</small>
        </label>
        <button data-testid="email-submit" class="portal-security__submit" type="submit" :disabled="emailSubmitting">
          {{ emailSubmitting ? '提交中…' : '保存邮箱' }}
        </button>
      </form>
    </div>
  </section>
</template>

<style scoped src="./security.css"></style>
