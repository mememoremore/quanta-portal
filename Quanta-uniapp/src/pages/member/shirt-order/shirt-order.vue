<template>
	<view class="order-page">
		<member-safe-header title="塔服订购" back @back="goBack" />
		<image class="announcement" src="/static/icon/member/shirt-announcement.svg" mode="aspectFit" @click="showNotice" />
		<view class="product-card">
			<swiper class="product-swiper" :indicator-dots="product.images.length > 1" indicator-color="rgba(255,255,255,.55)" indicator-active-color="#ff9f24" circular>
				<swiper-item v-for="image in product.images" :key="image"><image class="product-image" :src="image" mode="aspectFill" /></swiper-item>
			</swiper>
		</view>

		<view class="section"><text class="section-title">塔服颜色</text>
			<view class="option-row color-row"><view v-for="item in colors" :key="item" class="option" @click="color = item"><view class="radio" :class="{ selected: color === item }" /><text>{{ item }}</text></view></view>
		</view>
		<view class="section"><text class="section-title">塔服尺寸</text>
			<view class="size-grid"><view v-for="item in sizes" :key="item" class="option" @click="size = item"><view class="radio" :class="{ selected: size === item }" /><text>{{ item }}</text></view></view>
		</view>
		<view class="section price-section"><text class="section-title">塔服单价（以实物通知为准）</text><text class="payment-note">支付时请备注你的真实姓名，保留付款成功的截图</text><button class="pay-button" @click="startOrder">保存订购信息</button></view>

		<view v-if="confirmVisible" class="modal-mask" @click="confirmVisible = false"><view class="confirm-card" @click.stop>
			<text class="confirm-text">确认订购“{{ color }}”、“{{ size }}码”的塔服？到货后将会通过大群通知取货。</text>
			<view class="confirm-actions"><button class="cancel-btn" @click="confirmVisible = false">取消订购</button><button class="confirm-btn" @click="confirmOrder">确认订购</button></view>
		</view></view>
	</view>
</template>

<script setup lang="js">
import { onMounted, ref } from 'vue'
import MemberSafeHeader from '../../../component/MemberSafeHeader.vue'
import { validateShirtSelection } from '../../../utils/memberMock'
import { createDraftShirtOrder, getShirtProduct } from '../../../api/clothing'

const product = ref({ itemId: 0, name: '', images: [], colors: [], sizes: [], price: null })
const colors = ref([])
const sizes = ref([])
const color = ref('')
const size = ref('')
const confirmVisible = ref(false)
onMounted(async () => {
	try {
		product.value = await getShirtProduct()
		colors.value = product.value.colors
		sizes.value = product.value.sizes
	} catch (error) { uni.showToast({ title: error?.message || '塔服信息加载失败', icon: 'none' }) }
})
const goBack = () => uni.navigateBack()
const showNotice = () => uni.showToast({ title: '请按需选择颜色与尺码', icon: 'none' })
const startOrder = () => {
	const message = validateShirtSelection({ color: color.value, size: size.value })
	if (message) return uni.showToast({ title: message, icon: 'none' })
	confirmVisible.value = true
}
const confirmOrder = async () => {
	try {
		await createDraftShirtOrder(product.value, { color: color.value, size: size.value })
		confirmVisible.value = false
		uni.showToast({ title: '订购信息已保存到后端，待后续支付', icon: 'none', duration: 2600 })
	} catch (error) { uni.showToast({ title: error?.message || '订购信息保存失败', icon: 'none' }) }
}
</script>

<style scoped>
.order-page { min-height: 100vh; box-sizing: border-box; padding: 0 50rpx 80rpx; background: #fff; color: #111; }
.announcement { display: block; margin-top: 32rpx; width: 48rpx; height: 48rpx; }
.product-card { margin-top: 22rpx; padding: 20rpx; border-radius: 12rpx; background: #fff; box-shadow: 0 12rpx 42rpx rgba(0,0,0,.14); }
.product-swiper { width: 100%; height: 690rpx; border-radius: 10rpx; overflow: hidden; }
.product-image { width: 100%; height: 100%; display: block; }
.section { margin-top: 54rpx; }.section-title { display: block; font-size: 36rpx; line-height: 52rpx; font-weight: 700; }
.option-row { display: flex; margin-top: 30rpx; }.color-row .option { width: 34%; }
.option { min-height: 58rpx; display: flex; align-items: center; gap: 14rpx; font-size: 28rpx; }
.radio { width: 24rpx; height: 24rpx; border: 2rpx solid #929292; border-radius: 50%; box-sizing: border-box; }.radio.selected { border: 7rpx solid #fd9f24; }
.size-grid { display: grid; grid-template-columns: repeat(3, 1fr); row-gap: 24rpx; margin-top: 30rpx; }
.payment-note { display: block; margin-top: 24rpx; font-size: 23rpx; line-height: 36rpx; color: #ff6600; text-align: center; }
.pay-button { width: 330rpx; height: 88rpx; margin-top: 20rpx; border-radius: 18rpx; background: #08bf65; color: #fff; font-size: 28rpx; line-height: 88rpx; }.pay-button::after,.cancel-btn::after,.confirm-btn::after { border: 0; }
.modal-mask { position: fixed; inset: 0; z-index: 50; padding: 0 28rpx; box-sizing: border-box; background: rgba(0,0,0,.34); display: flex; align-items: center; justify-content: center; }
.confirm-card { width: 600rpx; box-sizing: border-box; padding: 68rpx 56rpx 48rpx; border-radius: 32rpx; border: 2rpx solid #ddd; background: #fff; }
.confirm-text { font-size: 30rpx; line-height: 1.65; }.confirm-actions { display: flex; justify-content: space-between; gap: 50rpx; margin-top: 54rpx; }
.cancel-btn,.confirm-btn { flex: 1; height: 72rpx; padding: 0; border-radius: 10rpx; color: #fff; font-size: 28rpx; line-height: 72rpx; }.cancel-btn { background: #999; }.confirm-btn { background: #ff6600; }
</style>
