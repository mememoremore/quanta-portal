<template>
	<view class="join-us-page">
		<view class="top-bar"><view class="back-btn" @click="handleBack"><image class="back-icon" :src="backIconSrc" mode="aspectFit" /></view><text class="page-title">Join us</text><view class="top-bar-spacer" /></view>
		<view class="sub-tab-bar"><view class="sub-tab-item" :class="{ 'sub-tab-item-active': activeTab === 'process' }" @click="switchTab('process')"><text class="sub-tab-text" :class="{ 'sub-tab-text-active': activeTab === 'process' }">应聘流程</text><view v-if="activeTab === 'process'" class="sub-tab-indicator" /></view><view class="sub-tab-item" :class="{ 'sub-tab-item-active': activeTab === 'submit' }" @click="switchTab('submit')"><text class="sub-tab-text" :class="{ 'sub-tab-text-active': activeTab === 'submit' }">投递简历</text><view v-if="activeTab === 'submit'" class="sub-tab-indicator" /></view></view>
		<view class="tab-content" :class="{ 'tab-content--process': activeTab === 'process' }"><Process v-if="activeTab === 'process'" class="process-component" @go-submit="switchTab('submit')" /><Submit v-else ref="submitRef" /></view>
	</view>
</template>

<script setup lang="js">
import { ref } from 'vue'
import Process from './process.vue'
import Submit from './submit.vue'

const activeTab = ref('process')
const submitRef = ref(null)
const backIconSrc = `data:image/svg+xml;utf8,${encodeURIComponent('<svg width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg"><path d="m15 18-6-6 6-6" stroke="#101828" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/></svg>')}`

const leaveSubmit = (next) => {
	if (activeTab.value !== 'submit' || !submitRef.value?.hasUnsavedChanges()) { next(); return }
	uni.showModal({
		title: '是否保存简历',
		content: '你有未保存的简历内容，是否保存后再离开？',
		confirmText: '保存',
		cancelText: '不保存',
		success: (result) => {
			if (result.confirm) {
				if (!submitRef.value.doSave({ silent: true })) return
				next()
				return
			}
			if (result.cancel) next()
		},
	})
}
const switchTab = (tab) => { if (tab !== activeTab.value) leaveSubmit(() => { activeTab.value = tab }) }
const handleBack = () => leaveSubmit(() => uni.navigateBack({ delta: 1 }))
</script>

<style scoped>
.join-us-page{width:100%;height:100vh;min-height:100vh;background:#fff;box-sizing:border-box;display:flex;flex-direction:column;overflow:hidden}.top-bar{width:100%;display:flex;align-items:center;justify-content:space-between;padding:0 32rpx;box-sizing:border-box}.back-btn,.top-bar-spacer{width:48rpx;height:48rpx;margin-top:88rpx;flex-shrink:0}.back-btn{display:flex;align-items:center;justify-content:center}.back-icon{width:40rpx;height:40rpx}.page-title{margin-top:88rpx;font-size:36rpx;line-height:48rpx;font-weight:700;color:#101828}.sub-tab-bar{margin-top:37rpx;display:flex;justify-content:center;gap:180rpx}.sub-tab-item{display:flex;flex-direction:column;align-items:center;min-width:64rpx}.sub-tab-text{font-size:32rpx;line-height:48rpx;font-weight:700;color:#99a1af}.sub-tab-text-active{color:#101828}.sub-tab-indicator{margin-top:12rpx;width:64rpx;height:6rpx;border-radius:999rpx;background:#ff6600;box-shadow:0 0 16rpx rgba(255,102,0,.6)}.tab-content{flex:1;width:100%;min-height:0;display:flex}.process-component{flex:1;min-height:0;display:flex}.tab-content--process{background:#f8fafc}
</style>
