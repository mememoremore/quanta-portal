<template>
	<view class="workstation-page">
		<member-safe-header title="工位预约" title-size="large" back @back="goBack" />
		<view class="required-button" @click="noticeVisible = true"><text class="required-brand">Quanta</text><text>实验室预约必读</text><text class="required-arrow">›</text></view>

		<view class="date-row">
			<text class="date-arrow" :class="{ disabled: isFirstDate }" @click="changeDate(-1)">‹</text>
			<text class="date-label">{{ displayDate }}</text>
			<text class="date-arrow" :class="{ disabled: isLastDate }" @click="changeDate(1)">›</text>
		</view>
		<view class="legend-row">
			<view v-for="item in legend" :key="item.status" class="legend-item"><view class="legend-color" :class="`status-${item.status}`" /><text>{{ item.label }}</text></view>
		</view>

		<view class="map-card">
			<view class="top-layout">
				<view class="top-seats">
					<template v-for="seatId in topSeatIds" :key="seatId || 'blocked'">
						<view v-if="seatId" class="seat-card" :class="{ 'fully-booked': isFullyBooked(seatById(seatId)), 'fully-available': isFullyAvailable(seatById(seatId)) }" @click="openSeat(seatById(seatId))">
							<view v-for="slot in seatById(seatId)?.slots" :key="slot.period" class="seat-segment" :class="`status-${slot.status}`" />
							<text class="seat-name">{{ seatById(seatId)?.name }}</text>
						</view>
						<view v-else class="blocked-seat"><view class="diagonal" /></view>
					</template>
				</view>
				<view class="printer">打印机</view>
			</view>

			<view class="middle-layout">
				<view class="four-seat-zone available-zone">
					<view v-for="seatId in middleSeatIds" :key="seatId" class="seat-card" :class="{ 'fully-booked': isFullyBooked(seatById(seatId)), 'fully-available': isFullyAvailable(seatById(seatId)) }" @click="openSeat(seatById(seatId))">
						<view v-for="slot in seatById(seatId)?.slots" :key="slot.period" class="seat-segment" :class="`status-${slot.status}`" />
						<text class="seat-name">{{ seatById(seatId)?.name }}</text>
					</view>
				</view>
				<view class="four-seat-zone disabled-zone"><view v-for="index in 4" :key="index" class="empty-desk" /><text class="zone-label">管理员 · 执行层</text></view>
			</view>

			<view class="lower-layout">
				<view v-for="group in 2" :key="group" class="four-seat-zone disabled-zone"><view v-for="index in 4" :key="index" class="empty-desk" /><text class="zone-label">管理层 · 执行层</text></view>
			</view>
			<view class="machine-room">机房</view>
			<text class="room-label">C503 教室</text>
		</view>

		<view v-if="noticeVisible" class="modal-mask" @click="noticeVisible = false">
			<view class="notice-modal" @click.stop>
				<text class="modal-title orange">Quanta之家注意事项</text>
				<scroll-view class="notice-scroll" scroll-y>
					<view class="notice-section"><text class="notice-index">01</text><text>最后离开实验室时请锁门，并将钥匙放回原位。</text></view>
					<view class="notice-section"><text class="notice-index">02</text><text>离开时请检查门口总闸；可关闭总闸，也可关闭灯闸，确保灯光与设备已断电。</text></view>
					<view class="notice-section"><text class="notice-index">03</text><text>空调开放时间与教学区规则一致；若空调没有反应，请检查电源和开放时段。</text></view>
					<text class="notice-subtitle">【工位预约事宜】</text>
					<text class="notice-rule">1. 只有状态为“未预约”的时段可以提交预约。</text>
					<text class="notice-rule">2. 预约信息使用当前登录账号的本人身份资料。</text>
				</scroll-view>
				<button class="single-primary" @click="noticeVisible = false">知道了</button>
			</view>
		</view>

		<view v-if="selectedSeat" class="modal-mask" @click="closeSeatModal">
			<view class="booking-modal" @click.stop>
				<text class="modal-title orange">{{ selectedSeat.name }}</text>
				<view class="slot-list">
					<view v-for="period in periods" :key="period.key" class="slot-row" :class="{ disabled: slotStatus(period.key) !== 'available' }" @click="togglePeriod(period.key)">
						<view class="checkbox" :class="{ checked: selectedPeriods.includes(period.key), locked: slotStatus(period.key) !== 'available' }"><text v-if="selectedPeriods.includes(period.key)">✓</text></view>
						<text class="slot-label">{{ period.label }}</text>
						<text v-if="slotStatus(period.key) !== 'available'" class="slot-state">{{ statusLabel(slotStatus(period.key)) }}</text>
					</view>
				</view>
				<view class="modal-actions"><button class="secondary" :disabled="submitting" @click="closeSeatModal">取消预约</button><button class="primary" :disabled="!selectedPeriods.length || submitting" @click="submitReservation">{{ submitting ? '预约中…' : '确认预约' }}</button></view>
			</view>
		</view>

		<view v-if="fullToast" class="full-toast">{{ fullToast }}</view>
	</view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import MemberSafeHeader from '../../../component/MemberSafeHeader.vue'
import { getWorkstationDay, reserveWorkstation, type Workstation, type WorkstationDay } from '../../../utils/workstationMock'
import { WORKSTATION_PERIODS, shiftWorkstationDate, toLocalDateKey, type WorkstationPeriodKey, type WorkstationSlotStatus } from '../../../utils/workstationRules'

const periods = WORKSTATION_PERIODS
const today = new Date()
const todayKey = toLocalDateKey(today)
const lastDateKey = toLocalDateKey(new Date(today.getFullYear(), today.getMonth(), today.getDate() + 14))
const currentDate = ref(todayKey)
const day = ref<WorkstationDay>({ dateKey: todayKey, workstations: [] })
const noticeVisible = ref(true)
const selectedSeat = ref<Workstation | null>(null)
const selectedPeriods = ref<WorkstationPeriodKey[]>([])
const submitting = ref(false)
const fullToast = ref('')
let toastTimer: ReturnType<typeof setTimeout> | null = null

const legend = [
	{ status: 'booked', label: '已预约' },
	{ status: 'available', label: '未预约' },
	{ status: 'mine', label: '你的预约' },
	{ status: 'unavailable', label: '无法预约' },
] as const
const topSeatIds: Array<string | null> = ['seat-4', 'seat-3', 'seat-2', 'seat-1', 'seat-5', 'seat-6', null, 'seat-7']
const middleSeatIds = ['seat-8', 'seat-11', 'seat-9', 'seat-10']
const isFirstDate = computed(() => currentDate.value === todayKey)
const isLastDate = computed(() => currentDate.value === lastDateKey)
const displayDate = computed(() => { const [year, month, date] = currentDate.value.split('-').map(Number); return `${year}年${month}月${date}日` })

const goBack = () => uni.navigateBack()
const seatById = (id: string) => day.value.workstations.find((seat) => seat.id === id)
const isFullyBooked = (seat?: Workstation) => Boolean(seat?.slots.every((slot) => slot.status === 'booked'))
const isFullyAvailable = (seat?: Workstation) => Boolean(seat?.slots.every((slot) => slot.status === 'available'))
const loadDay = async () => { day.value = await getWorkstationDay(currentDate.value) }
const changeDate = async (delta: number) => {
	const next = shiftWorkstationDate(currentDate.value, delta, today)
	if (next === currentDate.value) return
	currentDate.value = next
	selectedSeat.value = null
	selectedPeriods.value = []
	try { await loadDay() } catch (error: any) { uni.showToast({ title: error?.message || '工位状态加载失败', icon: 'none' }) }
}
const openSeat = (seat?: Workstation) => {
	if (!seat) return
	if (isFullyBooked(seat)) {
		fullToast.value = `「${seat.name}」已被约满！`
		if (toastTimer) clearTimeout(toastTimer)
		toastTimer = setTimeout(() => { fullToast.value = '' }, 1800)
		return
	}
	selectedSeat.value = seat
	selectedPeriods.value = []
}
const closeSeatModal = () => { if (!submitting.value) { selectedSeat.value = null; selectedPeriods.value = [] } }
const slotStatus = (period: WorkstationPeriodKey) => selectedSeat.value?.slots.find((slot) => slot.period === period)?.status || 'unavailable'
const statusLabel = (status: WorkstationSlotStatus) => ({ booked: '已预约', mine: '你的预约', unavailable: '无法预约', available: '' }[status])
const togglePeriod = (period: WorkstationPeriodKey) => {
	if (slotStatus(period) !== 'available' || submitting.value) return
	selectedPeriods.value = selectedPeriods.value.includes(period)
		? selectedPeriods.value.filter((item) => item !== period)
		: [...selectedPeriods.value, period]
}
const submitReservation = async () => {
	if (!selectedSeat.value || !selectedPeriods.value.length || submitting.value) return
	submitting.value = true
	try {
		await reserveWorkstation(selectedSeat.value.id, currentDate.value, selectedPeriods.value)
		selectedSeat.value = null
		selectedPeriods.value = []
		await loadDay()
		uni.showToast({ title: '预约成功', icon: 'success' })
	} catch (error: any) { uni.showToast({ title: error?.message || '预约失败，请稍后重试', icon: 'none' }) }
	finally { submitting.value = false }
}

onShow(async () => {
	currentDate.value = todayKey
	noticeVisible.value = true
	selectedSeat.value = null
	selectedPeriods.value = []
	try { await loadDay() } catch (error: any) { uni.showToast({ title: error?.message || '工位状态加载失败', icon: 'none' }) }
})
</script>

<style scoped>
.workstation-page { min-height: 100vh; box-sizing: border-box; padding: 0 42rpx 70rpx; background: #fff; color: #222; }
.required-button { position: sticky; top: calc(12rpx + env(safe-area-inset-top)); z-index: 12; height: 104rpx; margin: 26rpx 24rpx 28rpx; padding: 0 34rpx; border: 2rpx solid #ffe1be; border-radius: 34rpx; background: rgba(255,255,255,.96); box-shadow: 0 12rpx 28rpx rgba(0,0,0,.06); display: flex; align-items: center; justify-content: center; gap: 18rpx; font-size: 30rpx; }.required-brand { color: #ff5b00; font-weight: 800; }.required-arrow { position: absolute; right: 28rpx; color: #ff8a3c; font-size: 46rpx; }
.date-row { display: flex; align-items: center; justify-content: center; gap: 38rpx; }.date-arrow { width: 54rpx; color: #333; font-size: 56rpx; line-height: 62rpx; text-align: center; }.date-arrow.disabled { color: #d7d7d7; }.date-label { min-width: 250rpx; font-size: 30rpx; font-weight: 600; text-align: center; }.legend-row { margin: 18rpx auto 30rpx; display: flex; flex-wrap: wrap; align-items: center; justify-content: center; gap: 14rpx 28rpx; }.legend-item { display: flex; align-items: center; gap: 10rpx; color: #888; font-size: 22rpx; }.legend-color { width: 28rpx; height: 28rpx; border-radius: 7rpx; box-sizing: border-box; }
.status-available { background: #eafbf1; border: 2rpx solid #9ff0c0; }.status-booked { background: #ff2937; border: 2rpx solid #e80015; }.status-mine { background: #16864c; border: 2rpx solid #096e39; }.status-unavailable { background: #fff8ed; border: 2rpx solid #f8a526; }
.map-card { padding: 42rpx 34rpx 36rpx; border: 2rpx solid #f1f1f1; border-radius: 54rpx; background: #fff; box-shadow: 0 18rpx 46rpx rgba(0,0,0,.07); }.top-layout { display: flex; gap: 18rpx; }.top-seats { flex: 1; padding: 16rpx; border: 2rpx solid #e6e6e6; border-radius: 28rpx; display: grid; grid-template-columns: repeat(4, 1fr); gap: 10rpx; }.seat-card { position: relative; width: 100%; min-width: 0; height: auto; aspect-ratio: 1 / 1; border-radius: 18rpx; overflow: hidden; display: grid; grid-template-columns: repeat(3, 1fr); box-shadow: 0 5rpx 10rpx rgba(0,0,0,.05); }.seat-segment { min-width: 0; border-right-width: 0; border-radius: 0; }.seat-segment:first-child { border-radius: 18rpx 0 0 18rpx; }.seat-segment:last-of-type { border-radius: 0 18rpx 18rpx 0; }.seat-card.fully-available { box-sizing: border-box; border: 2rpx solid #9ff0c0; background: #eafbf1; }.seat-card.fully-booked { box-sizing: border-box; border: 2rpx solid #e80015; background: #ff2937; }.seat-card.fully-available .seat-segment,.seat-card.fully-booked .seat-segment { border: 0; background: transparent; }.seat-name { position: absolute; inset: 0; z-index: 2; display: flex; align-items: center; justify-content: center; color: #075f34; font-size: 22rpx; font-weight: 700; text-shadow: 0 1rpx 1rpx rgba(255,255,255,.8); }.seat-card.fully-booked .seat-name { color: #fff; text-shadow: none; }.blocked-seat { position: relative; width: 100%; height: auto; aspect-ratio: 1 / 1; border-radius: 16rpx; background: #e7e7e7; overflow: hidden; }.diagonal { position: absolute; left: -15%; top: 49%; width: 130%; height: 2rpx; background: #c5c5c5; transform: rotate(-42deg); }.printer { width: 76rpx; border: 2rpx solid #ddd; border-radius: 28rpx; background: #f4f4f4; color: #888; font-size: 24rpx; font-weight: 700; writing-mode: vertical-rl; display: flex; align-items: center; justify-content: center; }
.middle-layout,.lower-layout { margin-top: 24rpx; display: grid; grid-template-columns: 1fr 1fr; gap: 42rpx; }.four-seat-zone { position: relative; padding: 16rpx; border-radius: 28rpx; display: grid; grid-template-columns: 1fr 1fr; gap: 10rpx; }.available-zone { border: 2rpx solid #baf5d0; }.disabled-zone { border: 2rpx solid #ffe0b0; }.empty-desk { height: 92rpx; border: 2rpx solid #ffe0b0; border-radius: 20rpx; background: #fffaf2; }.zone-label { position: absolute; inset: 0; display: flex; align-items: center; justify-content: center; color: rgba(168,117,38,.40); font-size: 22rpx; }.machine-room { height: 174rpx; margin-top: 30rpx; border-radius: 28rpx; background: #e2e2e2; color: #888; font-size: 46rpx; font-weight: 700; display: flex; align-items: center; justify-content: center; }.room-label { display: block; margin-top: 22rpx; color: #888; font-size: 38rpx; font-weight: 800; text-align: center; }
.modal-mask { position: fixed; inset: 0; z-index: 80; padding: 38rpx; box-sizing: border-box; background: rgba(0,0,0,.58); display: flex; align-items: center; justify-content: center; }.notice-modal,.booking-modal { width: 650rpx; max-height: 86vh; box-sizing: border-box; padding: 46rpx 42rpx 38rpx; border: 2rpx solid #ddd; border-radius: 34rpx; background: #fff; display: flex; flex-direction: column; }.modal-title { display: block; font-size: 36rpx; font-weight: 800; text-align: center; }.modal-title.orange { color: #ff5b00; }.notice-scroll { height: 760rpx; max-height: 60vh; margin-top: 30rpx; }.notice-section { min-height: 128rpx; margin-bottom: 18rpx; padding: 24rpx; border: 2rpx solid #f0f0f0; border-radius: 18rpx; color: #555; font-size: 25rpx; line-height: 1.65; box-sizing: border-box; display: flex; gap: 20rpx; }.notice-index { color: #ff6600; font-size: 28rpx; font-weight: 800; }.notice-subtitle { display: block; margin: 24rpx 8rpx 16rpx; font-size: 27rpx; font-weight: 800; }.notice-rule { display: block; margin: 10rpx 8rpx; color: #555; font-size: 24rpx; line-height: 1.6; }.single-primary { width: 180rpx; height: 68rpx; margin-top: 28rpx; padding: 0; border-radius: 10rpx; background: #ff6600; color: #fff; font-size: 28rpx; line-height: 68rpx; }.single-primary::after,.modal-actions button::after { border: 0; }
.booking-modal { width: 630rpx; padding: 52rpx 60rpx 46rpx; }.slot-list { margin-top: 30rpx; }.slot-row { min-height: 82rpx; display: flex; align-items: center; gap: 24rpx; }.slot-row.disabled { color: #c9c9c9; }.checkbox { width: 40rpx; height: 40rpx; border: 3rpx solid #333; border-radius: 10rpx; box-sizing: border-box; color: #fff; display: flex; align-items: center; justify-content: center; }.checkbox.checked { border-color: #ff6600; background: #ff6600; }.checkbox.locked { border-color: #d3d3d3; background: #d3d3d3; }.slot-label { flex: 1; font-size: 28rpx; }.slot-state { font-size: 21rpx; }.modal-actions { margin-top: 36rpx; display: flex; gap: 34rpx; }.modal-actions button { flex: 1; height: 68rpx; margin: 0; padding: 0; border-radius: 10rpx; color: #fff; font-size: 27rpx; line-height: 68rpx; }.secondary { background: #999; }.primary { background: #ff6600; }.modal-actions button[disabled] { opacity: .48; }
.full-toast { position: fixed; left: 50%; top: 44%; z-index: 100; padding: 34rpx 44rpx; border-radius: 16rpx; background: #050505; color: #fdaf32; font-size: 33rpx; white-space: nowrap; transform: translate(-50%,-50%); }
</style>
