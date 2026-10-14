<template>
	<view class="service-list">
		<template v-for="group in groups" :key="group.label">
			<text class="group-label">{{ group.label }}</text>
			<view v-for="record in group.records" :key="record.id" class="record-card">
				<view class="detail-line"><text class="field">借阅书籍：</text><text class="value strong">{{ record.bookCode }}</text></view>
				<view class="detail-line"><text class="field">书籍类型：</text><text class="value strong">{{ record.category }}</text></view>
				<view class="detail-line"><text class="field">借阅时间：</text><text class="value">{{ formatDate(record.borrowedOn) }}</text></view>
				<view class="detail-line return-line"><text class="field">归还时间：</text><text class="due">{{ formatDate(record.dueOn) }}前</text>
					<button v-if="record.status === 'borrowed'" class="return-button" @click="$emit('return', record)">归还</button>
					<button v-else class="return-button disabled" disabled>{{ statusText(record.status) }}</button>
				</view>
			</view>
		</template>
		<text v-if="!groups.length" class="empty">暂无借阅记录</text>
	</view>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { formatServiceGroupLabel } from '../../utils/memberServiceRules'
import type { BorrowRecord, BorrowStatus } from '../../utils/memberServiceMock'

const props = defineProps<{ records: BorrowRecord[], nowMs?: number }>()
defineEmits<{ return: [record: BorrowRecord] }>()
const formatDate = (source: string) => { const [y, m, d] = source.split('-').map(Number); return `${y}年${m}月${d}日` }
const statusText = (status: BorrowStatus) => status === 'returned' ? '已归还' : '待确认'
const groups = computed(() => {
	const now = new Date(props.nowMs ?? Date.now())
	const map = new Map<string, BorrowRecord[]>()
	props.records.slice().sort((a, b) => new Date(b.submittedAt).getTime() - new Date(a.submittedAt).getTime()).forEach((record) => {
		const label = formatServiceGroupLabel(record.submittedAt, now)
		map.set(label, [...(map.get(label) || []), record])
	})
	return [...map.entries()].map(([label, records]) => ({ label, records }))
})
</script>

<style scoped>
.service-list { padding: 0 34rpx 70rpx; }.group-label { display: block; margin: 28rpx 10rpx 24rpx; color: #999; font-family: Inter, sans-serif; font-size: 24rpx; font-weight: 300; line-height: 32rpx; }.record-card { min-height: 276rpx; margin-bottom: 38rpx; padding: 32rpx 34rpx 24rpx; box-sizing: border-box; border: 2rpx solid #eceff2; border-radius: 20rpx; background: #fff; box-shadow: 0 7rpx 18rpx rgba(31,35,41,.04); }.detail-line { min-height: 50rpx; display: flex; align-items: center; font-size: 29rpx; line-height: 40rpx; }.field { color: #888; flex-shrink: 0; }.value { color: #333; }.strong { font-weight: 700; }.return-line { position: relative; }.due { color: #ff6600; }.return-button { position: absolute; right: 0; bottom: 0; width: 160rpx; height: 58rpx; margin: 0; padding: 0; border-radius: 12rpx; background: #fdaf32; color: #fff; font-size: 28rpx; line-height: 58rpx; }.return-button::after { border: 0; }.return-button.disabled { background: #d9d9d9; }.empty { display: block; padding: 180rpx 0; text-align: center; color: #aaa; font-size: 28rpx; }
</style>
