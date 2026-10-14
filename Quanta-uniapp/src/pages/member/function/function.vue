<template>
	<view class="function-page">
		<member-safe-header mode="brand" title="Quanta">
			<template #actions><view class="notice-wrap" @click="openNotices"><image class="notice-icon" src="/static/icon/member/announcement.svg" mode="aspectFit" /><view v-if="unreadCount" class="notice-dot" /></view></template>
		</member-safe-header>
		<view class="feature-grid">
			<view v-for="item in features" :key="item.key" class="feature-card" @click="openFeature(item.key)">
				<view class="feature-icon-shell" :style="{ background: item.background }">
					<image class="feature-icon" :src="item.icon" mode="aspectFit" />
				</view>
				<text class="feature-title">{{ item.title }}</text>
			</view>
		</view>
		<member-tab active="menu" />
	</view>
</template>

<script setup lang="js">
import { onMounted, ref } from 'vue'
import MemberSafeHeader from '../../../component/MemberSafeHeader.vue'
import MemberTab from '../../../component/member_Tab.vue'
import { getTopNotices, openNoticePicker } from '../../../api/notice'
import { openMaterialPicker } from '../../../api/material'

const features = [
	{ key: 'workstation', title: '工位预约', icon: '/static/icon/member/workstation.svg', background: '#fff4e8' },
	{ key: 'books', title: '图书借阅', icon: '/static/icon/member/books.svg', background: '#edf4ff' },
	{ key: 'shirt', title: '塔服订购', icon: '/static/icon/member/shirt.svg', background: '#ebfbf2' },
	{ key: 'learning', title: '学习资源', icon: '/static/icon/member/learning.svg', background: '#faf1ff' },
]

const showUnavailable = () => uni.showToast({ title: '该功能即将开放', icon: 'none' })
const unreadCount = ref(0)
const loadNoticeCount = async () => { try { unreadCount.value = (await getTopNotices()).unreadCount } catch { unreadCount.value = 0 } }
const openNotices = async () => { try { await openNoticePicker(); await loadNoticeCount() } catch (error) { uni.showToast({ title: error?.message || '通知加载失败', icon: 'none' }) } }
const openFeature = (key) => {
	if (key === 'workstation') {
		uni.navigateTo({ url: '/pages/member/workstation/workstation' })
		return
	}
	if (key === 'books') {
		uni.navigateTo({ url: '/pages/member/library/library' })
		return
	}
	if (key === 'shirt') {
		uni.navigateTo({ url: '/pages/member/shirt-order/shirt-order' })
		return
	}
	if (key === 'learning') {
		openMaterialPicker().catch((error) => uni.showToast({ title: error?.message || '学习资料加载失败', icon: 'none' }))
		return
	}
	showUnavailable()
}
onMounted(loadNoticeCount)
</script>

<style scoped>
.function-page { min-height: 100vh; box-sizing: border-box; padding: 0 46rpx calc(230rpx + env(safe-area-inset-bottom)); background: linear-gradient(135deg, rgba(255,237,212,.32), #fff 35%, #fff 70%, rgba(255,237,212,.28)); }
.notice-wrap { position: relative; width: 56rpx; height: 56rpx; display: flex; align-items: center; justify-content: center; }
.notice-icon { width: 44rpx; height: 44rpx; }
.notice-dot { position: absolute; right: 4rpx; top: 2rpx; width: 14rpx; height: 14rpx; border-radius: 50%; background: #fdaf32; }
.feature-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 32rpx; margin-top: 84rpx; }
.feature-card { height: 324rpx; border: 2rpx solid #eee; border-radius: 42rpx; background: #fff; box-shadow: 0 18rpx 46rpx rgba(0,0,0,.055); display: flex; flex-direction: column; align-items: center; justify-content: center; }
.feature-icon-shell { width: 112rpx; height: 112rpx; border-radius: 32rpx; display: flex; align-items: center; justify-content: center; }
.feature-icon { width: 52rpx; height: 52rpx; }
.feature-title { margin-top: 34rpx; font-size: 31rpx; line-height: 44rpx; font-weight: 700; color: #333; }
</style>
