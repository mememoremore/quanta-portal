<template>
	<view class="freshman-home">
		<view class="top-title">Quanta</view>
		<view class="menu-icon-wrap" @click="handleToggleMenu">
			<image class="menu-icon" :src="menuIconSrc" mode="aspectFit" />
		</view>

		<!--下拉菜单-->
		<view class="dropdown" v-if="menuVisible" @click.stop>
			<view class="dropdown-item" @click="handleChangePassword">
				<text class="dropdown-item-text">修改密码</text>
			</view>
			<view class="dropdown-item" @click="handleLogout">
				<text class="dropdown-item-text">退出登录</text>
			</view>
		</view>

		<!--修改密码弹窗-->
		<view v-if="showModal" class="modal-mask" @click="closeModal"></view>
		<view v-if="showModal" class="modal-body" @click.stop>
			<view class="modal-content">
				<view class="modal-title">修改密码</view>
				<view class="form-item">
					<text class="textstyle">原密码       :</text>
					<input type="password" v-model="oldPassword" :disabled="loading" />
				</view>
				<view class="form-item">
					<text class="textstyle">新密码       :</text>
					<input type="password" v-model="newPassword" :disabled="loading" />
				</view>
				<view class="form-item">
					<text class="textstyle">确认新密码      :</text>
					<input type="password" v-model="confirmPassword" :disabled="loading" />
				</view>
				<view class="button-group">
					<button @click="closeModal" class="cancel-pw-button" :disabled="loading">
						<text class="cancel-pw-button-text">取消</text>
					</button>
					<button @click="submitChangePassword" class="submit-button" :disabled="loading">
						<text class="submit-button-text">{{ loading ? '提交中...' : '确认' }}</text>
					</button>
				</view>
			</view>
		</view>

		<!--退出登录弹窗-->
		<view v-if="showModal2" class="modal-mask" @click="closeModal"></view>
		<view v-if="showModal2" class="modal-body modal-body-logout" @click.stop>
			<view class="modal-content">
				<view class="modal-title">确认退出登录</view>
				<view class="button-group">
					<button @click="submitLogout" class="confirm-button">
						<text class="confirm-button-text">确认</text>
					</button>
					<button @click="closeModal" class="cancel-button">
						<text class="cancel-button-text">取消</text>
					</button>
				</view>
			</view>
		</view>

		<!--页面内容-->
		<view class="content">
			<view class="logo-wrap">
				<view class="logo-stage">
					<view class="logo-halo" />
					<view class="logo-circle" :class="{ 'logo-circle--flipped': logoFlipped }" @click="toggleLogo">
						<view class="logo-face logo-face--front">
							<image class="logo-image" :src="brandMarkSrc" mode="aspectFit"/>
						</view>
						<view class="logo-face logo-face--back">
							<text class="slogan-label">QUANTA LAB</text>
							<view class="slogan-rule" />
							<text class="slogan-prefix">nothing but</text>
							<view class="slogan-word"><text>profes</text><text class="slogan-accent">S</text><text>i</text><text class="slogan-accent">O</text><text>nal</text></view>
						</view>
					</view>
				</view>
			</view>

			<view class="intro-title">
				<text class="intro-black">自</text><text class="intro-accent">我</text><text class="intro-black">介绍</text>
			</view>

			<view class="intro-card">
				<text class="intro-text">Quanta（量子）信息技术服务中心是以半企业化模式运营的一个专业的IT技术组织，致力于大型的商业或非商业项目开发、技术攻关与自主产品研发工作。以育人文化为宗旨，Quanta秉持着“Nothing but professional.”的原则，培育出一届又一届优秀的IT人才。在Quanta，我们推崇简单、务实的工作方式，在轻松、快乐的工作环境中积累和分享。你不只是在Quanta工作，是和一群志趣相投的人一起生活！</text>
			</view>
		</view>

		<freshman-tab active="home" />
	</view>
</template>

<script setup lang="js">
import { ref } from 'vue'
import FreshmanTab from '../../component/freshman_Tab.vue'
import { submitChangePasswordApi } from '../../api/user'
import { IS_QUANTA_MEMBER_KEY, ROLE_KEY, TOKEN_KEY } from '../../utils/storage'

const menuIconSrc = '/static/picture/menu.png'
const brandMarkSrc = '/static/picture/quanta.png'
const logoFlipped = ref(false)
const toggleLogo = () => {
	logoFlipped.value = !logoFlipped.value
}

//打开侧边菜单逻辑
const menuVisible = ref(false)
const handleToggleMenu = () => {
	menuVisible.value = !menuVisible.value
}


//修改密码部分
const showModal = ref(false)  //修改密码弹窗显示控制
const handleChangePassword = () => {
	menuVisible.value = false  //关闭菜单
	showModal.value = true    //显示修改密码弹窗
}

const oldPassword = ref('')
const newPassword = ref('')
const confirmPassword = ref('')

const resetPasswordForm = () => {
	oldPassword.value = ''
	newPassword.value = ''
	confirmPassword.value = ''
}

//关闭弹窗
const closeModal = () => {
	showModal.value = false
	showModal2.value = false
	resetPasswordForm()
}

const loading = ref(false)  //提交状态控制

const validateChangePassword = () => {
	if (!oldPassword.value.trim()) {
		uni.showToast({
			title: '请输入原密码',
			icon: 'none',
		})
		return false
	}

	if (!newPassword.value.trim()) {
		uni.showToast({
			title: '请输入新密码',
			icon: 'none',
		})
		return false
	}

	if (!confirmPassword.value.trim()) {
		uni.showToast({
			title: '请确认新密码',
			icon: 'none',
		})
		return false
	}

	if (newPassword.value.trim() !== confirmPassword.value.trim()) {
		uni.showToast({
			title: '两次输入的密码不一致',
			icon: 'none',
		})
		return false
	}

	oldPassword.value = oldPassword.value.trim()
	newPassword.value = newPassword.value.trim()
	confirmPassword.value = confirmPassword.value.trim()
	return true
}

const submitChangePassword = async () => {
	if (!validateChangePassword()) {
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
		const res = await submitChangePasswordApi({
			oldPassword: oldPassword.value,
			newPassword: newPassword.value,
		})

		// 拦截器已保证 code === 200 才会到这里
		uni.showToast({
			title: res.msg || '密码修改成功',
			icon: 'success',
		})
		closeModal()
	} catch (error) {
		uni.showToast({
			title: error.message || '密码修改失败',
			icon: 'none',
		})
	} finally {
		loading.value = false
	}
}


//退出登录逻辑
const showModal2 = ref(false)  //退出登录确认弹窗显示控制
const handleLogout = () => {
	menuVisible.value = false  //关闭菜单
	showModal2.value = true  //显示退出登录确认弹窗
}

const clearLoginStorage = () => {
	uni.removeStorageSync(TOKEN_KEY)
	uni.removeStorageSync(ROLE_KEY)
	uni.removeStorageSync(IS_QUANTA_MEMBER_KEY)
}

const submitLogout = () => {
	clearLoginStorage()
	closeModal()
	uni.reLaunch({
		url: '/pages/login/select-role',
	})
}


</script>

<style scoped>
.freshman-home {
	width: 100%;
	min-height: 100vh;
	background: #ffffff;
	position: relative;
	overflow: visible;
	padding-bottom: calc(320rpx + env(safe-area-inset-bottom));
	box-sizing: border-box;
}

.top-title {
	position: absolute;
	top: 104rpx;
	left: 0;
	right: 0;
	text-align: center;
	font-family: Inter, -apple-system, sans-serif;
	font-size: 32rpx;
	line-height: 32rpx;
	font-weight: 400;
	color: #000000;
}

.menu-icon-wrap {
	position: absolute;
	top: 164rpx;
	right: 48rpx;
	width: 64rpx;
	height: 102rpx;
	z-index: 10;
}

.menu-icon {
	width: 64rpx;
	height: 102rpx;
}

.content {
	width: 100%;
	display: flex;
	flex-direction: column;
	align-items: center;
}

.logo-wrap {
	margin-top: 200rpx;
	width: 100%;
	display: flex;
	justify-content: center;
	}

.logo-stage {
	width: 366rpx;
	height: 366rpx;
	border-radius: 50%;
	position: relative;
	perspective: 1200rpx;
}

.logo-halo {
	position: absolute;
	inset: -128rpx;
	border-radius: 50%;
	background: radial-gradient(circle, rgba(253, 175, 50, 0.3) 0%, rgba(255, 132, 44, 0.16) 38%, rgba(255, 102, 0, 0.06) 58%, rgba(255, 102, 0, 0) 74%);
	pointer-events: none;
}

.logo-circle {
	width: 100%;
	height: 100%;
	border-radius: 220rpx;
	position: relative;
	z-index: 1;
	transform-style: preserve-3d;
	transition: transform 0.65s cubic-bezier(0.22, 0.61, 0.36, 1);
}

.logo-circle--flipped {
	transform: rotateY(180deg);
}

.logo-face {
	position: absolute;
	inset: 0;
	border-radius: 50%;
	overflow: hidden;
	backface-visibility: hidden;
	-webkit-backface-visibility: hidden;
	display: flex;
	flex-direction: column;
	align-items: center;
	justify-content: center;
	box-sizing: border-box;
}

.logo-face--front {
	background: #ffffff;
}

.logo-face--back {
	transform: rotateY(180deg);
	padding: 54rpx 34rpx 48rpx;
	background: radial-gradient(circle at 28% 20%, #343434 0, #1e1e1e 44%, #101010 100%);
	border: 2rpx solid rgba(255, 102, 0, 0.72);
	color: #ffffff;
	box-shadow: inset 0 0 42rpx rgba(255, 102, 0, 0.18);
}

.logo-image {
	width: 100%;
	height: 100%;
	border-radius: 50%;
}

.slogan-label {
	font-family: Inter, -apple-system, sans-serif;
	font-size: 21rpx;
	line-height: 28rpx;
	font-weight: 600;
	letter-spacing: 6rpx;
	color: rgba(255, 255, 255, 0.58);
}

.slogan-rule {
	width: 72rpx;
	height: 4rpx;
	margin-top: 20rpx;
	border-radius: 99rpx;
	background: #ff6600;
	box-shadow: 0 0 18rpx rgba(255, 102, 0, 0.72);
}

.slogan-prefix {
	margin-top: 28rpx;
	font-family: Dubai, Inter, -apple-system, sans-serif;
	font-size: 38rpx;
	line-height: 44rpx;
	font-weight: 400;
	letter-spacing: 1rpx;
	color: rgba(255, 255, 255, 0.82);
}

.slogan-word {
	display: flex;
	align-items: baseline;
	font-family: Dubai, Inter, -apple-system, sans-serif;
	font-size: 48rpx;
	line-height: 56rpx;
	font-weight: 700;
	letter-spacing: 1rpx;
	white-space: nowrap;
	color: #ffffff;
}

.slogan-accent {
	color: #ff6600;
	text-shadow: 0 0 18rpx rgba(255, 102, 0, 0.58);
}

/*下拉菜单样式*/ 
.dropdown {
  position: absolute;
  top: 274rpx;
  right: 40rpx;
  width: 200rpx;
  background: #fff;
  z-index: 999;
  border-radius: 12rpx;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.12);
  overflow: hidden;
}

/* 菜单项 */
.dropdown-item {
  width: 100%;
  height: 80rpx;
  box-sizing: border-box;
  display: flex;
  align-items: center;
  justify-content: center;
  border-bottom: 1rpx solid #F0F0F0;
}

.dropdown-item:last-child {
  border-bottom: none;
}

.dropdown-item:active {
  background: #F5F5F5;
}

/* 文本样式 */
.dropdown-item-text {
  font-family: Inter, -apple-system, sans-serif;
  font-weight: 400;
  font-style: normal;
  font-size: 28rpx;
  line-height: 40rpx;
  color: #333333;
  letter-spacing: 0;
}

/**	quanta介绍 */
.intro-title {
	margin-top: 48rpx;
	font-family: "HP Simplified", Inter, -apple-system, sans-serif;
	font-size: 48rpx;
	line-height: 48rpx;
	font-weight: 700;
	text-align: center;
	text-shadow: 6px 7px 8.2px #00000040;
}

.intro-black {
	color: #000000;
}

.intro-accent {
	color: #FF6600;
}

.intro-card {
	margin-top: 36rpx;
	width: calc(100% - 92rpx);
	margin-left: 46rpx;
	margin-right: 46rpx;
	border-radius: 20rpx;
	background: #ffffff;
	box-shadow: 0px 1px 10.5px 0px #00000040;
	padding: 44rpx 44rpx;
	box-sizing: border-box;
}

.intro-text {
	display: block;
	width: 100%;
	font-family: Dubai, Inter, -apple-system, sans-serif;
	font-size: 40rpx;
	line-height: 60rpx;
	font-weight: 400;
	color: #000000;
	text-align: left;
	white-space: pre-wrap;
}

/* 弹窗遮罩 */
.modal-mask {
	position: fixed;
	top: 0;
	left: 0;
	right: 0;
	bottom: 0;
	background: rgba(0, 0, 0, 0.45);
	z-index: 1000;
}

/* 弹窗主体 */
.modal-body {
	position: fixed;
	top: 50%;
	left: 50%;
	transform: translate(-50%, -50%);
	width: calc(100% - 120rpx);
	max-width: 620rpx;
	z-index: 1001;
}

.modal-content {
	background: #ffffff;
	border-radius: 24rpx;
	padding: 48rpx 40rpx;
	box-sizing: border-box;
	box-shadow: 0 8rpx 32rpx rgba(0, 0, 0, 0.15);
}

.modal-title {
	font-family: Inter, -apple-system, sans-serif;
	font-size: 36rpx;
	font-weight: 600;
	line-height: 48rpx;
	color: #000000;
	text-align: center;
	margin-bottom: 40rpx;
}

.modal-body-logout .modal-content {
	padding: 56rpx 40rpx 48rpx;
}

.modal-body-logout .modal-title {
	margin-bottom: 48rpx;
}

.form-item {
	display: flex;
	align-items: center;
	margin-bottom: 28rpx;
}

.textstyle {
	flex-shrink: 0;
	width: 180rpx;
	font-size: 28rpx;
	color: #333333;
}

.form-item input {
	flex: 1;
	height: 72rpx;
	padding: 0 20rpx;
	border: 1rpx solid #E0E0E0;
	border-radius: 12rpx;
	font-size: 28rpx;
	box-sizing: border-box;
}

.button-group {
	display: flex;
	justify-content: space-between;
	gap: 24rpx;
	margin-top: 16rpx;
}

.button-group button {
	flex: 1;
	height: 80rpx;
	line-height: 80rpx;
	border-radius: 16rpx;
	border: none;
	padding: 0;
	margin: 0;
	font-size: 30rpx;
}

.button-group button::after {
	border: none;
}

.cancel-button {
	background: #FF6600;
}

.cancel-button-text {
	color: #ffffff;
	font-size: 30rpx;
}

.cancel-pw-button {
	background: #B0B0B0;
}

.cancel-pw-button-text {
	color: #ffffff;
	font-size: 30rpx;
}

.submit-button {
	background: #FF6600;
}

.submit-button-text {
	color: #ffffff;
	font-size: 30rpx;
}

.confirm-button {
	background: #B0B0B0;
}

.confirm-button-text {
	color: #ffffff;
	font-size: 30rpx;
}
</style>
