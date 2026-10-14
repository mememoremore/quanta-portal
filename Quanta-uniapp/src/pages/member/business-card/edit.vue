<template>
	<view class="edit-page">
		<member-safe-header title="创建名片" back @back="goBack" />
		<view class="section-heading"><text>基本信息</text><text class="preview-link" @click="previewVisible = true">预览名片</text></view>
		<view class="info-card">
			<view class="info-row"><text>部门</text><text>{{ form.department }}</text></view>
			<view class="info-row"><text>届次</text><text>{{ displayBatch }}</text></view>
			<view class="info-row"><text>职位</text><text>{{ form.position }}</text></view>
		</view>
		<text class="bio-title">个人简介</text>
		<view class="bio-card"><textarea v-model="form.bio" class="bio-input" maxlength="800" :disabled="!editing" placeholder="请介绍一下你自己…" /><text class="counter">{{ form.bio.length }}/800</text></view>
		<view class="footer"><button class="edit-btn" @click="editing = true">修改</button><button class="save-btn" @click="save">保存并创建名片</button></view>
		<business-card-preview v-if="previewVisible" :card="form" @close="previewVisible = false" />
	</view>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import BusinessCardPreview from '../../../component/BusinessCardPreview.vue'
import MemberSafeHeader from '../../../component/MemberSafeHeader.vue'
import { sessionState } from '../../../stores/user'
import { getBusinessCard, saveBusinessCard, type MemberBusinessCard } from '../../../utils/memberMock'

const account = computed(() => sessionState.profile?.account || 'PM2301')
const form = reactive<MemberBusinessCard>({ memberId: '', name: '', code: '', department: '产品部', batch: '20th', position: '首席执行官', role: 'COO', bio: '', hasBusinessCard: false, isActiveTalent: true })
const editing = ref(true)
const previewVisible = ref(false)
const displayBatch = computed(() => form.batch.replace('th', '届'))
const goBack = () => uni.navigateBack()
const save = async () => {
	if (!form.bio.trim()) return uni.showToast({ title: '请填写个人简介', icon: 'none' })
	const saved = await saveBusinessCard(form)
	editing.value = false
	uni.redirectTo({ url: `/pages/member/business-card/view?id=${encodeURIComponent(saved.memberId)}` })
}
onLoad(async () => {
	const profile = sessionState.profile
	Object.assign(form, { memberId: account.value, code: account.value, name: profile?.name || '方东升', department: profile?.department || '产品部', batch: profile?.batch || '20th' })
	const saved = await getBusinessCard(account.value)
	if (saved) { Object.assign(form, saved); editing.value = false }
})
</script>

<style scoped>
.edit-page { min-height:100vh; box-sizing:border-box; padding:0 56rpx calc(150rpx + env(safe-area-inset-bottom)); background:linear-gradient(135deg,#fff 65%,rgba(255,237,212,.28)); }.section-heading{display:flex;justify-content:space-between;margin:55rpx 4rpx 22rpx;font-size:32rpx}.preview-link{color:#fdaf32}.info-card{padding:35rpx 48rpx;border:2rpx solid #eee;border-radius:48rpx;background:#fff;box-shadow:0 15rpx 42rpx rgba(0,0,0,.04)}.info-row{height:62rpx;display:flex;justify-content:space-between;align-items:center;font-size:28rpx;color:#929292}.bio-title{display:block;margin:45rpx 4rpx 22rpx;font-size:32rpx}.bio-card{position:relative;height:370rpx;padding:28rpx 36rpx 60rpx;box-sizing:border-box;border:2rpx solid #eee;border-radius:48rpx;background:#fff;box-shadow:0 15rpx 42rpx rgba(0,0,0,.04)}.bio-input{width:100%;height:100%;font-size:28rpx;line-height:1.6}.counter{position:absolute;right:40rpx;bottom:28rpx;color:#c5c5c5;font-size:24rpx}.footer{position:fixed;left:56rpx;right:56rpx;bottom:calc(50rpx + env(safe-area-inset-bottom));display:flex;gap:14rpx}.edit-btn,.save-btn{height:86rpx;border-radius:20rpx;background:#fdaf32;color:#fff;font-size:28rpx;line-height:86rpx}.edit-btn{width:160rpx}.save-btn{flex:1}.edit-btn::after,.save-btn::after{border:0}
</style>
