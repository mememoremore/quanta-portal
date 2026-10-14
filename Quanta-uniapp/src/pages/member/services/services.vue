<template>
	<view class="services-page">
		<member-safe-header title="我的服务" back @back="goBack" />
		<view class="tabs">
			<view v-for="tab in tabs" :key="tab.key" class="tab" :class="{ active: activeTab === tab.key }" @click="activeTab = tab.key"><text>{{ tab.label }}</text><view class="tab-line" /></view>
		</view>
		<scroll-view class="content" scroll-y>
			<reservation-list v-if="activeTab === 'reservation'" :records="snapshot.reservations" @cancel="openCancel" />
			<borrow-list v-else-if="activeTab === 'borrow'" :records="snapshot.borrows" @return="openReturn" />
			<order-list v-else :records="snapshot.orders" @explain="reviewVisible = true" />
		</scroll-view>

		<view v-if="cancelTarget" class="modal-mask" @click="cancelTarget = null">
			<view class="modal-card compact" @click.stop>
				<text class="modal-title">取消预约</text>
				<text class="modal-copy">确认取消 {{ cancelTarget.workspace }} 的预约吗？取消后需要重新选择时间。</text>
				<view class="modal-actions"><button class="secondary" @click="cancelTarget = null">暂不取消</button><button class="primary" :disabled="submitting" @click="submitCancel">确认取消</button></view>
			</view>
		</view>

		<view v-if="returnTarget" class="modal-mask" @click="closeReturn">
			<view class="modal-card return-modal" @click.stop>
				<text class="modal-title">归还书籍</text>
				<text class="return-label">请选择归还日期</text>
				<picker mode="date" :value="returnDate" :end="today" @change="handleDateChange"><view class="date-picker" :class="{ placeholder: !returnDate }">{{ returnDate ? displayPickerDate(returnDate) : '点击选择日期' }}</view></picker>
				<button class="single-primary" :disabled="submitting" @click="submitReturn">确认归还</button>
				<view class="return-tip"><image src="/static/icon/member/service-notice.svg" mode="aspectFit" /><text>该时间是你归还书籍的时间，管理员会根据该时间检查确认是否归还。</text></view>
			</view>
		</view>

		<view v-if="reviewVisible" class="modal-mask" @click="reviewVisible = false">
			<view class="modal-card review-modal" @click.stop><text class="review-copy">{{ ORDER_REVIEW_MESSAGE }}</text><button class="single-primary" @click="reviewVisible = false">知道了</button></view>
		</view>
	</view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import MemberSafeHeader from '../../../component/MemberSafeHeader.vue'
import ReservationList from '../../../component/member-services/ReservationList.vue'
import BorrowList from '../../../component/member-services/BorrowList.vue'
import OrderList from '../../../component/member-services/OrderList.vue'
import { ORDER_REVIEW_MESSAGE, type BorrowRecord, type MemberServicesSnapshot } from '../../../utils/memberServiceMock'
import type { ReservationRecord } from '../../../utils/memberServiceRules'
import { getMemberServices } from '../../../api/memberServices'

const tabs = [{ key: 'reservation', label: '预约' }, { key: 'borrow', label: '借阅' }, { key: 'order', label: '订购' }] as const
const activeTab = ref<(typeof tabs)[number]['key']>('reservation')
const snapshot = ref<MemberServicesSnapshot>({ reservations: [], borrows: [], orders: [] })
const cancelTarget = ref<ReservationRecord | null>(null)
const returnTarget = ref<BorrowRecord | null>(null)
const returnDate = ref('')
const reviewVisible = ref(false)
const submitting = ref(false)
const pad = (value: number) => String(value).padStart(2, '0')
const todayValue = new Date()
const today = `${todayValue.getFullYear()}-${pad(todayValue.getMonth() + 1)}-${pad(todayValue.getDate())}`

const goBack = () => uni.navigateBack()
const load = async () => { snapshot.value = await getMemberServices() }
const openCancel = (record: ReservationRecord) => { cancelTarget.value = record }
const openReturn = (record: BorrowRecord) => { returnTarget.value = record; returnDate.value = '' }
const closeReturn = () => { returnTarget.value = null; returnDate.value = '' }
const handleDateChange = (event: any) => { returnDate.value = event.detail.value }
const displayPickerDate = (source: string) => { const [year, month, day] = source.split('-').map(Number); return `${year}年${month}月${day}日` }
const submitCancel = async () => {
	if (!cancelTarget.value || submitting.value) return
	const target = cancelTarget.value
	submitting.value = true
	try { snapshot.value.reservations = snapshot.value.reservations.filter((record) => record.id !== target.id); cancelTarget.value = null; uni.showToast({ title: '本地模拟取消，尚未同步后端', icon: 'none' }) }
	catch (error: any) { uni.showToast({ title: error?.message || '操作失败，请稍后重试', icon: 'none' }) }
	finally { submitting.value = false }
}
const submitReturn = async () => {
	if (!returnTarget.value || submitting.value) return
	if (!returnDate.value) return uni.showToast({ title: '请选择归还日期', icon: 'none' })
	submitting.value = true
	try { const record = snapshot.value.borrows.find((item) => item.id === returnTarget.value?.id); if (record) { record.status = 'return-pending'; record.returnedOn = returnDate.value }; closeReturn(); uni.showToast({ title: '本地模拟归还，尚未同步后端', icon: 'none' }) }
	catch (error: any) { uni.showToast({ title: error?.message || '操作失败，请稍后重试', icon: 'none' }) }
	finally { submitting.value = false }
}

onShow(async () => { try { await load() } catch (error: any) { uni.showToast({ title: error?.message || '服务记录加载失败', icon: 'none' }) } })
</script>

<style scoped>
.services-page { height: 100vh; box-sizing: border-box; background: rgba(255,237,212,.3); color: #111; display: flex; flex-direction: column; }
.tabs { height: 124rpx; display: flex; align-items: center; justify-content: space-around; flex-shrink: 0; }.tab { width: 160rpx; height: 100%; position: relative; color: #999; font-size: 31rpx; font-weight: 600; display: flex; align-items: center; justify-content: center; }.tab.active { color: #333; }.tab-line { position: absolute; left: 50%; bottom: 8rpx; width: 64rpx; height: 7rpx; border-radius: 999rpx; background: transparent; transform: translateX(-50%); }.tab.active .tab-line { background: #ff6600; }.content { flex: 1; height: 0; }
.modal-mask { position: fixed; inset: 0; z-index: 80; padding: 40rpx; box-sizing: border-box; background: rgba(0,0,0,.56); display: flex; align-items: center; justify-content: center; }.modal-card { width: 600rpx; box-sizing: border-box; padding: 48rpx 54rpx; border: 2rpx solid #ddd; border-radius: 34rpx; background: #fff; }.modal-card.compact { padding-bottom: 42rpx; }.modal-title { display: block; text-align: center; font-size: 34rpx; font-weight: 700; }.modal-copy { display: block; margin-top: 34rpx; color: #333; font-size: 28rpx; line-height: 1.65; }.modal-actions { display: flex; gap: 28rpx; margin-top: 40rpx; }.modal-actions button,.single-primary { border-radius: 10rpx; color: #fff; font-size: 28rpx; }.modal-actions button::after,.single-primary::after { border: 0; }.secondary { flex: 1; background: #999; }.primary { flex: 1; background: #ff6600; }
.return-modal { padding-bottom: 42rpx; }.return-label { display: block; margin-top: 38rpx; font-size: 29rpx; }.date-picker { height: 72rpx; margin-top: 22rpx; padding: 0 24rpx; border: 2rpx solid #eee; border-radius: 12rpx; color: #333; font-size: 27rpx; line-height: 72rpx; }.date-picker.placeholder { color: #aaa; }.single-primary { width: 210rpx; height: 66rpx; margin-top: 34rpx; padding: 0; background: #ff6600; line-height: 66rpx; }.return-tip { margin-top: 34rpx; display: flex; align-items: flex-start; gap: 12rpx; color: #ff6600; font-size: 20rpx; font-weight: 600; line-height: 1.5; }.return-tip image { width: 26rpx; height: 26rpx; margin-top: 2rpx; flex-shrink: 0; }
.review-modal { padding: 56rpx 58rpx 46rpx; }.review-copy { display: block; font-family: Inter, sans-serif; font-size: 32rpx; font-weight: 400; line-height: 48rpx; letter-spacing: .13em; }.review-modal .single-primary { margin-top: 40rpx; }
</style>
