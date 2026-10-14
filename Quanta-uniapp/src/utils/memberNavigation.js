const DEFAULT_STATUS_BAR_HEIGHT = 20
const DEFAULT_NAVIGATION_HEIGHT = 44
const DEFAULT_CAPSULE_INSET_RIGHT = 104
const CAPSULE_GAP = 10

const positiveNumber = (value, fallback = 0) =>
	Number.isFinite(Number(value)) && Number(value) > 0 ? Number(value) : fallback

export const calculateMemberNavigationMetrics = (windowInfo = {}, menuRect = null) => {
	const windowWidth = positiveNumber(windowInfo.windowWidth, 375)
	const reportedStatusBar = positiveNumber(windowInfo.statusBarHeight, DEFAULT_STATUS_BAR_HEIGHT)
	const safeAreaTop = positiveNumber(windowInfo.safeArea?.top)
	const statusBarHeight = Math.max(reportedStatusBar, safeAreaTop)
	const capsuleIsValid = menuRect
		&& positiveNumber(menuRect.top)
		&& positiveNumber(menuRect.left)
		&& positiveNumber(menuRect.width)
		&& positiveNumber(menuRect.height)
		&& Number(menuRect.left) + Number(menuRect.width) <= windowWidth + 1

	if (!capsuleIsValid) {
		return {
			statusBarHeight,
			navigationHeight: DEFAULT_NAVIGATION_HEIGHT,
			totalHeight: statusBarHeight + DEFAULT_NAVIGATION_HEIGHT,
			capsuleInsetRight: DEFAULT_CAPSULE_INSET_RIGHT,
		}
	}

	const navigationHeight = Math.max(
		DEFAULT_NAVIGATION_HEIGHT,
		(Number(menuRect.top) - statusBarHeight) * 2 + Number(menuRect.height),
	)
	const capsuleInsetRight = Math.max(
		DEFAULT_CAPSULE_INSET_RIGHT,
		windowWidth - Number(menuRect.left) + CAPSULE_GAP,
	)

	return {
		statusBarHeight,
		navigationHeight,
		totalHeight: statusBarHeight + navigationHeight,
		capsuleInsetRight,
	}
}

export const getMemberNavigationMetrics = () => {
	let windowInfo = {}
	let menuRect = null

	try {
		windowInfo = uni.getWindowInfo?.() || {}
	} catch (_) {
		windowInfo = {}
	}

	try {
		menuRect = uni.getMenuButtonBoundingClientRect?.() || null
	} catch (_) {
		menuRect = null
	}

	return calculateMemberNavigationMetrics(windowInfo, menuRect)
}
