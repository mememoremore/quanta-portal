export type ActivityStatus = 'DRAFT' | 'PUBLISHED' | 'CANCELED' | 'DELETED'
export type ActivitySignupStatus = 'APPLIED' | 'APPROVED' | 'CANCELED' | 'REJECTED'

export interface MockActivity {
	activityId: number
	activityType: 'TALK' | 'ELITE_SHARE'
	title: string
	scenePrefix: string
	brandName: string
	sceneSuffix: string
	description: string
	signupStart: string
	signupEnd: string
	activityStart: string
	activityEnd: string
	locationDesc: string
	capacity: number
	signupCount: number
	status: ActivityStatus
}

export interface LocalActivitySignup {
	activityId: number
	status: ActivitySignupStatus
	signupTime: string
	remark?: string
}

export interface ActivityGuest {
	guestId: number
	activityId: number
	name: string
	company: string
	position: string
	background: string
	topic: string
	avatarUrl: string
	sortOrder: number
}

export type TalkSignupStateKey =
	| 'canceled'
	| 'signed'
	| 'full'
	| 'not-started'
	| 'ended'
	| 'unavailable'
	| 'available'

export interface TalkSignupState {
	key: TalkSignupStateKey
	label: string
	enabled: boolean
	message: string
}

const LEGACY_TALK_SIGNED_KEY = 'talk_signed'
const ACTIVITY_SIGNUP_KEY_PREFIX = 'quanta_activity_signup_'

const mockTalkActivity: MockActivity = {
	activityId: 1,
	activityType: 'TALK',
	title: '广外Quanta专场宣讲会',
	scenePrefix: '广外',
	brandName: 'Quanta',
	sceneSuffix: '专场宣讲会',
	description: '现场可以免费领取小礼品，参与抽奖将有机会获得限量周边',
	signupStart: '2026-08-01T00:00:00+08:00',
	signupEnd: '2026-09-16T23:59:59+08:00',
	activityStart: '2026-09-17T19:00:00+08:00',
	activityEnd: '2026-09-17T20:00:00+08:00',
	locationDesc: '教学楼E306',
	capacity: 50,
	signupCount: 0,
	status: 'PUBLISHED'
}

const mockEliteShareActivity: MockActivity = {
	activityId: 2,
	activityType: 'ELITE_SHARE',
	title: 'Quanta 精英分享会',
	scenePrefix: '',
	brandName: 'Quanta',
	sceneSuffix: '精英分享会',
	description: '与优秀的 Quanta 师兄师姐面对面，了解真实的互联网岗位与成长路径。',
	signupStart: '2026-08-01T00:00:00+08:00',
	signupEnd: '2026-09-16T23:59:59+08:00',
	activityStart: '2026-09-17T19:00:00+08:00',
	activityEnd: '2026-09-17T20:00:00+08:00',
	locationDesc: '教学楼E306',
	capacity: 50,
	signupCount: 0,
	status: 'PUBLISHED'
}

const mockEliteShareGuests: ActivityGuest[] = [
	{
		guestId: 1,
		activityId: 2,
		name: '巫鹏浩师兄',
		company: '腾讯',
		position: '产品',
		background: 'Quanta 9th COO',
		topic: '从产品小白到专业产品经理的蝶变之路',
		avatarUrl: '',
		sortOrder: 1
	},
	{
		guestId: 2,
		activityId: 2,
		name: '王泽帆师兄',
		company: '腾讯',
		position: '后台',
		background: 'Quanta 17th CEO',
		topic: '待揭秘',
		avatarUrl: '',
		sortOrder: 2
	}
]

const state = (
	key: TalkSignupStateKey,
	label: string,
	enabled: boolean,
	message: string
): TalkSignupState => ({ key, label, enabled, message })

const isValidSignup = (value: unknown): value is LocalActivitySignup => {
	if (!value || typeof value !== 'object') return false
	const signup = value as Partial<LocalActivitySignup>
	return (
		typeof signup.activityId === 'number' &&
		typeof signup.signupTime === 'string' &&
		['APPLIED', 'APPROVED', 'CANCELED', 'REJECTED'].includes(signup.status || '')
	)
}

export const getMockTalkActivity = (): MockActivity => ({ ...mockTalkActivity })

export const getMockEliteShareActivity = (): MockActivity => ({ ...mockEliteShareActivity })

export const getMockEliteShareGuests = (): ActivityGuest[] =>
	mockEliteShareGuests
		.map((guest) => ({ ...guest }))
		.sort((left, right) => left.sortOrder - right.sortOrder)

export const getActivitySignupKey = (activityId: number) =>
	`${ACTIVITY_SIGNUP_KEY_PREFIX}${activityId}`

export const getActivitySignup = (activityId: number): LocalActivitySignup | null => {
	try {
		const cached = uni.getStorageSync(getActivitySignupKey(activityId))
		return isValidSignup(cached) && cached.activityId === activityId ? cached : null
	} catch (error) {
		console.warn('读取宣讲会报名缓存失败', error)
		return null
	}
}

export const saveActivitySignup = (activityId: number, remark = ''): LocalActivitySignup => {
	const signup: LocalActivitySignup = {
		activityId,
		status: 'APPLIED',
		signupTime: new Date().toISOString(),
		remark
	}
	uni.setStorageSync(getActivitySignupKey(activityId), signup)

	const saved = getActivitySignup(activityId)
	if (!saved) throw new Error('报名信息未能保存到本地')
	return saved
}

export const migrateLegacyTalkSignup = (activityId: number): LocalActivitySignup | null => {
	const current = getActivitySignup(activityId)
	if (current) return current

	try {
		if (uni.getStorageSync(LEGACY_TALK_SIGNED_KEY) === true) {
			const migrated = saveActivitySignup(activityId)
			uni.removeStorageSync(LEGACY_TALK_SIGNED_KEY)
			return migrated
		}
	} catch (error) {
		console.warn('迁移旧宣讲会报名缓存失败', error)
	}

	return null
}

export const getTalkSignupState = (
	activity: MockActivity,
	signup: LocalActivitySignup | null,
	now = Date.now()
): TalkSignupState => {
	if (activity.status === 'CANCELED' || activity.status === 'DELETED') {
		return state('canceled', '活动取消', false, '本场活动已取消')
	}

	if (signup?.status === 'APPLIED' || signup?.status === 'APPROVED') {
		return state('signed', '已报名', false, '你已经报名本场活动')
	}

	if (activity.signupCount >= activity.capacity) {
		return state('full', '名额已满', false, '本场活动报名名额已满')
	}

	const signupStart = Date.parse(activity.signupStart)
	const signupEnd = Date.parse(activity.signupEnd)
	if (Number.isFinite(signupStart) && now < signupStart) {
		return state('not-started', '报名未开始', false, '本场活动报名尚未开始')
	}
	if (Number.isFinite(signupEnd) && now > signupEnd) {
		return state('ended', '报名已截止', false, '本场活动报名已截止')
	}

	if (activity.status !== 'PUBLISHED') {
		return state('unavailable', '暂未开放', false, '本场活动暂未开放报名')
	}

	return state('available', '立即报名', true, '')
}

const pad = (value: number) => String(value).padStart(2, '0')

export const formatActivityTimeRange = (start: string, end: string): string => {
	const startDate = new Date(start)
	const endDate = new Date(end)
	if (Number.isNaN(startDate.getTime()) || Number.isNaN(endDate.getTime())) return ''

	const weekdays = ['日', '一', '二', '三', '四', '五', '六']
	const dateText = `${startDate.getFullYear()}年${startDate.getMonth() + 1}月${startDate.getDate()}日（周${weekdays[startDate.getDay()]}）`
	return `${dateText} ${pad(startDate.getHours())}:${pad(startDate.getMinutes())}-${pad(endDate.getHours())}:${pad(endDate.getMinutes())}`
}
