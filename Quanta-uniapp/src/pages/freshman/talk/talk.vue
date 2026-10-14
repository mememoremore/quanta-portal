<template>
	<view class="talk-page">
		<view class="top-bar">
			<view class="back-btn" @click="handleBack">
				<image class="back-icon" :src="backIconSrc" mode="aspectFit" />
			</view>
			<text class="page-title">宣讲会</text>
			<view class="top-bar-spacer" />
		</view>

		<view class="talk-content">
			<view class="hero-wrap">
				<text class="hero-welcome">欢迎莅临</text>
				<view class="hero-title">
					<text class="hero-title-black">{{ talkInfo.scenePrefix }}</text>
					<text class="hero-title-accent">{{ talkInfo.brandName }}</text>
					<text class="hero-title-black">{{ talkInfo.sceneSuffix }}</text>
				</view>
			</view>

			<view class="talk-card">
				<view class="info-row">
					<text class="info-label">时间：</text>
					<text class="info-value">{{ displayTime }}</text>
				</view>

				<view class="info-row">
					<text class="info-label">地点：</text>
					<text class="info-value">{{ talkInfo.locationDesc }}</text>
				</view>

				<view class="info-row info-row-remark">
					<text class="info-label">备注：</text>
					<text class="info-value info-value-remark">{{ talkInfo.description }}</text>
				</view>

				<button
					class="signup-btn"
					:class="{ 'signup-btn-disabled': !signupState.enabled }"
					:disabled="!signupState.enabled"
					@click="handleSignup"
				>
					<text class="signup-btn-text">{{ signupState.label }}</text>
				</button>
			</view>

			<text class="talk-tip">仅限报名{{ talkInfo.capacity }}名，先到先得！</text>
		</view>

		<!--报名成功弹窗-->
		<view v-if="showSuccessModal" class="modal-mask" @click="closeSuccessModal"></view>
		<view v-if="showSuccessModal" class="modal-body" @click.stop>
			<text class="success-title">报名成功！</text>
			<text class="success-subtitle">请务必准时参与，精彩内容等你来解锁 ~</text>
		</view>
	</view>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import {
	formatActivityTimeRange,
	getTalkSignupState,
	type LocalActivitySignup,
	type MockActivity
} from '@/utils/mockActivity'
import { getActivityByKind, getMyActivitySignup, signupActivity } from '@/api/activity'

const talkInfo = reactive<MockActivity>({ activityId: 0, activityType: 'TALK', title: '', scenePrefix: '广外', brandName: 'Quanta', sceneSuffix: '专场宣讲会', description: '', signupStart: '', signupEnd: '', activityStart: '', activityEnd: '', locationDesc: '', capacity: 0, signupCount: 0, status: 'DRAFT' })
const signup = ref<LocalActivitySignup | null>(null)
const signupState = computed(() => getTalkSignupState(talkInfo, signup.value))
const displayTime = computed(() =>
	formatActivityTimeRange(talkInfo.activityStart, talkInfo.activityEnd)
)

const backIconSrc = `data:image/svg+xml;utf8,${encodeURIComponent(
	'<svg width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg"><path d="M15 18L9 12L15 6" stroke="#101828" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/></svg>'
)}`

const handleBack = () => {
	uni.navigateBack({ delta: 1 })
}

const showSuccessModal = ref(false)

const handleSignup = async () => {
	const currentState = getTalkSignupState(talkInfo, signup.value)
	if (!currentState.enabled) return

	try {
		signup.value = await signupActivity(talkInfo.activityId)
		showSuccessModal.value = true
	} catch (error) {
		console.error('保存宣讲会报名信息失败', error)
		uni.showToast({ title: error?.message || '报名失败，请稍后重试', icon: 'none' })
	}
}

const closeSuccessModal = () => {
	showSuccessModal.value = false
}

onMounted(async () => {
	try {
		Object.assign(talkInfo, await getActivityByKind('LECTURE'))
		signup.value = await getMyActivitySignup(talkInfo.activityId)
	} catch (error) {
		uni.showToast({ title: error?.message || '宣讲会加载失败', icon: 'none' })
	}
})
</script>

<style scoped>
.talk-page {
	width: 100%;
	min-height: 100vh;
	background: #ffffff;
	box-sizing: border-box;
}

.top-bar {
	width: 100%;
	display: flex;
	align-items: center;
	justify-content: space-between;
	padding: 0 32rpx;
	box-sizing: border-box;
}

.back-btn {
	width: 48rpx;
	height: 48rpx;
	margin-top: 88rpx;
	display: flex;
	align-items: center;
	justify-content: center;
	flex-shrink: 0;
}

.back-icon {
	width: 40rpx;
	height: 40rpx;
}

.page-title {
	margin-top: 88rpx;
	font-family: Inter, -apple-system, sans-serif;
	font-size: 36rpx;
	line-height: 48rpx;
	font-weight: 500;
	color: #000000;
	text-align: center;
}

.top-bar-spacer {
	width: 48rpx;
	height: 48rpx;
	flex-shrink: 0;
}

.talk-content {
	width: 100%;
	padding: 118rpx 54rpx 80rpx;
	box-sizing: border-box;
	display: flex;
	flex-direction: column;
	align-items: center;
}

.hero-wrap {
	display: flex;
	flex-direction: column;
	align-items: center;
}

.hero-welcome {
	font-family: Inter, -apple-system, sans-serif;
	font-size: 48rpx;
	line-height: 48rpx;
	font-weight: 700;
	letter-spacing: 0;
	color: #000000;
}

.hero-title {
	margin-top: 28rpx;
	font-family: Inter, -apple-system, sans-serif;
	font-size: 48rpx;
	line-height: 48rpx;
	font-weight: 700;
	letter-spacing: 0;
	text-align: center;
}

.hero-title-black {
	color: #000000;
}

.hero-title-accent {
	color: #ff6500;
}

.talk-card {
	margin-top: 154rpx;
	width: 648rpx;
	height: 644rpx;
	margin-left: auto;
	margin-right: auto;
	display: flex;
	flex-direction: column;
	border-radius: 8rpx;
	background: #ffffff;
	box-shadow: 0 0 24rpx rgba(0, 0, 0, 0.1);
	padding: 72rpx 58rpx 0;
	box-sizing: border-box;
}

.info-row {
	display: flex;
	align-items: flex-start;
}

.info-row + .info-row {
	margin-top: 52rpx;
}

.info-row-remark {
	align-items: flex-start;
}

.info-label {
	flex-shrink: 0;
	width: 130rpx;
	font-family: Inter, -apple-system, sans-serif;
	font-size: 40rpx;
	line-height: 40rpx;
	font-weight: 700;
	letter-spacing: 0;
	color: #000000;
}

.info-value {
	flex: 1;
	font-family: Inter, -apple-system, sans-serif;
	font-size: 36rpx;
	line-height: 36rpx;
	font-weight: 400;
	letter-spacing: 0;
	color: #000000;
	word-break: break-all;
}

.info-value-remark {
	white-space: pre-wrap;
}

.signup-btn {
	margin-top: auto;
	margin-bottom: auto;
	align-self: center;
	width: 438rpx;
	height: 84rpx;
	border: none;
	border-radius: 8rpx;
	background: #000000;
	display: flex;
	align-items: center;
	justify-content: center;
	padding: 0;
}

.signup-btn::after {
	border: none;
}

.signup-btn-text {
	font-family: Inter, -apple-system, sans-serif;
	font-size: 40rpx;
	line-height: 40rpx;
	font-weight: 400;
	letter-spacing: 11rpx;
	color: #ffffff;
}

.signup-btn-disabled {
	background: #d9d9d9;
}

.signup-btn[disabled] {
	color: #ffffff;
	opacity: 1;
}

.talk-tip {
	margin-top: 86rpx;
	font-family: Inter, -apple-system, sans-serif;
	font-size: 32rpx;
	line-height: 32rpx;
	font-weight: 400;
	letter-spacing: 0;
	color: #ff6500;
	text-align: center;
}

/* 报名成功弹窗 */
.modal-mask {
	position: fixed;
	top: 0;
	left: 0;
	right: 0;
	bottom: 0;
	background: rgba(0, 0, 0, 0.45);
	z-index: 1000;
}

.modal-body {
	position: fixed;
	top: 50%;
	left: 50%;
	transform: translate(-50%, -50%);
	width: 596rpx;
	height: 292rpx;
	background: #ffffff;
	border-radius: 32rpx;
	border: 1px solid #d9d9d9;
	z-index: 1001;
	display: flex;
	flex-direction: column;
	align-items: center;
	justify-content: center;
	gap: 20rpx;
	box-sizing: border-box;
}

.success-title {
	font-family: Inter, -apple-system, sans-serif;
	font-size: 40rpx;
	font-weight: 400;
	line-height: 48rpx;
	letter-spacing: 1.6rpx;
	color: #ff6500;
}

.success-subtitle {
	width: 480rpx;
	font-family: Inter, -apple-system, sans-serif;
	font-size: 40rpx;
	font-weight: 400;
	line-height: 48rpx;
	letter-spacing: 1.6rpx;
	color: #000000;
	text-align: center;
}
</style>
