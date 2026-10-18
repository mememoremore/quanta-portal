<script setup>
import { onMounted, reactive, ref } from 'vue'
import { getCaptcha } from '@/api/auth'
import { useUserStore } from '@/stores/user'

const emit = defineEmits(['signed-in'])
const userStore = useUserStore()
const busy = ref(false)
const error = ref('')
const captcha = ref(null)
const form = reactive({ username: '', password: '', code: '', uuid: '' })
async function loadCaptcha() {
  captcha.value = null
  try {
    const result = await getCaptcha()
    if (result.captchaEnabled !== false && (!result.uuid || !result.img)) throw new Error('验证码加载失败')
    captcha.value = result
    form.uuid = result.uuid || ''
  } catch { error.value = '验证码服务连接失败，请重试。' }
}
async function submit() {
  if (busy.value || !captcha.value) return
  error.value = ''
  if (!form.username.trim() || !form.password || (captcha.value.captchaEnabled !== false && !form.code.trim())) { error.value = '请完整填写账号、密码及验证码。'; return }
  busy.value = true
  try {
    await userStore.login({ ...form, username: form.username.trim() }, 'member')
    emit('signed-in')
  } catch (failure) {
    error.value = failure.message || '登录失败，请重试。'
    await loadCaptcha()
  } finally { form.password = ''; form.code = ''; busy.value = false }
}
onMounted(loadCaptcha)
</script>

<template>
  <form class="oauth-login" @submit.prevent="submit">
    <h1>登录塔员账号</h1><p>登录后继续授权 Quanta Challenge。</p>
    <label>账号<input v-model="form.username" autocomplete="username" placeholder="学号或用户名" :disabled="busy" maxlength="100"></label>
    <label>密码<input v-model="form.password" autocomplete="current-password" type="password" placeholder="请输入密码" :disabled="busy" maxlength="200"></label>
    <label v-if="captcha?.captchaEnabled !== false && captcha">验证码<span class="oauth-captcha"><input v-model="form.code" autocomplete="off" placeholder="请输入验证码" :disabled="busy" maxlength="20"><button type="button" aria-label="更换验证码" :disabled="busy" @click="loadCaptcha"><img :src="`data:image/gif;base64,${captcha.img}`" alt="登录验证码"></button></span></label>
    <p v-if="error" role="alert" class="oauth-error">{{ error }}</p>
    <button v-if="!captcha" type="button" class="oauth-secondary" @click="loadCaptcha">重新加载验证码</button>
    <button class="oauth-primary" type="submit" :disabled="busy || !captcha">{{ busy ? '正在登录…' : '登录并继续' }}</button>
  </form>
</template>
