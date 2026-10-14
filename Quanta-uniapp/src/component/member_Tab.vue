<template>
	<view class="tab-layer">
		<view class="tab-white-gap" :style="whiteMaskStyle" />
		<view class="member-tab" :style="wrapperStyle">
			<view class="tab-inner">
				<view class="tab-item" @click="handleClick('home')">
					<view class="tab-icon" :class="props.active === 'home' ? 'tab-icon-active' : 'tab-icon-inactive'">
						<image class="tab-icon-image" :src="props.active === 'home' ? homeActiveIcon : homeInactiveIcon" mode="aspectFit" />
					</view>
				</view>

				<view class="tab-item" @click="handleClick('contacts')">
					<view class="tab-icon" :class="props.active === 'contacts' ? 'tab-icon-active' : 'tab-icon-inactive'">
						<image
							class="tab-icon-image tab-icon-image-contacts"
							:src="props.active === 'contacts' ? contactsActiveIcon : contactsInactiveIcon"
							mode="aspectFit"
						/>
					</view>
				</view>

				<view class="tab-item" @click="handleClick('menu')">
					<view class="tab-icon" :class="props.active === 'menu' ? 'tab-icon-active' : 'tab-icon-inactive'">
						<image
							class="tab-icon-image"
							:src="props.active === 'menu' ? menuActiveIcon : menuInactiveIcon"
							mode="aspectFit"
						/>
					</view>
				</view>

				<view class="tab-item" @click="handleClick('profile')">
					<view class="tab-icon" :class="props.active === 'profile' ? 'tab-icon-active' : 'tab-icon-inactive'">
						<image
							class="tab-icon-image tab-icon-image-profile"
							:src="props.active === 'profile' ? profileActiveIcon : profileInactiveIcon"
							mode="aspectFit"
						/>
					</view>
				</view>
			</view>
		</view>
	</view>
</template>

<script setup lang="js">
import { computed } from 'vue'

const props = defineProps({
	active: {
		type: String,
		default: 'home',
	},
	bottomOffsetRpx: {
		type: Number,
		default: 44,
	},
	autoNavigate: {
		type: Boolean,
		default: true,
	},
	homeUrl: {
		type: String,
		default: '/pages/member/home/home',
	},
	contactsUrl: {
		type: String,
		default: '/pages/member/contact/contact',
	},
	menuUrl: {
		type: String,
		default: '/pages/member/function/function',
	},
	profileUrl: {
		type: String,
		default: '/pages/member/profile/profile',
	},
})

const emit = defineEmits(['change'])

const NAV_HEIGHT_RPX = 140
const MASK_ABOVE_NAV_RPX = 50

const wrapperStyle = computed(() => ({
	bottom: `calc(${props.bottomOffsetRpx}rpx + env(safe-area-inset-bottom))`,
}))

const whiteMaskStyle = computed(() => ({
	height: `calc(${props.bottomOffsetRpx}rpx + env(safe-area-inset-bottom) + ${NAV_HEIGHT_RPX}rpx + ${MASK_ABOVE_NAV_RPX}rpx)`,
}))

const toSvgDataUri = (svg) => `data:image/svg+xml;utf8,${encodeURIComponent(svg)}`

const toActiveSvg = (svg) =>
	svg.replace(/stroke="#D9D9D9"\s+stroke-opacity="0.5"/g, 'stroke="#FFFFFF"')

const homeInactiveSvg =
	'<svg width="24" height="26" viewBox="0 0 24 26" fill="none" xmlns="http://www.w3.org/2000/svg"><path d="M15.4584 23.6243V14.291C15.4584 13.9815 15.3355 13.6848 15.1167 13.466C14.8979 13.2472 14.6011 13.1243 14.2917 13.1243H9.62503C9.31563 13.1243 9.01885 13.2472 8.80006 13.466C8.58128 13.6848 8.45837 13.9815 8.45837 14.291V23.6243" stroke="#D9D9D9" stroke-opacity="0.5" stroke-width="2.91666" stroke-linecap="round" stroke-linejoin="round"/><path d="M1.45837 10.791C1.4583 10.4516 1.53227 10.1162 1.67512 9.80835C1.818 9.50042 2.02631 9.22741 2.28555 9.00831L10.4522 2.00948C10.8734 1.65354 11.407 1.45825 11.9584 1.45825C12.5098 1.45825 13.0434 1.65354 13.4645 2.00948L21.6312 9.00831C21.8904 9.22741 22.0987 9.50042 22.2416 9.80835C22.3845 10.1162 22.4584 10.4516 22.4584 10.791V21.291C22.4584 21.9098 22.2126 22.5033 21.775 22.9408C21.3374 23.3785 20.7439 23.6243 20.1251 23.6243H3.79171C3.17287 23.6243 2.57938 23.3785 2.1418 22.9408C1.70421 22.5033 1.45837 21.9098 1.45837 21.291V10.791Z" stroke="#D9D9D9" stroke-opacity="0.5" stroke-width="2.91666" stroke-linecap="round" stroke-linejoin="round"/></svg>'

const menuInactiveSvg =
	'<svg width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg"><path d="M8.45837 1.45825H2.62504C1.98071 1.45825 1.45837 1.98059 1.45837 2.62491V8.45825C1.45837 9.10258 1.98071 9.62491 2.62504 9.62491H8.45837C9.1027 9.62491 9.62504 9.10258 9.62504 8.45825V2.62491C9.62504 1.98059 9.1027 1.45825 8.45837 1.45825Z" stroke="#D9D9D9" stroke-opacity="0.5" stroke-width="2.91667" stroke-linecap="round" stroke-linejoin="round"/><path d="M21.2917 1.45825H15.4583C14.814 1.45825 14.2917 1.98059 14.2917 2.62491V8.45825C14.2917 9.10258 14.814 9.62491 15.4583 9.62491H21.2917C21.936 9.62491 22.4583 9.10258 22.4583 8.45825V2.62491C22.4583 1.98059 21.936 1.45825 21.2917 1.45825Z" stroke="#D9D9D9" stroke-opacity="0.5" stroke-width="2.91667" stroke-linecap="round" stroke-linejoin="round"/><path d="M21.2917 14.2915H15.4583C14.814 14.2915 14.2917 14.8138 14.2917 15.4581V21.2915C14.2917 21.9358 14.814 22.4581 15.4583 22.4581H21.2917C21.936 22.4581 22.4583 21.9358 22.4583 21.2915V15.4581C22.4583 14.8138 21.936 14.2915 21.2917 14.2915Z" stroke="#D9D9D9" stroke-opacity="0.5" stroke-width="2.91667" stroke-linecap="round" stroke-linejoin="round"/><path d="M8.45837 14.2915H2.62504C1.98071 14.2915 1.45837 14.8138 1.45837 15.4581V21.2915C1.45837 21.9358 1.98071 22.4581 2.62504 22.4581H8.45837C9.1027 22.4581 9.62504 21.9358 9.62504 21.2915V15.4581C9.62504 14.8138 9.1027 14.2915 8.45837 14.2915Z" stroke="#D9D9D9" stroke-opacity="0.5" stroke-width="2.91667" stroke-linecap="round" stroke-linejoin="round"/></svg>'

const contactsInactiveSvg =
	'<svg width="28" height="28" viewBox="0 0 28 28" fill="none" xmlns="http://www.w3.org/2000/svg"><path d="M18.6666 24.4999V22.1667C18.6666 20.929 18.175 19.742 17.2998 18.8668C16.4246 17.9917 15.2376 17.4999 14 17.4999H7C5.76233 17.4999 4.57533 17.9917 3.70017 18.8668C2.825 19.742 2.33334 20.929 2.33334 22.1667V24.4999" stroke="#D9D9D9" stroke-opacity="0.5" stroke-width="2.91667" stroke-linecap="round" stroke-linejoin="round"/><path d="M10.5 12.8333C13.0774 12.8333 15.1667 10.744 15.1667 8.16666C15.1667 5.58933 13.0774 3.5 10.5 3.5C7.92267 3.5 5.83334 5.58933 5.83334 8.16666C5.83334 10.744 7.92267 12.8333 10.5 12.8333Z" stroke="#D9D9D9" stroke-opacity="0.5" stroke-width="2.91667" stroke-linecap="round" stroke-linejoin="round"/><path d="M25.6666 24.5001V22.1667C25.6659 21.1327 25.3217 20.1283 24.6882 19.311C24.0548 18.4939 23.1679 17.9102 22.1667 17.6517" stroke="#D9D9D9" stroke-opacity="0.5" stroke-width="2.91667" stroke-linecap="round" stroke-linejoin="round"/><path d="M18.6666 3.65167C19.6705 3.90869 20.5602 4.49249 21.1956 5.31103C21.8309 6.12957 22.1758 7.1363 22.1758 8.1725C22.1758 9.2087 21.8309 10.2154 21.1956 11.034C20.5602 11.8526 19.6705 12.4364 18.6666 12.6934" stroke="#D9D9D9" stroke-opacity="0.5" stroke-width="2.91667" stroke-linecap="round" stroke-linejoin="round"/></svg>'

const profileInactiveSvg =
	'<svg width="29" height="29" viewBox="0 0 29 29" fill="none" xmlns="http://www.w3.org/2000/svg"><path d="M22.1376 24.7042C22.1376 22.662 21.3263 20.7034 19.8824 19.2595C18.4383 17.8154 16.4797 17.0042 14.4376 17.0042C12.3955 17.0042 10.4369 17.8154 8.99289 19.2595C7.54886 20.7034 6.73761 22.662 6.73761 24.7042" stroke="#D9D9D9" stroke-opacity="0.5" stroke-width="3.20834" stroke-linecap="round" stroke-linejoin="round"/><path d="M14.4375 17.0042C17.2726 17.0042 19.5708 14.7059 19.5708 11.8708C19.5708 9.03575 17.2726 6.73749 14.4375 6.73749C11.6025 6.73749 9.30419 9.03575 9.30419 11.8708C9.30419 14.7059 11.6025 17.0042 14.4375 17.0042Z" stroke="#D9D9D9" stroke-opacity="0.5" stroke-width="3.20834" stroke-linecap="round" stroke-linejoin="round"/><path d="M14.4375 27.2708C21.5251 27.2708 27.2708 21.5251 27.2708 14.4375C27.2708 7.34986 21.5251 1.60419 14.4375 1.60419C7.34983 1.60419 1.60416 7.34986 1.60416 14.4375C1.60416 21.5251 7.34983 27.2708 14.4375 27.2708Z" stroke="#D9D9D9" stroke-opacity="0.5" stroke-width="3.20834" stroke-linecap="round" stroke-linejoin="round"/></svg>'

const homeActiveIcon = toSvgDataUri(toActiveSvg(homeInactiveSvg))
const homeInactiveIcon = toSvgDataUri(homeInactiveSvg)
const menuActiveIcon = toSvgDataUri(toActiveSvg(menuInactiveSvg))
const menuInactiveIcon = toSvgDataUri(menuInactiveSvg)
const contactsActiveIcon = toSvgDataUri(toActiveSvg(contactsInactiveSvg))
const contactsInactiveIcon = toSvgDataUri(contactsInactiveSvg)
const profileActiveIcon = toSvgDataUri(toActiveSvg(profileInactiveSvg))
const profileInactiveIcon = toSvgDataUri(profileInactiveSvg)

const urlMap = {
	home: props.homeUrl,
	contacts: props.contactsUrl,
	menu: props.menuUrl,
	profile: props.profileUrl,
}

const handleClick = (key) => {
	if (key === props.active) return

	emit('change', key)
	if (!props.autoNavigate) return

	const url = urlMap[key]
	if (!url) return

	uni.reLaunch({ url })
}
</script>

<style scoped>
.tab-layer {
	position: fixed;
	left: 0;
	right: 0;
	bottom: 0;
	z-index: 10;
	pointer-events: none;
}

.tab-white-gap {
	position: fixed;
	left: 0;
	right: 0;
	bottom: 0;
	background: #ffffff;
	z-index: 9;
	pointer-events: none;
}

.member-tab {
	position: fixed;
	left: 50%;
	transform: translateX(-50%);
	width: 680rpx;
	height: 140rpx;
	border-radius: 33554400px;
	background: #ffffff1a;
	backdrop-filter: blur(128px);
	-webkit-backdrop-filter: blur(128px);
	box-shadow: 0 12rpx 40rpx rgba(0, 0, 0, 0.12);
	display: flex;
	align-items: center;
	justify-content: center;
	z-index: 10;
	pointer-events: auto;
}

.tab-inner {
	width: 100%;
	height: 100%;
	display: flex;
	align-items: center;
	justify-content: space-between;
	padding: 0 48rpx;
	box-sizing: border-box;
}

.tab-item {
	flex: 1;
	height: 100%;
	display: flex;
	align-items: center;
	justify-content: center;
}

.tab-icon {
	width: 100rpx;
	height: 100rpx;
	border-radius: 50%;
	display: flex;
	align-items: center;
	justify-content: center;
}

.tab-icon-active {
	background: #fdaf32;
	box-shadow: 0 0 40rpx rgba(253, 175, 50, 0.55);
}

.tab-icon-inactive {
	background: transparent;
}

.tab-icon-image {
	width: 54rpx;
	height: 54rpx;
}

.tab-icon-image-contacts {
	width: 56rpx;
	height: 56rpx;
}

.tab-icon-image-profile {
	width: 58rpx;
	height: 58rpx;
}
</style>
