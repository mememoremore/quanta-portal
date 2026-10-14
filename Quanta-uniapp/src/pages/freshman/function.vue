<template>
	<view class="freshman-function">
		<view class="top-title">Quanta</view>

		<scroll-view scroll-y class="scroll-area" :show-scrollbar="false">
			<view class="circle-list">
				<view class="func-circle circle-presentation" @click="handlePresentation">
					<text class="circle-text text-presentation">宣讲会</text>
				</view>

				<view class="func-circle circle-join" @click="handleJoinUs">
					<text class="circle-text text-join">Join us</text>
				</view>

				<view class="func-circle circle-elite" @click="handleEliteShare">
					<view class="circle-text-stack">
						<text class="circle-text text-elite">精英</text>
						<text class="circle-text text-elite">分享会</text>
					</view>
				</view>
			</view>
		</scroll-view>

		<freshman-tab active="menu" />
	</view>
</template>

<script setup lang="js">
import FreshmanTab from '../../component/freshman_Tab.vue'

const handlePresentation = () => {
	uni.navigateTo({
		url: '/pages/freshman/talk/talk',
	})
}

const handleJoinUs = () => {
	uni.navigateTo({
		url: '/pages/freshman/join-us/join-us',
	})
}

const handleEliteShare = () => {
	uni.navigateTo({
		url: '/pages/freshman/elite-share/elite-share',
	})
}
</script>

<style scoped>
.freshman-function {
	width: 100%;
	min-height: 100vh;
	background: #ffffff;
	position: relative;
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
	z-index: 2;
}

.scroll-area {
	width: 100%;
	height: 100vh;
	box-sizing: border-box;
	background: #ffffff;
	padding-top: 180rpx;
	padding-bottom: 0;
}

.circle-list {
	display: flex;
	flex-direction: column;
	align-items: center;
	padding: 50rpx 0 calc(250rpx + env(safe-area-inset-bottom));
	box-sizing: border-box;
}

.func-circle {
	width: 408rpx;
	height: 408rpx;
	border-radius: 50%;
	display: flex;
	align-items: center;
	justify-content: center;
	box-shadow: 2rpx 2rpx 108rpx 0 rgba(0, 0, 0, 0.25);
	box-sizing: border-box;
}

.func-circle + .func-circle {
	margin-top: 108rpx;
}

.circle-presentation {
	background: #ffffff;
}

.circle-join {
	background: #1e1e1e;
}

.circle-elite {
	background: #ff6600;
}

.circle-text {
	font-weight: 700;
	font-size: 80rpx;
	line-height: 1;
	letter-spacing: 0;
}

.text-presentation {
	font-family: 'HP Simplified', Inter, -apple-system, sans-serif;
	color: #000000;
}

.text-join {
	font-family: 'HP Simplified', Inter, -apple-system, sans-serif;
	color: #ff6600;
}

.circle-text-stack {
	display: flex;
	flex-direction: column;
	align-items: center;
	justify-content: center;
	gap: 24rpx;
}

.text-elite {
	font-family: Inter, -apple-system, sans-serif;
	color: #ffffff;
}
</style>
