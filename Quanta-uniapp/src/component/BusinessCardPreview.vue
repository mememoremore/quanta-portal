<template>
	<view class="preview-mask" @click="$emit('close')">
		<view class="preview-card" @click.stop>
			<view class="preview-header">
				<view class="preview-avatar"><text>{{ card.name.slice(0, 1) }}</text></view>
				<view class="preview-identity"><text class="preview-name">{{ card.name }}</text><text class="preview-meta">{{ card.role }} · Quanta · {{ card.batch }}</text></view>
				<image class="preview-brand" src="/static/picture/business-card-brand.png" mode="aspectFit" />
			</view>
			<view class="preview-divider" />
			<text class="preview-bio">{{ card.bio || '这位塔员还没有填写个人简介。' }}</text>
		</view>
	</view>
</template>

<script setup lang="ts">
import type { MemberBusinessCard } from '../utils/memberMock'
defineProps<{ card: MemberBusinessCard }>()
defineEmits<{ close: [] }>()
</script>

<style scoped>
.preview-mask { position: fixed; inset: 0; z-index: 80; padding: 40rpx; box-sizing: border-box; background: rgba(0,0,0,.56); display: flex; align-items: center; justify-content: center; }
.preview-card { width: 660rpx; box-sizing: border-box; padding: 52rpx 56rpx 62rpx; border-radius: 52rpx; background: #171717; color: #fff; }
.preview-header { display: flex; align-items: center; position: relative; }.preview-avatar { width: 112rpx; height: 112rpx; border-radius: 50%; background: #f29a20; display: flex; align-items: center; justify-content: center; font-size: 58rpx; font-weight: 700; flex-shrink: 0; }.preview-identity { margin-left: 30rpx; display: flex; flex-direction: column; }.preview-name { font-size: 42rpx; line-height: 56rpx; font-weight: 700; }.preview-meta { margin-top: 2rpx; font-family: Georgia, serif; font-size: 26rpx; }.preview-brand { position: absolute; right: 0; top: 2rpx; width: 50rpx; height: 38rpx; }.preview-divider { height: 1px; margin: 36rpx 0; background: rgba(255,255,255,.22); }.preview-bio { display: block; font-size: 28rpx; line-height: 1.72; letter-spacing: 2rpx; color: #f6f6f6; white-space: pre-wrap; }
</style>
