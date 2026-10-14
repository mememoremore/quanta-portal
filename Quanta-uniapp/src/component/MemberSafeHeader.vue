<template>
	<view class="member-safe-header" :class="`member-safe-header--${mode}`" :style="headerStyle">
		<view class="member-safe-header__row" :style="rowStyle">
			<template v-if="mode === 'brand'">
				<text class="member-safe-header__brand">{{ title }}</text>
				<view class="member-safe-header__actions" :style="actionsStyle">
					<slot name="actions" />
				</view>
			</template>
			<template v-else>
				<view v-if="back" class="member-safe-header__back" @click="handleBack">‹</view>
				<text class="member-safe-header__title" :class="`member-safe-header__title--${titleSize}`" :style="titleSafeStyle">{{ title }}</text>
			</template>
		</view>
	</view>
</template>

<script setup lang="js">
import { getMemberNavigationMetrics } from '../utils/memberNavigation'

const props = defineProps({
	mode: { type: String, default: 'detail' },
	title: { type: String, required: true },
	back: { type: Boolean, default: false },
	titleSize: { type: String, default: 'normal' },
})

const emit = defineEmits(['back'])
const metrics = getMemberNavigationMetrics()
const headerStyle = {
	height: `${metrics.totalHeight}px`,
	paddingTop: `${metrics.statusBarHeight}px`,
}
const rowStyle = { height: `${metrics.navigationHeight}px` }
const actionsStyle = { right: `${metrics.capsuleInsetRight}px` }
const titleSafeStyle = {
	left: `${metrics.capsuleInsetRight}px`,
	right: `${metrics.capsuleInsetRight}px`,
}

const handleBack = () => emit('back')
</script>

<style scoped>
.member-safe-header {
	position: relative;
	width: 100vw;
	margin-left: calc(50% - 50vw);
	box-sizing: border-box;
	flex-shrink: 0;
}

.member-safe-header__row {
	position: relative;
	width: 100%;
	display: flex;
	align-items: center;
	box-sizing: border-box;
}

.member-safe-header__brand {
	position: absolute;
	left: 46rpx;
	top: 50%;
	transform: translateY(-50%);
	font-family: Inter, -apple-system, sans-serif;
	font-size: 48rpx;
	line-height: 64rpx;
	font-weight: 700;
	letter-spacing: -1.2rpx;
	color: #333;
}

.member-safe-header__actions {
	position: absolute;
	top: 50%;
	transform: translateY(-50%);
	display: flex;
	align-items: center;
	gap: 28rpx;
}

.member-safe-header__back {
	position: absolute;
	left: 32rpx;
	top: 50%;
	transform: translateY(-50%);
	width: 64rpx;
	height: 64rpx;
	display: flex;
	align-items: center;
	justify-content: center;
	font-size: 68rpx;
	font-weight: 300;
	line-height: 56rpx;
	color: #111;
}

.member-safe-header__title {
	position: absolute;
	top: 50%;
	transform: translateY(-50%);
	text-align: center;
	white-space: nowrap;
	overflow: hidden;
	text-overflow: ellipsis;
	color: #222;
}

.member-safe-header__title--normal {
	font-size: 32rpx;
	font-weight: 500;
}

.member-safe-header__title--large {
	font-size: 36rpx;
	font-weight: 600;
}
</style>
