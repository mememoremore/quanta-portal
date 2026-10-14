<template>
	<view class="member-home">
		<member-safe-header class="page-header" mode="brand" title="Quanta">
			<template #actions>
				<view class="header-actions">
					<image class="header-icon" :src="searchIconSrc" mode="aspectFit" @click="showUnavailable" />
					<image class="header-icon header-icon-bell" :src="bellIconSrc" mode="aspectFit" @click="openNotices" />
				</view>
			</template>
		</member-safe-header>

		<swiper
			class="banner-swiper"
			:indicator-dots="bannerList.length > 1"
			indicator-color="rgba(255,255,255,0.4)"
			indicator-active-color="#ffffff"
			circular
			autoplay
		>
			<swiper-item v-for="item in bannerList" :key="item.id">
				<view class="banner-card">
					<image class="banner-image" :src="item.image" mode="aspectFill" />
					<view class="banner-overlay">
						<text class="banner-text">{{ item.title }}</text>
						<view class="banner-arrow-btn" @click="handleBannerClick(item)">
							<image class="banner-arrow-icon" :src="arrowIconSrc" mode="aspectFit" />
						</view>
					</view>
				</view>
			</swiper-item>
		</swiper>

		<text class="section-title">近期活动</text>

		<view class="activity-grid">
			<view
				v-for="item in activityList"
				:key="item.id"
				class="activity-card"
				@click="handleActivityClick(item)"
			>
				<view class="activity-number">
					<text class="activity-number-text">{{ item.id }}</text>
				</view>
				<view class="activity-info">
					<text class="activity-title">{{ item.title }}</text>
					<text class="activity-subtitle">{{ item.subtitle }}</text>
				</view>
			</view>
		</view>
		<text v-if="!loading && !activityList.length" class="empty-text">暂无近期活动</text>

		<member-tab
			active="home"
			home-url="/pages/member/home/home"
			contacts-url="/pages/member/contact/contact"
		/>
	</view>
</template>

<script setup lang="js">
import { onMounted, ref } from 'vue'
import MemberSafeHeader from '../../../component/MemberSafeHeader.vue'
import MemberTab from '../../../component/member_Tab.vue'
import { getMemberBanners } from '../../../utils/memberMock'
import { getPublishedActivities } from '../../../api/activity'
import { openNoticePicker } from '../../../api/notice'

const toSvgDataUri = (svg) => `data:image/svg+xml;utf8,${encodeURIComponent(svg)}`

const searchIconSrc = toSvgDataUri(
	'<svg width="19" height="19" viewBox="0 0 19 19" fill="none" xmlns="http://www.w3.org/2000/svg"><path d="M8.24982 15.583C12.2998 15.583 15.583 12.2998 15.583 8.24981C15.583 4.19982 12.2998 0.916645 8.24982 0.916645C4.19982 0.916645 0.916649 4.19982 0.916649 8.24981C0.916649 12.2998 4.19982 15.583 8.24982 15.583Z" stroke="#888888" stroke-width="1.83329" stroke-linecap="round" stroke-linejoin="round"/><path d="M17.4163 17.4163L13.4747 13.4747" stroke="#888888" stroke-width="1.83329" stroke-linecap="round" stroke-linejoin="round"/></svg>'
)

const bellIconSrc = toSvgDataUri(
	'<svg width="19" height="21" viewBox="0 0 19 21" fill="none" xmlns="http://www.w3.org/2000/svg"><path d="M7.57816 18.3329C7.73907 18.6116 7.97049 18.843 8.24918 19.0039C8.52787 19.1648 8.84399 19.2495 9.16579 19.2495C9.48758 19.2495 9.80371 19.1648 10.0824 19.0039C10.3611 18.843 10.5925 18.6116 10.7534 18.3329" stroke="#888888" stroke-width="1.83329" stroke-linecap="round" stroke-linejoin="round"/><path d="M1.15613 13.1319C1.03639 13.2631 0.957362 13.4263 0.928672 13.6017C0.899982 13.777 0.922864 13.9569 0.994534 14.1195C1.0662 14.282 1.18357 14.4203 1.33236 14.5174C1.48115 14.6144 1.65495 14.6662 1.83262 14.6663H16.499C16.6766 14.6664 16.8504 14.6148 16.9993 14.5179C17.1482 14.421 17.2657 14.2829 17.3376 14.1205C17.4094 13.958 17.4325 13.7782 17.4041 13.6028C17.3756 13.4275 17.2968 13.2642 17.1773 13.1328C15.9581 11.8761 14.6657 10.5405 14.6657 6.41652C14.6657 4.95786 14.0862 3.55895 13.0548 2.52752C12.0234 1.49609 10.6244 0.916645 9.16578 0.916645C7.70713 0.916645 6.30821 1.49609 5.27679 2.52752C4.24536 3.55895 3.66591 4.95786 3.66591 6.41652C3.66591 10.5405 2.37252 11.8761 1.15613 13.1319Z" stroke="#888888" stroke-width="1.83329" stroke-linecap="round" stroke-linejoin="round"/></svg>'
)

const arrowIconSrc = toSvgDataUri(
	'<svg width="7" height="11" viewBox="0 0 7 11" fill="none" xmlns="http://www.w3.org/2000/svg"><path d="M0.936661 9.92861L5.43264 5.43264L0.936661 0.936661" stroke="white" stroke-width="1.87332" stroke-linecap="round" stroke-linejoin="round"/></svg>'
)

const bannerList = ref([])
const activityList = ref([])
const loading = ref(true)

const showUnavailable = () => uni.showToast({ title: '功能即将开放', icon: 'none' })
const handleBannerClick = showUnavailable
const handleActivityClick = showUnavailable
const openNotices = async () => { try { await openNoticePicker() } catch (error) { uni.showToast({ title: error?.message || '通知加载失败', icon: 'none' }) } }

onMounted(async () => {
	try {
		bannerList.value = await getMemberBanners()
		activityList.value = (await getPublishedActivities()).map((item) => ({ id: item.activityId, title: item.title, subtitle: item.locationDesc || '活动详情待公布' }))
	} catch (error) {
		uni.showToast({ title: error?.message || '首页数据加载失败', icon: 'none' })
	} finally {
		loading.value = false
	}
})
</script>

<style scoped>
.member-home {
	min-height: 100vh;
	background: #ffffff;
	box-sizing: border-box;
	padding: 0 46rpx calc(220rpx + env(safe-area-inset-bottom));
}

.header-icon {
	width: 38rpx;
	height: 38rpx;
	flex-shrink: 0;
}

.header-actions { display: flex; align-items: center; gap: 28rpx; }

.header-icon-bell {
	width: 38rpx;
	height: 42rpx;
}

.banner-swiper {
	width: 690rpx;
	height: 346rpx;
	margin: 0 auto;
}

.banner-card {
	position: relative;
	width: 100%;
	height: 100%;
	border-radius: 32rpx;
	overflow: hidden;
}

.banner-image {
	width: 100%;
	height: 100%;
	display: block;
	background: #e8e8e8;
}

.banner-overlay {
	position: absolute;
	left: 0;
	right: 0;
	bottom: 0;
	height: 100%;
	display: flex;
	align-items: flex-end;
	justify-content: space-between;
	padding: 0 28rpx 28rpx;
	box-sizing: border-box;
	background: linear-gradient(180deg, rgba(0, 0, 0, 0) 45%, rgba(0, 0, 0, 0.35) 100%);
}

.banner-text {
	flex: 1;
	font-family: Inter, -apple-system, sans-serif;
	font-weight: 700;
	font-size: 36rpx;
	line-height: 45rpx;
	color: #ffffff;
}

.banner-arrow-btn {
	width: 68rpx;
	height: 68rpx;
	border-radius: 50%;
	background: #ffffff33;
	display: flex;
	align-items: center;
	justify-content: center;
	flex-shrink: 0;
	margin-left: 20rpx;
}

.banner-arrow-icon {
	width: 14rpx;
	height: 22rpx;
}

.section-title {
	display: block;
	margin-top: 48rpx;
	margin-bottom: 28rpx;
	font-family: Inter, -apple-system, sans-serif;
	font-weight: 700;
	font-size: 36rpx;
	line-height: 56rpx;
	color: #333333;
}

.activity-grid {
	display: flex;
	flex-wrap: wrap;
	justify-content: space-between;
	row-gap: 32rpx;
}

.activity-card {
	width: 328rpx;
	height: 312rpx;
	box-sizing: border-box;
	border: 2rpx solid #ededed;
	border-radius: 32rpx;
	padding: 32rpx;
	background: #ffffff;
	display: flex;
	flex-direction: column;
	justify-content: space-between;
}

.activity-number {
	width: 80rpx;
	height: 80rpx;
	border-radius: 50%;
	background: #ffedd4;
	display: flex;
	align-items: center;
	justify-content: center;
}

.activity-number-text {
	font-family: Inter, -apple-system, sans-serif;
	font-weight: 700;
	font-size: 32rpx;
	line-height: 48rpx;
	color: #fdaf32;
}

.activity-info {
	display: flex;
	flex-direction: column;
	gap: 8rpx;
}

.activity-title {
	font-family: Inter, -apple-system, sans-serif;
	font-weight: 700;
	font-size: 28rpx;
	line-height: 40rpx;
	color: #333333;
}

.activity-subtitle {
	font-family: Inter, -apple-system, sans-serif;
	font-weight: 400;
	font-size: 24rpx;
	line-height: 32rpx;
	color: #888888;
}

.page-header { margin-bottom: 40rpx; }

.empty-text {
	display: block;
	padding: 80rpx 0;
	text-align: center;
	font-size: 26rpx;
	color: #999999;
}
</style>
