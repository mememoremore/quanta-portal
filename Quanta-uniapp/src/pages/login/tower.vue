<template>
	<view class="freshman-page">
		<view class="top-bar">
			<image class="back-button" :src="backIconSrc" mode="aspectFit" @click="handleBack" />
			<text class="page-title">塔员登录</text>
			<view class="top-bar-spacer"></view>
		</view>

		<view class="brand-section">
			<view class="brand-avatar-shell">
				<image class="brand-avatar" src="/static/picture/quanta.png" mode="aspectFill" />
			</view>
			<text class="brand-text">
				<text class="brand-q">Q</text><text class="brand-rest">uant</text><text class="brand-a">a</text>
			</text>
		</view>

		<view class="form-card">
			<view class="field-group student-group">
				<text class="field-label">工号</text>
				<view class="input-shell">
					<input class="field-input" type="text"
          placeholder="请输入工号"
          placeholder-class="field-placeholder"
          v-model="tower_id"
					:disabled="loading" />
				</view>
			</view>

			<view class="field-group password-group">
				<text class="field-label">密码</text>
				<view class="input-shell input-shell-password">
					<input
						class="field-input password-input"
						:password="!showPassword"
						type="text"
						placeholder="请输入密码"
						placeholder-class="field-placeholder"
						v-model="tower_password"
						:disabled="loading"
					/>
					<image
						class="eye-icon"
						:src="showPassword ? eyeOpenIconSrc : eyeClosedIconSrc"
						mode="aspectFit"
						@click="togglePasswordVisible"
					/>
				</view>
			</view>

			<text class="problem-text">遇到问题？</text>

			<view
				class="login-button"
				:class="{ 'login-button--loading': loading }"
				@click="handleLogin"
			>
				<text class="login-button-text">{{ loading ? '登录中...' : '登录' }}</text>
			</view>
		</view>

	</view>
</template>

<script setup lang="js">
import { ref } from 'vue'
import { towerMemberLoginApi } from '../../api/auth'
import { completeLogin } from '../../hooks/useLogin'

const showPassword = ref(false)

const backIconSrc = '/static/picture/return.png'
const eyeOpenIconSrc = `data:image/svg+xml;utf8,${encodeURIComponent('<svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 20 20" fill="none"><path d="M1.66663 10C1.66663 10 4.99996 3.33331 10 3.33331C15 3.33331 18.3333 10 18.3333 10C18.3333 10 15 16.6666 10 16.6666C4.99996 16.6666 1.66663 10 1.66663 10Z" stroke="#9CA3AF" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/><path d="M10 12.5C11.3807 12.5 12.5 11.3807 12.5 10C12.5 8.61929 11.3807 7.5 10 7.5C8.61929 7.5 7.5 8.61929 7.5 10C7.5 11.3807 8.61929 12.5 10 12.5Z" stroke="#9CA3AF" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/></svg>')}`
const eyeClosedIconSrc = `data:image/svg+xml;utf8,${encodeURIComponent('<svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 20 20" fill="none"><path d="M3.33331 3.33331L16.6666 16.6666" stroke="#9CA3AF" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/><path d="M9.16663 9.16663C8.88889 9.44437 8.75 9.7777 8.75 10C8.75 10.6904 9.30965 11.25 10 11.25C10.2223 11.25 10.5556 11.1111 10.8333 10.8333" stroke="#9CA3AF" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/><path d="M5.83331 5.83331C4.1624 6.84709 2.66663 8.75 1.66663 10C1.66663 10 5 16.6666 10 16.6666C11.1371 16.6666 12.1862 16.3598 13.1302 15.875" stroke="#9CA3AF" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/><path d="M8.91248 4.99177C9.26748 4.93774 9.63077 4.91663 10 4.91663C15 4.91663 18.3333 10 18.3333 10C17.5225 11.2362 16.4243 12.5197 15.2451 13.4807" stroke="#9CA3AF" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/></svg>')}`

//返回上一级
const handleBack = () => {
	uni.navigateBack({ delta: 1 })
}

const togglePasswordVisible = () => {
	showPassword.value = !showPassword.value
}

//定义工号和密码的响应式变量并且通过v-model绑定到输入框上
const tower_id = ref('')
const tower_password = ref('')

const loading = ref(false)

//验证工号和密码是否为空
const validateTowerLogin = () => {
	//工号非空校验 只输入空格也算空
	if (!tower_id.value.trim()) {
		uni.showToast({
			title: '请输入工号',
			icon: 'none',
		})
		return false
	}

	//密码非空校验 只输入空格也算空
	if (!tower_password.value.trim()) {
		uni.showToast({
			title: '请输入密码',
			icon: 'none',
		})
		return false
	}
	tower_id.value = tower_id.value.trim()
	tower_password.value = tower_password.value.trim()
	return true
}

//用户点击登录按钮后的处理函数
const handleLogin = async () => {
	if (!validateTowerLogin()) {
		return
	}

	if (loading.value) {
		uni.showToast({
			title: '请勿重复提交',
			icon: 'none',
		})
		return
	}
	loading.value = true

	try {
		const res = await towerMemberLoginApi({
			username: tower_id.value,
			password: tower_password.value,
			loginType: '1',
		})

		await completeLogin(res, 'tower', tower_id.value)
		uni.showToast({
			title: res.msg || '登录成功',
			icon: 'success',
		})
		setTimeout(() => {
			uni.reLaunch({ url: '/pages/member/home/home' })
		}, 500)
	} catch (error) {
		uni.showToast({
			title: error.message || '登录失败',
			icon: 'none',
		})
	} finally {
		loading.value = false
	}
}
</script>

<style scoped>
.freshman-page {
	min-height: 100vh;
	box-sizing: border-box;
	padding: 38rpx 44rpx 48rpx;
background: 
  linear-gradient(135deg, #FFEDD466 0%, #FFFFFF 45%, #FFFFFF 55%, #FFEDD466 100%),
  linear-gradient(45deg, #FFEDD466 0%, #FFFFFF 45%, #FFFFFF 55%, #FFEDD466 100%);
	display: flex;
	flex-direction: column;
	align-items: center;
}

.top-bar {
	width: 100%;
	display: flex;
	align-items: center;
	justify-content: space-between;
}

.top-bar-spacer {
	width: 42rpx;
	height: 42rpx;
}

.back-button {
	width: 42rpx;
	height: 42rpx;
	margin-left: 2rpx;
	margin-top: 52rpx;
	opacity: 1;
	flex-shrink: 0;
}

.page-title {
	width: 112rpx;
	height: 40rpx;
	margin-top: 52rpx;
	font-family: Inter, -apple-system, sans-serif;
	font-size: 28rpx;
	line-height: 40rpx;
	font-weight: 500;
	color: #6A7282;
	text-align: center;
}


.brand-section {
	margin-top: 52rpx;
	display: flex;
	justify-content: center;
	align-items: center;
	flex-direction: column;
}

.brand-text {
	display: flex;
	align-items: center;
	justify-content: center;
	font-family: Inknut Antiqua, serif;
	font-size: 64rpx;
	line-height: 64rpx;
	font-weight: 400;
	font-style: normal;
	letter-spacing: 0;
	text-align: center;
}

.brand-avatar-shell {
	width: 172rpx;
	height: 172rpx;
	border-radius: 50%;
	border: 10rpx solid #FFFFFF;
	overflow: hidden;
	box-sizing: border-box;
	margin-bottom: 24rpx;
	flex-shrink: 0;
}

.brand-avatar {
	width: 100%;
	height: 100%;
	display: block;
}

.brand-q {
	color: #FF6600;
}

.brand-rest {
	color: #111111;
}

.brand-a {
	color: #FF6600;
}

.form-card {
	width: 712rpx;
	height: 732rpx;
	margin-top: 104rpx;
	box-sizing: border-box;
	border-radius: 48rpx;
	border: 2rpx solid #F3F4F6;
	border-top: 2rpx solid #F3F4F6;
	background: #FFFFFF;
	box-shadow: 0 16rpx 60rpx 0 #0000000A;
	padding: 66rpx 64rpx 56rpx;
	display: flex;
	flex-direction: column;
}

.field-group {
	width: 100%;
	display: flex;
	flex-direction: column;
}

.student-group {
	margin-top: 0;
}

.password-group {
	margin-top: 36rpx;
}

.field-label {
	font-family: Inter, -apple-system, sans-serif;
	font-size: 28rpx;
	line-height: 40rpx;
	font-weight: 500;
	color: #1E2939;
	letter-spacing: 0;
}

.input-shell {
	width: 574rpx;
	height: 100rpx;
	box-sizing: border-box;
	margin-top: 24rpx;
	padding: 24rpx 32rpx;
	border-radius: 28rpx;
	border: 2rpx solid #E5E7EB;
	border-top: 2rpx solid #E5E7EB;
	background: #F9FAFB;
	display: flex;
	align-items: center;
}

.input-shell-password {
	justify-content: space-between;
	gap: 16rpx;
}

.field-input {
	flex: 1;
	height: 48rpx;
	font-family: Inter, -apple-system, sans-serif;
	font-size: 28rpx;
	line-height: 40rpx;
	color: #1E2939;
}

.password-input {
	width: auto;
}

.field-placeholder {
	color: #9CA3AF;
	font-family: Inter, -apple-system, sans-serif;
	font-size: 28rpx;
	line-height: 40rpx;
}

.eye-icon {
	width: 40rpx;
	height: 40rpx;
	flex-shrink: 0;
}

.problem-text {
	margin-top: 44rpx;
	font-family: Inter, -apple-system, sans-serif;
	font-size: 28rpx;
	line-height: 40rpx;
	font-weight: 500;
	color: #FF6600;
	text-align: center;
	align-self: flex-end;
}

.login-button {
	width: 572rpx;
	height: 104rpx;
	margin-top: 54rpx;
	box-sizing: border-box;
	border-radius: 28rpx;
	padding: 28rpx 0;
	background: #FDAF32;
	box-shadow: 0 4rpx 8rpx -4rpx #FF690033, 0 8rpx 12rpx -2rpx #FF690033;
	display: flex;
	align-items: center;
	justify-content: center;
}

.login-button--loading {
	opacity: 0.7;
}

.login-button-text {
	width: auto;
	min-width: 64rpx;
	height: 48rpx;
	white-space: nowrap;
	font-family: Inter, -apple-system, sans-serif;
	font-size: 32rpx;
	line-height: 48rpx;
	font-weight: 500;
	color: #FFFFFF;
	text-align: center;
}
</style>
