<template>
	<view class="card-view-page">
		<member-safe-header title="个人名片" back @back="goBack" />
		<view v-if="card" class="identity-card">
			<view class="avatar">{{ card.name.slice(0, 1) }}</view>
			<view class="identity-copy"><text class="name">{{ card.name }}</text><text class="code">{{ card.code }} · {{ card.position }}</text><view class="tags"><text v-if="card.isActiveTalent" class="tag active">活跃达人</text><text class="tag department">{{ card.department }}</text></view></view>
			<view class="corner-shape" />
		</view>

		<view v-if="card" class="growth-tree">
			<view class="crown">
				<view v-for="leaf in 16" :key="leaf" class="leaf" :class="`leaf-${leaf}`" />
				<view class="node node-intro" @click="locked"><text>简介</text><text class="lock">🔒</text></view>
				<view class="node node-honor" @click="locked"><text>荣誉</text><text class="lock">🔒</text></view>
				<view class="node node-work" @click="locked"><text>作品</text><text class="lock">🔒</text></view>
				<view class="node node-skill" @click="locked"><text>技能</text><text class="lock">🔒</text></view>
				<view class="node node-basic" @click="previewVisible = true">基本信息</view>
			</view>
			<view class="trunk" />
		</view>
		<business-card-preview v-if="card && previewVisible" :card="card" @close="previewVisible = false" />
	</view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import BusinessCardPreview from '../../../component/BusinessCardPreview.vue'
import MemberSafeHeader from '../../../component/MemberSafeHeader.vue'
import { getBusinessCard, type MemberBusinessCard } from '../../../utils/memberMock'

const card = ref<MemberBusinessCard | null>(null)
const previewVisible = ref(false)
const goBack = () => uni.navigateBack()
const locked = () => uni.showToast({ title: '该内容暂未开放', icon: 'none' })
onLoad(async (query) => {
	card.value = await getBusinessCard(String(query?.id || ''))
	if (!card.value) uni.showToast({ title: '该成员尚未创建名片', icon: 'none' })
})
</script>

<style scoped>
.card-view-page { min-height: 100vh; padding: 0 52rpx 80rpx; box-sizing: border-box; background: #fff; overflow: hidden; }
.identity-card { position: relative; height: 270rpx; margin-top: 62rpx; padding: 44rpx 48rpx; border: 2rpx solid #eee; border-radius: 52rpx; box-sizing: border-box; box-shadow: 0 14rpx 42rpx rgba(0,0,0,.05); display: flex; align-items: center; overflow: hidden; }.avatar { width: 134rpx; height: 134rpx; border: 8rpx solid #ffe1b8; border-radius: 50%; background: #ff9d19; color: #fff; font-size: 62rpx; font-weight: 700; display: flex; align-items: center; justify-content: center; z-index: 2; }.identity-copy { margin-left: 36rpx; z-index: 2; display: flex; flex-direction: column; }.name { font-size: 48rpx; font-weight: 700; color: #333; }.code { margin-top: 12rpx; font-size: 27rpx; color: #929292; }.tags { display: flex; gap: 14rpx; margin-top: 26rpx; }.tag { padding: 9rpx 20rpx; border-radius: 999rpx; font-size: 22rpx; font-weight: 600; }.active { color: #ff5a00; background: #fff4e8; }.department { color: #00a63e; background: #effcf4; }.corner-shape { position: absolute; right: -70rpx; top: -92rpx; width: 280rpx; height: 280rpx; border-radius: 0 0 0 100%; background: linear-gradient(135deg,#eee,#d9d6d1); }
.growth-tree { position: relative; width: 100%; height: 880rpx; margin-top: 90rpx; }.crown { position: relative; z-index: 2; height: 540rpx; }.leaf { position: absolute; width: 190rpx; height: 190rpx; border-radius: 50%; background: #35d676; }.leaf-1{left:120rpx;top:0}.leaf-2{left:260rpx;top:12rpx;background:#70e89c}.leaf-3{left:20rpx;top:92rpx}.leaf-4{left:180rpx;top:100rpx;background:#2dc86b}.leaf-5{left:360rpx;top:112rpx;background:#72e99d}.leaf-6{left:70rpx;top:220rpx;background:#6ae695}.leaf-7{left:230rpx;top:220rpx}.leaf-8{left:390rpx;top:240rpx;background:#60df8d}.leaf-9{left:25rpx;top:350rpx}.leaf-10{left:170rpx;top:355rpx;background:#68e493}.leaf-11{left:325rpx;top:360rpx}.leaf-12{left:440rpx;top:342rpx;background:#61df8d}.leaf-13{left:115rpx;top:450rpx}.leaf-14{left:280rpx;top:455rpx}.leaf-15{left:400rpx;top:452rpx}.leaf-16{left:235rpx;top:330rpx;background:#39d77a}
.trunk { position: absolute; top: 450rpx; left: 190rpx; width: 280rpx; height: 440rpx; background: linear-gradient(90deg,#9a5126,#a95f2e); clip-path: polygon(40% 0,60% 0,100% 100%,0 100%); }.node { position: absolute; z-index: 4; height: 76rpx; min-width: 130rpx; padding: 0 28rpx; border: 2rpx solid #ddd; border-radius: 26rpx; background: #fff; box-shadow: 0 8rpx 15rpx rgba(0,0,0,.12); color: #a1a1a1; font-size: 28rpx; font-weight: 600; display: flex; align-items: center; justify-content: center; gap: 12rpx; }.node-intro{left:18rpx;top:135rpx}.node-honor{right:12rpx;top:110rpx}.node-work{right:65rpx;top:270rpx}.node-skill{left:5rpx;top:390rpx}.node-basic{right:95rpx;top:440rpx;color:#333;border-color:#fdaf32}.lock { font-size: 22rpx; }
</style>
