<template>
	<view class="elite-page">
		<view class="top-bar">
			<view class="back-btn" @click="handleBack">
				<image class="back-icon" :src="backIconSrc" mode="aspectFit" />
			</view>
			<text class="page-title">精英分享会</text>
			<view class="top-bar-spacer" />
		</view>

		<scroll-view scroll-y class="page-scroll" :show-scrollbar="false">
			<view class="content-card">
				<text class="section-heading">我想说</text>
				<textarea
					v-model="remark"
					class="remark-input"
					:class="{ 'remark-input--disabled': isSigned }"
					:disabled="isSigned"
					maxlength="300"
					placeholder="你想听什么，或者有什么想问的，都可以在这里备注"
					placeholder-class="remark-placeholder"
				/>

				<view class="activity-info">
					<view class="info-row">
						<text class="info-label">时间：</text>
						<text class="info-value">{{ displayTime }}</text>
					</view>
					<view class="info-row">
						<text class="info-label">地点：</text>
						<text class="info-value">{{ activity.locationDesc || '地点待公布' }}</text>
					</view>
					<view class="info-row">
						<text class="info-label">介绍：</text>
						<text class="info-value info-value--description">{{ activity.description || '活动内容将在活动前公布' }}</text>
					</view>
				</view>

				<text class="guest-title">邀请嘉宾：</text>
				<view v-if="guests.length" class="guest-list">
					<view v-for="(guest, index) in guests" :key="guest.guestId" class="guest-item">
						<view class="guest-heading">
							<view class="guest-index">{{ String(index + 1).padStart(2, '0') }}</view>
							<view class="guest-name-wrap">
								<text class="guest-name">{{ guest.name }}</text>
								<view class="guest-name-line" />
							</view>
							<text class="guest-flower">✿</text>
						</view>

						<view class="guest-detail">
							<text class="guest-detail-line">来自何方：　{{ guest.company }}｜{{ guest.position }}</text>
							<text class="guest-detail-line">背调：{{ guest.background }}</text>
							<text class="guest-detail-line guest-detail-line--topic">投喂何等干货：{{ guest.topic }}</text>
						</view>
					</view>
				</view>
				<view v-else class="guest-empty">嘉宾信息将在活动前公布</view>
			</view>
			<view class="scroll-spacer" />
		</scroll-view>

		<view class="footer-bar">
			<button
				class="signup-btn"
				:class="{ 'signup-btn--disabled': !signupState.enabled }"
				:disabled="!signupState.enabled"
				@click="handleSignup"
			>
				<text class="signup-btn-text">{{ signupState.label }}</text>
			</button>
		</view>

		<view v-if="showSuccessModal" class="modal-mask" @click="closeSuccessModal" />
		<view v-if="showSuccessModal" class="success-modal" @click.stop>
			<text class="success-title">报名成功！</text>
			<text class="success-subtitle">请务必准时参与，精彩内容等你来解锁 ~</text>
		</view>
	</view>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import {
	formatActivityTimeRange,
	getMockEliteShareGuests,
	getTalkSignupState,
	type LocalActivitySignup,
	type MockActivity
} from '@/utils/mockActivity'
import { getActivityByKind, getMyActivitySignup, signupActivity } from '@/api/activity'

const activity = reactive<MockActivity>({ activityId: 0, activityType: 'ELITE_SHARE', title: '', scenePrefix: '', brandName: 'Quanta', sceneSuffix: '精英分享会', description: '', signupStart: '', signupEnd: '', activityStart: '', activityEnd: '', locationDesc: '', capacity: 0, signupCount: 0, status: 'DRAFT' })
const guests = ref(getMockEliteShareGuests())
const signup = ref<LocalActivitySignup | null>(null)
const remark = ref('')
const showSuccessModal = ref(false)

const signupState = computed(() => getTalkSignupState(activity, signup.value))
const isSigned = computed(() => signupState.value.key === 'signed')
const displayTime = computed(() =>
	formatActivityTimeRange(activity.activityStart, activity.activityEnd) || '时间待公布'
)

const backIconSrc = `data:image/svg+xml;utf8,${encodeURIComponent(
	'<svg width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg"><path d="M15 18L9 12L15 6" stroke="#101828" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/></svg>'
)}`

const handleBack = () => uni.navigateBack({ delta: 1 })

const handleSignup = async () => {
	const currentState = getTalkSignupState(activity, signup.value)
	if (!currentState.enabled) return

	try {
		signup.value = await signupActivity(activity.activityId, remark.value.trim())
		showSuccessModal.value = true
	} catch (error) {
		console.error('保存精英分享会报名信息失败', error)
		uni.showToast({ title: error?.message || '报名失败，请稍后重试', icon: 'none' })
	}
}

const closeSuccessModal = () => {
	showSuccessModal.value = false
}

onMounted(async () => {
	try {
		Object.assign(activity, await getActivityByKind('SHARING'))
		signup.value = await getMyActivitySignup(activity.activityId)
		if (typeof signup.value?.remark === 'string') remark.value = signup.value.remark
	} catch (error) {
		uni.showToast({ title: error?.message || '精英分享会加载失败', icon: 'none' })
	}
})
</script>

<style scoped>
.elite-page {
	width: 100%;
	height: 100vh;
	background: #f7f7f7;
	box-sizing: border-box;
	overflow: hidden;
	display: flex;
	flex-direction: column;
}

.top-bar {
	width: 100%;
	height: 176rpx;
	padding: 88rpx 32rpx 40rpx;
	background: #ffffff;
	display: flex;
	align-items: center;
	justify-content: space-between;
	box-sizing: border-box;
	flex-shrink: 0;
}

.back-btn,
.top-bar-spacer {
	width: 48rpx;
	height: 48rpx;
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
	font-size: 36rpx;
	line-height: 48rpx;
	font-weight: 500;
	color: #000000;
}

.page-scroll {
	width: 100%;
	flex: 1;
	height: 0;
}

.content-card {
	width: calc(100% - 88rpx);
	margin: 40rpx auto 0;
	padding: 42rpx 36rpx 80rpx;
	background: #ffffff;
	box-sizing: border-box;
	box-shadow: 0 0 24rpx rgba(0, 0, 0, 0.08);
}

.scroll-spacer {
	height: calc(180rpx + env(safe-area-inset-bottom));
}

.section-heading,
.guest-title {
	display: block;
	font-size: 42rpx;
	line-height: 56rpx;
	font-weight: 700;
	color: #000000;
}

.remark-input {
	width: calc(100% - 16rpx);
	height: 264rpx;
	margin: 18rpx auto 0;
	padding: 26rpx;
	border: 2rpx solid #e5e7eb;
	border-radius: 20rpx;
	background: #ffffff;
	font-size: 26rpx;
	line-height: 40rpx;
	color: #101828;
	box-sizing: border-box;
}

.remark-input--disabled {
	background: #f4f4f4;
	color: #667085;
}

.remark-placeholder {
	color: #d0d5dd;
}

.activity-info {
	margin: 38rpx 10rpx 0;
	display: flex;
	flex-direction: column;
	gap: 30rpx;
}

.info-row {
	display: flex;
	align-items: flex-start;
}

.info-label {
	width: 168rpx;
	flex-shrink: 0;
	font-size: 36rpx;
	line-height: 48rpx;
	font-weight: 700;
	color: #000000;
}

.info-value {
	flex: 1;
	font-size: 30rpx;
	line-height: 42rpx;
	color: #000000;
}

.info-value--description {
	color: #344054;
}

.guest-title {
	margin: 42rpx 10rpx 0;
	font-size: 36rpx;
}

.guest-list {
	margin-top: 52rpx;
	display: flex;
	flex-direction: column;
	gap: 56rpx;
}

.guest-heading {
	display: flex;
	align-items: center;
	gap: 20rpx;
}

.guest-index {
	width: 62rpx;
	height: 62rpx;
	background: #ffd45d;
	color: #ffffff;
	font-size: 30rpx;
	font-weight: 700;
	display: flex;
	align-items: center;
	justify-content: center;
}

.guest-name-wrap {
	position: relative;
	padding-bottom: 12rpx;
}

.guest-name {
	font-size: 32rpx;
	line-height: 44rpx;
	font-weight: 700;
	color: #000000;
}

.guest-name-line {
	position: absolute;
	left: 0;
	bottom: 0;
	width: 164rpx;
	height: 10rpx;
	background: #9ad2dd;
}

.guest-flower {
	font-size: 46rpx;
	line-height: 1;
	color: #ffd45d;
}

.guest-detail {
	margin-top: 34rpx;
	padding: 30rpx 28rpx;
	background: #fff3d5;
	display: flex;
	flex-direction: column;
	gap: 18rpx;
}

.guest-detail-line {
	font-size: 28rpx;
	line-height: 42rpx;
	color: #000000;
}

.guest-detail-line--topic {
	white-space: normal;
}

.guest-empty {
	margin-top: 36rpx;
	padding: 54rpx 24rpx;
	background: #f8f9fa;
	border-radius: 16rpx;
	font-size: 28rpx;
	color: #98a2b3;
	text-align: center;
}

.footer-bar {
	position: fixed;
	left: 0;
	right: 0;
	bottom: 0;
	z-index: 20;
	padding: 20rpx 0 calc(28rpx + env(safe-area-inset-bottom));
	background: linear-gradient(to top, #ffffff 68%, rgba(255, 255, 255, 0));
}

.signup-btn {
	width: 438rpx;
	height: 84rpx;
	margin: 0 auto;
	padding: 0;
	border: none;
	border-radius: 8rpx;
	background: #000000;
	display: flex;
	align-items: center;
	justify-content: center;
}

.signup-btn::after {
	border: none;
}

.signup-btn--disabled {
	background: #d0d5dd;
}

.signup-btn[disabled] {
	opacity: 1;
}

.signup-btn-text {
	font-size: 40rpx;
	line-height: 44rpx;
	font-weight: 400;
	letter-spacing: 11rpx;
	color: #ffffff;
}

.modal-mask {
	position: fixed;
	inset: 0;
	background: rgba(0, 0, 0, 0.45);
	z-index: 1000;
}

.success-modal {
	position: fixed;
	top: 50%;
	left: 50%;
	transform: translate(-50%, -50%);
	width: 596rpx;
	min-height: 292rpx;
	padding: 44rpx;
	background: #ffffff;
	border: 2rpx solid #d9d9d9;
	border-radius: 32rpx;
	z-index: 1001;
	display: flex;
	flex-direction: column;
	align-items: center;
	justify-content: center;
	gap: 20rpx;
	box-sizing: border-box;
}

.success-title {
	font-size: 40rpx;
	line-height: 48rpx;
	color: #ff6500;
}

.success-subtitle {
	font-size: 40rpx;
	line-height: 48rpx;
	color: #000000;
	text-align: center;
}
</style>
