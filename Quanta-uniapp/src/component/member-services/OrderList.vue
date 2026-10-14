<template>
	<view class="service-list">
		<template v-for="group in groups" :key="group.label">
			<text class="group-label">{{ group.label }}</text>
			<view v-for="record in group.records" :key="record.id" class="record-card">
				<view class="detail-line"><text class="field">塔服颜色：</text><text class="value strong">{{ record.color }}</text></view>
				<view class="detail-line"><text class="field">塔服尺寸：</text><text class="value">{{ record.size }}</text></view>
				<view class="payment-row"><text class="field">付款截图：</text><image class="receipt" :src="record.paymentImage" mode="aspectFit" /></view>
				<view class="status-row"><text class="field">付款状态：</text><text class="status" :class="record.status">{{ record.status === 'ordered' ? '已订购' : '待管理员确认' }}</text><image v-if="record.status === 'pending-review'" class="notice-icon" src="/static/icon/member/service-notice.svg" mode="aspectFit" @click="$emit('explain', record)" /></view>
			</view>
		</template>
		<text v-if="!groups.length" class="empty">暂无订购记录</text>
	</view>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { formatServiceGroupLabel } from '../../utils/memberServiceRules'
import type { ShirtOrderRecord } from '../../utils/memberServiceMock'
const props = defineProps<{ records: ShirtOrderRecord[], nowMs?: number }>()
defineEmits<{ explain: [record: ShirtOrderRecord] }>()
const groups = computed(() => {
	const now = new Date(props.nowMs ?? Date.now())
	const map = new Map<string, ShirtOrderRecord[]>()
	props.records.slice().sort((a, b) => new Date(b.submittedAt).getTime() - new Date(a.submittedAt).getTime()).forEach((record) => {
		const label = formatServiceGroupLabel(record.submittedAt, now)
		map.set(label, [...(map.get(label) || []), record])
	})
	return [...map.entries()].map(([label, records]) => ({ label, records }))
})
</script>

<style scoped>
.service-list { padding: 0 34rpx 70rpx; }.group-label { display: block; margin: 28rpx 10rpx 24rpx; color: #999; font-family: Inter, sans-serif; font-size: 24rpx; font-weight: 300; line-height: 32rpx; }.record-card { min-height: 570rpx; margin-bottom: 38rpx; padding: 34rpx; box-sizing: border-box; border: 2rpx solid #eceff2; border-radius: 20rpx; background: #fff; box-shadow: 0 7rpx 18rpx rgba(31,35,41,.04); }.detail-line,.payment-row,.status-row { display: flex; align-items: flex-start; font-size: 29rpx; line-height: 42rpx; }.detail-line { margin-bottom: 42rpx; }.field { color: #888; flex-shrink: 0; }.value { color: #333; }.strong { font-weight: 700; }.receipt { width: 230rpx; height: 210rpx; margin-left: 10rpx; }.status-row { align-items: center; margin-top: 30rpx; }.status { padding: 7rpx 18rpx; border-radius: 9rpx; color: #fff; font-size: 23rpx; line-height: 32rpx; }.status.pending-review { background: #ff6600; }.status.ordered { background: #04df67; }.notice-icon { width: 28rpx; height: 28rpx; margin-left: 14rpx; }.empty { display: block; padding: 180rpx 0; text-align: center; color: #aaa; font-size: 28rpx; }
</style>
