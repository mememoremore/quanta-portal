<template>
	<view class="profile-page">
		<view class="hero">
			<view class="identity">
				<view class="avatar"><image v-if="profile.avatar" :src="profile.avatar" mode="aspectFill" /><text v-else>{{ surname }}</text></view>
				<view class="identity-copy"><text class="name">{{ profile.name }}</text><text class="account">工号：{{ profile.account }}</text><view class="department-pill"><view class="department-dot" /><text>{{ profile.department || '未分配部门' }}</text></view></view>
			</view>
		</view>

		<view class="action-card">
			<view v-for="item in actions" :key="item.key" class="action-row" @click="handleAction(item.key)">
				<view class="action-icon" :style="{ background: item.background }"><image :src="item.icon" mode="aspectFit" /></view>
				<text class="action-title">{{ item.title }}</text><view class="chevron"><image src="/static/icon/member/chevron-right.svg" mode="aspectFit" /></view>
			</view>
		</view>
		<member-tab active="profile" />
	</view>
</template>

<script setup lang="js">
import { computed, onMounted } from 'vue'
import MemberTab from '../../../component/member_Tab.vue'
import { sessionState, updateProfile } from '../../../stores/user'
import { getCurrentProfileApi } from '../../../api/user'

const fallback = { id: 'PM2301', name: '方东升', account: 'PM2301', role: 'tower', department: '产品部', batch: '20th' }
const profile = computed(() => sessionState.profile || fallback)
const surname = computed(() => profile.value.name?.slice(0, 1) || 'Q')
const actions = [
	{ key: 'card', title: '创建名片', icon: '/static/icon/member/create-card.svg', background: '#e7faff' },
	{ key: 'services', title: '我的服务', icon: '/static/icon/member/services.svg', background: '#edf4ff' },
	{ key: 'help', title: '帮助中心', icon: '/static/icon/member/help.svg', background: '#fff4e8' },
	{ key: 'security', title: '账号安全', icon: '/static/icon/member/security.svg', background: '#ebfbf2' },
]
const unavailable = () => uni.showToast({ title: '功能即将开放', icon: 'none' })

const handleAction = (key) => {
	if (key === 'card') return uni.navigateTo({ url: '/pages/member/business-card/edit' })
	if (key === 'services') return uni.navigateTo({ url: '/pages/member/services/services' })
	if (key === 'security') return uni.navigateTo({ url: '/pages/member/security/security' })
	unavailable()
}

onMounted(async () => {
	try { updateProfile(await getCurrentProfileApi('tower')) } catch (error) { uni.showToast({ title: error?.message || '用户信息加载失败', icon: 'none' }) }
})
</script>

<style scoped>
.profile-page { min-height: 100vh; box-sizing: border-box; padding-bottom: calc(230rpx + env(safe-area-inset-bottom)); background: linear-gradient(135deg, #fff 55%, rgba(255,237,212,.28)); }
.hero { height: 470rpx; padding: calc(138rpx + env(safe-area-inset-top)) 64rpx 0; box-sizing: border-box; border-radius: 0 0 74rpx 74rpx; background: linear-gradient(135deg, #ff9e20, #ffb636); }
.identity { display: flex; align-items: center; }.avatar { width: 174rpx; height: 174rpx; border-radius: 50%; border: 10rpx solid rgba(255,255,255,.5); background: #ff9716; box-shadow: 0 18rpx 38rpx rgba(120,70,0,.28); display: flex; align-items: center; justify-content: center; overflow: hidden; color: #fff; font-size: 66rpx; font-weight: 700; }.avatar image { width: 100%; height: 100%; }
.identity-copy { margin-left: 46rpx; color: #fff; display: flex; flex-direction: column; }.name { font-size: 54rpx; line-height: 70rpx; font-weight: 700; }.account { margin-top: 8rpx; font-size: 28rpx; }.department-pill { margin-top: 30rpx; width: 154rpx; height: 48rpx; border: 2rpx solid rgba(255,255,255,.36); border-radius: 999rpx; background: rgba(255,255,255,.16); display: flex; align-items: center; justify-content: center; gap: 14rpx; font-size: 24rpx; }.department-dot { width: 16rpx; height: 16rpx; border-radius: 50%; background: #67e7b2; }
.action-card { margin: -38rpx 48rpx 0; padding: 32rpx 60rpx; border-radius: 54rpx; background: #fff; box-shadow: 0 20rpx 62rpx rgba(0,0,0,.08); position: relative; z-index: 2; }
.action-row { height: 154rpx; display: flex; align-items: center; }.action-icon { width: 80rpx; height: 80rpx; border-radius: 24rpx; display: flex; align-items: center; justify-content: center; }.action-icon image { width: 42rpx; height: 42rpx; }.action-title { flex: 1; margin-left: 34rpx; font-size: 32rpx; font-weight: 700; color: #333; }.chevron { width: 56rpx; height: 56rpx; border: 3rpx solid #e2e2e2; border-radius: 50%; display: flex; align-items: center; justify-content: center; }.chevron image { width: 24rpx; height: 24rpx; }
</style>
