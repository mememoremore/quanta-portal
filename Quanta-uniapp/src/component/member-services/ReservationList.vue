<template>
	<view class="service-list">
		<template v-for="group in groups" :key="group.label">
			<text class="group-label">{{ group.label }}</text>
			<view v-for="item in group.items" :key="`${item.record.id}-${item.card.timeLabel}`" class="record-card">
				<view class="detail-line"><text class="field">预约位置：</text><text class="value strong">{{ item.card.workspace }}</text></view>
				<view class="detail-line"><text class="field">预约时间：</text><text class="value">{{ item.card.timeLabel }}</text></view>
				<button class="action-button" :disabled="!item.cancellable" :class="{ disabled: !item.cancellable }" @click="$emit('cancel', item.record)">取消预约</button>
			</view>
		</template>
		<text v-if="!groups.length" class="empty">暂无预约记录</text>
	</view>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { buildReservationCards, canCancelReservation, formatServiceGroupLabel, type ReservationRecord } from '../../utils/memberServiceRules'

const props = defineProps<{ records: ReservationRecord[], nowMs?: number }>()
defineEmits<{ cancel: [record: ReservationRecord] }>()

const groups = computed(() => {
	const now = new Date(props.nowMs ?? Date.now())
	const map = new Map<string, Array<{ record: ReservationRecord, card: ReturnType<typeof buildReservationCards>[number], cancellable: boolean }>>()
	props.records.slice().sort((a, b) => new Date(b.submittedAt).getTime() - new Date(a.submittedAt).getTime()).forEach((record) => {
		const label = formatServiceGroupLabel(record.submittedAt, now)
		const items = buildReservationCards(record).map((card) => ({ record, card, cancellable: canCancelReservation(record, now) }))
		map.set(label, [...(map.get(label) || []), ...items])
	})
	return [...map.entries()].map(([label, items]) => ({ label, items }))
})
</script>

<style scoped>
.service-list { padding: 0 34rpx 70rpx; }.group-label { display: block; margin: 28rpx 10rpx 24rpx; color: #999; font-family: Inter, sans-serif; font-size: 24rpx; font-weight: 300; line-height: 32rpx; }
.record-card { min-height: 244rpx; margin-bottom: 38rpx; padding: 34rpx 34rpx 20rpx; box-sizing: border-box; border: 2rpx solid #eceff2; border-radius: 20rpx; background: #fff; box-shadow: 0 7rpx 18rpx rgba(31,35,41,.04); }.detail-line { display: flex; align-items: baseline; margin-bottom: 28rpx; font-size: 29rpx; line-height: 40rpx; }.field { color: #999; flex-shrink: 0; }.value { color: #333; }.strong { font-weight: 700; }
.action-button { width: 204rpx; height: 46rpx; margin: -6rpx 0 0; padding: 0; border-radius: 8rpx; background: #fdaf32; color: #fff; font-size: 24rpx; line-height: 46rpx; }.action-button::after { border: 0; }.action-button.disabled { background: #d9d9d9; color: #fff; }
.empty { display: block; padding: 180rpx 0; text-align: center; color: #aaa; font-size: 28rpx; }
</style>
