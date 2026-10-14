<template>
	<view class="member-contact">
		<member-safe-header class="page-header" mode="brand" title="Quanta">
			<template #actions><view class="header-actions" @click="toggleSearch"><image class="header-icon" :src="searchIconSrc" mode="aspectFit" /></view></template>
		</member-safe-header>
		<view v-if="searching" class="search-shell">
			<input v-model="keyword" class="search-input" confirm-type="search" placeholder="搜索姓名、工号或职位" focus />
			<text class="search-cancel" @click="closeSearch">取消</text>
		</view>

		<view class="batch-switcher">
			<view class="batch-arrow" @click="handlePrevBatch">
				<image class="batch-arrow-icon" :src="prevArrowIconSrc" mode="aspectFit" />
			</view>
			<text class="batch-text">{{ currentBatch }}</text>
			<view class="batch-arrow" @click="handleNextBatch">
				<image class="batch-arrow-icon" :src="nextArrowIconSrc" mode="aspectFit" />
			</view>
		</view>

		<scroll-view class="contact-list" scroll-y>
			<view
				v-for="item in filteredContactList"
				:key="item.id"
				class="contact-item"
				:class="{ 'contact-item--interactive': item.hasBusinessCard }"
				@click="handleContactClick(item)"
			>
				<view class="contact-avatar">
					<image
						v-if="item.avatar"
						class="contact-avatar-image"
						:src="item.avatar"
						mode="aspectFill"
					/>
				</view>

				<view class="contact-main">
					<view class="contact-name-row">
						<text class="contact-name">{{ item.name }}</text>
						<image v-if="item.hasBusinessCard" class="contact-badge-icon" :src="memberBadgeIconSrc" mode="aspectFit" />
					</view>
					<view class="contact-meta">
						<text class="contact-code">{{ item.code }}</text>
						<text class="contact-batch">{{ item.batch }}</text>
					</view>
				</view>

				<view class="role-tag" :style="{ backgroundColor: item.roleColor }">
					<text class="role-tag-text">{{ item.role }}</text>
				</view>
			</view>
			<text v-if="!filteredContactList.length" class="empty-text">暂无匹配成员</text>
		</scroll-view>

		<member-tab
			active="contacts"
			home-url="/pages/member/home/home"
			contacts-url="/pages/member/contact/contact"
		/>
	</view>
</template>

<script setup lang="js">
import { computed, onMounted, ref } from 'vue'
import MemberSafeHeader from '../../../component/MemberSafeHeader.vue'
import MemberTab from '../../../component/member_Tab.vue'
import { filterContacts } from '../../../utils/memberMock'
import { getMemberDirectory } from '../../../api/member'

const toSvgDataUri = (svg) => `data:image/svg+xml;utf8,${encodeURIComponent(svg)}`

const searchIconSrc = toSvgDataUri(
	'<svg width="19" height="19" viewBox="0 0 19 19" fill="none" xmlns="http://www.w3.org/2000/svg"><path d="M8.24982 15.583C12.2998 15.583 15.583 12.2998 15.583 8.24981C15.583 4.19982 12.2998 0.916645 8.24982 0.916645C4.19982 0.916645 0.916649 4.19982 0.916649 8.24981C0.916649 12.2998 4.19982 15.583 8.24982 15.583Z" stroke="#888888" stroke-width="1.83329" stroke-linecap="round" stroke-linejoin="round"/><path d="M17.4163 17.4163L13.4747 13.4747" stroke="#888888" stroke-width="1.83329" stroke-linecap="round" stroke-linejoin="round"/></svg>'
)

const prevArrowIconSrc = toSvgDataUri(
	'<svg width="7" height="12" viewBox="0 0 7 12" fill="none" xmlns="http://www.w3.org/2000/svg"><path d="M5.83093 10.8288L0.833008 5.83091L5.83093 0.832987" stroke="#888888" stroke-width="1.66597" stroke-linecap="round" stroke-linejoin="round"/></svg>'
)

const nextArrowIconSrc = toSvgDataUri(
	'<svg width="7" height="12" viewBox="0 0 7 12" fill="none" xmlns="http://www.w3.org/2000/svg"><path d="M0.833008 10.8288L5.83093 5.83091L0.833008 0.832987" stroke="#888888" stroke-width="1.66597" stroke-linecap="round" stroke-linejoin="round"/></svg>'
)

const memberBadgeIconSrc = '/static/icon/member/card-badge.svg'

const batchList = ref([])
const batchIndex = ref(0)
const contactList = ref([])
const searching = ref(false)
const keyword = ref('')

const currentBatch = computed(() => batchList.value[batchIndex.value] || '')

const filteredContactList = computed(() =>
	filterContacts(contactList.value, currentBatch.value, keyword.value)
)

const handlePrevBatch = () => {
	if (batchIndex.value > 0) {
		batchIndex.value -= 1
	}
}

const handleNextBatch = () => {
	if (batchIndex.value < batchList.value.length - 1) {
		batchIndex.value += 1
	}
}

const toggleSearch = () => { searching.value = !searching.value }
const closeSearch = () => { searching.value = false; keyword.value = '' }
const handleContactClick = (item) => {
	if (!item.hasBusinessCard) return
	uni.navigateTo({ url: `/pages/member/business-card/view?id=${encodeURIComponent(item.code)}` })
}

onMounted(async () => {
	try {
		contactList.value = await getMemberDirectory()
		batchList.value = [...new Set(contactList.value.map((item) => item.batch))].sort((left, right) => right.localeCompare(left, undefined, { numeric: true }))
		batchIndex.value = 0
	} catch (error) {
		uni.showToast({ title: error?.message || '通讯录加载失败', icon: 'none' })
	}
})
</script>

<style scoped>
.member-contact {
	min-height: 100vh;
	background: #ffffff;
	box-sizing: border-box;
	padding: 0 46rpx;
	display: flex;
	flex-direction: column;
}

.header-actions {
	display: flex;
	align-items: center;
}

.header-icon {
	width: 38rpx;
	height: 38rpx;
	flex-shrink: 0;
}

.page-header { margin-bottom: 36rpx; }

.search-shell {
	height: 76rpx;
	margin: -10rpx 0 24rpx;
	padding: 0 24rpx;
	border-radius: 24rpx;
	background: #f5f5f5;
	display: flex;
	align-items: center;
	gap: 20rpx;
}

.search-input { flex: 1; font-size: 26rpx; color: #333333; }
.search-cancel { font-size: 24rpx; color: #fd9f24; }

.batch-switcher {
	display: flex;
	align-items: center;
	justify-content: center;
	gap: 40rpx;
	margin-bottom: 36rpx;
	flex-shrink: 0;
}

.batch-arrow {
	width: 48rpx;
	height: 48rpx;
	display: flex;
	align-items: center;
	justify-content: center;
}

.batch-arrow-icon {
	width: 14rpx;
	height: 24rpx;
}

.batch-text {
	font-family: Inter, -apple-system, sans-serif;
	font-weight: 700;
	font-size: 40rpx;
	line-height: 56rpx;
	letter-spacing: -1rpx;
	text-align: center;
	color: #333333;
}

.contact-list {
	flex: 1;
	height: 0;
	padding-bottom: calc(220rpx + env(safe-area-inset-bottom));
	box-sizing: border-box;
}

.contact-item {
	display: flex;
	align-items: center;
	padding: 28rpx 0;
	border-bottom: 2rpx solid #f0f0f0;
}

.contact-item:last-child {
	border-bottom: none;
}

.contact-item--interactive { cursor: pointer; }

.contact-avatar {
	width: 96rpx;
	height: 96rpx;
	border-radius: 50%;
	border: 2rpx solid #ededed;
	background: #f5f5f5;
	overflow: hidden;
	flex-shrink: 0;
}

.contact-avatar-image {
	width: 100%;
	height: 100%;
	display: block;
}

.contact-main {
	flex: 1;
	min-width: 0;
	margin-left: 24rpx;
	margin-right: 16rpx;
}

.contact-name-row {
	display: flex;
	align-items: center;
	gap: 8rpx;
}

.contact-name {
	font-family: Inter, -apple-system, sans-serif;
	font-weight: 600;
	font-size: 32rpx;
	line-height: 48rpx;
	color: #333333;
}

.contact-badge-icon {
	width: 28rpx;
	height: 28rpx;
	flex-shrink: 0;
}

.contact-meta {
	display: flex;
	align-items: center;
	gap: 12rpx;
	margin-top: 4rpx;
}

.contact-code {
	font-family: Inter, -apple-system, sans-serif;
	font-weight: 500;
	font-size: 24rpx;
	line-height: 32rpx;
	letter-spacing: 1.2rpx;
	color: #888888;
}

.contact-batch {
	font-family: Inter, -apple-system, sans-serif;
	font-weight: 500;
	font-size: 24rpx;
	line-height: 32rpx;
	letter-spacing: 1.2rpx;
	color: #fdaf32;
}

.role-tag {
	height: 49rpx;
	padding: 0 20rpx;
	border-radius: 999rpx;
	display: flex;
	align-items: center;
	justify-content: center;
	flex-shrink: 0;
}

.role-tag-text {
	font-family: Inter, -apple-system, sans-serif;
	font-weight: 500;
	font-size: 22rpx;
	line-height: 1;
	color: #ffffff;
	white-space: nowrap;
}

.empty-text { display: block; padding: 100rpx 0; text-align: center; color: #999999; font-size: 26rpx; }
</style>
