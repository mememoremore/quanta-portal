<template>
	<view class="process-page">
		<view v-if="departments.length" class="process-layout">
			<view class="dept-sidebar">
				<view
					v-for="dept in departments"
					:key="dept.name"
					class="dept-item"
					:class="{ 'dept-item-active': activeDepartment === dept.name }"
					@click="activeDepartment = dept.name"
				>
					<view v-if="activeDepartment === dept.name" class="dept-item-indicator" />
					<text class="dept-icon">{{ dept.icon }}</text>
					<text class="dept-label" :class="{ 'dept-label-active': activeDepartment === dept.name }">{{ dept.name }}</text>
				</view>
			</view>

			<scroll-view scroll-y class="process-scroll" :show-scrollbar="false">
				<view class="process-content">
					<view class="timeline-line" />
					<view v-for="stage in currentDepartment.stages" :key="stage.key" class="stage-row">
						<view class="timeline-slot">
							<view class="timeline-dot" :class="`timeline-dot--${stage.status}`" />
						</view>
						<view class="stage-card" @click="openDetail(stage)">
							<view class="stage-card-header">
								<text class="stage-title">{{ stage.title }}</text>
								<view class="stage-arrow">›</view>
							</view>
							<text class="stage-desc">{{ stage.description }}</text>
							<view class="stage-divider" />
							<view class="stage-card-footer">
								<text class="stage-detail" :class="{ 'stage-detail--disabled': !canView(stage) }">查看详情</text>
								<view class="stage-tag"><text>{{ currentDepartment.name }}岗</text></view>
							</view>
						</view>
					</view>
					<view class="process-end-wrap">
						<view class="process-end-line" />
						<text class="process-end">END OF PROCESS</text>
					</view>
				</view>
			</scroll-view>
		</view>

		<view v-else class="empty-state">
			<view class="empty-card">
				<view class="empty-mark">
					<view class="empty-mark-line" />
					<view class="empty-mark-line empty-mark-line--short" />
				</view>
				<text class="empty-title">暂未投递志愿</text>
				<text class="empty-desc">完成简历投递后，即可在这里查看应聘进度</text>
				<view class="empty-button" @click="emit('go-submit')">
					<text>去投递简历</text><text class="empty-button-arrow">›</text>
				</view>
			</view>
		</view>

		<view v-if="detail.visible" class="modal-mask" @click="handleMaskClick" />
		<view v-if="detail.visible" class="modal-wrap">
			<view class="detail-modal" :class="`detail-modal--${detail.type}`">
				<text class="modal-title" :class="{ 'modal-title--accent': detail.accent }">{{ detail.title }}</text>
				<text v-if="detail.content" class="modal-content">{{ detail.content }}</text>
				<view v-if="detail.rows.length" class="modal-detail-list">
					<view v-for="row in detail.rows" :key="row.label" class="modal-detail-row">
						<text class="modal-detail-label">{{ row.label }}：</text>
						<text class="modal-detail-value">{{ row.value }}</text>
					</view>
				</view>
				<view v-if="detail.statusText" class="modal-status">{{ detail.statusText }}</view>

				<view v-if="detail.type === 'invitation'" class="modal-actions">
					<view class="modal-button modal-button--dark" @click="rejectSecondInterview">拒绝</view>
					<view class="modal-button modal-button--primary" @click="acceptSecondInterview">接受</view>
				</view>
				<view v-else-if="detail.type === 'conflict'" class="modal-actions">
					<view class="modal-button modal-button--dark" @click="rejectSecondInterview">拒绝</view>
					<view class="modal-button modal-button--primary" @click="closeDetail">取消</view>
				</view>
				<view v-else class="modal-actions">
					<view class="modal-button modal-button--primary modal-button--full" @click="closeDetail">
						{{ detail.type === 'status' ? '关闭' : '我知道了' }}
					</view>
				</view>
			</view>
		</view>

		<template v-if="isDev">
			<view class="debug-fab" @click="debug.visible = true">调试</view>
			<view v-if="debug.visible" class="debug-mask" @click="debug.visible = false" />
			<view v-if="debug.visible" class="debug-panel">
				<text class="debug-title">流程状态调试</text>
				<template v-if="departments.length">
					<picker mode="selector" :range="departmentNames" :value="debug.departmentIndex" @change="onDebugDepartmentChange">
						<view class="debug-picker">部门：{{ departmentNames[debug.departmentIndex] }}</view>
					</picker>
					<picker mode="selector" :range="presetLabels" :value="debug.presetIndex" @change="onDebugPresetChange">
						<view class="debug-picker">状态：{{ presetLabels[debug.presetIndex] }}</view>
					</picker>
					<view class="debug-actions">
						<view class="debug-button debug-button--ghost" @click="debug.visible = false">取消</view>
						<view class="debug-button debug-button--primary" @click="applyDebugState">应用</view>
					</view>
				</template>
				<view v-else class="debug-empty">请先完成简历投递，再切换流程状态。</view>
			</view>
		</template>
	</view>
</template>

<script setup lang="js">
import { computed, onMounted, reactive, ref } from 'vue'
import {
	applyProcessPreset,
	processPresetOptions,
} from '../../../utils/mockRecruitment'
import { getMyInterviewProcess } from '../../../api/recruitment'

const emit = defineEmits(['go-submit'])
const isDev = import.meta.env.DEV
const activeDepartment = ref('')
const departments = ref([])

const createDetailState = () => ({
	visible: false,
	type: 'message',
	stageKey: '',
	departmentName: '',
	title: '',
	content: '',
	statusText: '',
	accent: false,
	dismissible: true,
	rows: [],
})

const detail = reactive(createDetailState())
const debug = reactive({ visible: false, departmentIndex: 0, presetIndex: 0 })

const currentDepartment = computed(() =>
	departments.value.find((item) => item.name === activeDepartment.value) || { name: '', stages: [] },
)
const currentStage = () => currentDepartment.value.stages.find((stage) => stage.key === detail.stageKey)
const departmentNames = computed(() => departments.value.map((department) => department.name))
const presetLabels = processPresetOptions.map((preset) => preset.label)
const canView = (stage) => stage.status !== 'locked'

const resetDetail = () => Object.assign(detail, createDetailState())
const showDetail = (config) => Object.assign(detail, createDetailState(), config, { visible: true })

const buildScheduleRows = (stage) => {
	const source = stage.detail || {}
	return [
		{ label: '时间', value: source.interviewTime },
		{ label: '面试间', value: source.interviewRoom },
		{ label: '候场室', value: source.waitingRoom },
		{ label: '提醒', value: source.reminder },
	].filter((row) => row.value)
}

const openDetail = (stage) => {
	if (!canView(stage)) {
		uni.showToast({ title: '该环节暂未开放', icon: 'none' })
		return
	}

	const base = { stageKey: stage.key, departmentName: currentDepartment.value.name }

	if (stage.status === 'scheduled') {
		showDetail({
			...base,
			type: 'schedule',
			title: stage.key === 'first' ? '一面加油！' : '二面加油！',
			accent: true,
			rows: buildScheduleRows(stage),
		})
		return
	}

	if (stage.status === 'invited') {
		showDetail({
			...base,
			type: 'invitation',
			title: '恭喜进入二面🎉',
			accent: true,
			dismissible: false,
			rows: buildScheduleRows(stage),
		})
		return
	}

	if (stage.status === 'accepted' || stage.status === 'declined') {
		showDetail({
			...base,
			type: 'status',
			title: `${currentDepartment.value.name}岗二面邀请`,
			content: stage.status === 'accepted'
				? '请留意后续面试通知，并按时到场参与。'
				: '你已拒绝该岗位的二面邀请。',
			statusText: stage.status === 'accepted' ? '你已接受面试' : '你已拒绝面试',
			accent: stage.status === 'accepted',
		})
		return
	}

	if (stage.status === 'rejected') {
		showDetail({
			...base,
			type: 'rejected',
			title: '感恩相遇',
			content: '很遗憾，你没有通过此次筛选。此次的不如意并不代表什么，愿你往后皆顺意！',
			accent: true,
		})
		return
	}

	if (stage.status === 'offered') {
		showDetail({
			...base,
			type: 'offered',
			title: '恭喜录用！',
			content: '恭喜你获得 Quanta 的录用邀请，期待与你相遇！',
			accent: true,
		})
		return
	}

	if (stage.status === 'passed') {
		showDetail({
			...base,
			type: 'status',
			title: stage.key === 'first' ? '一面通过' : '二面通过',
			content: stage.key === 'first'
				? '恭喜你通过一面，请查看二面安排。'
				: '恭喜你通过二面，请耐心等待录用结果。',
			statusText: '该阶段已通过',
			accent: true,
		})
		return
	}

	const pendingContent = {
		first: '简历已投递，请耐心等待筛选结果。',
		second: '二面流程正在进行中，请留意后续通知。',
		offer: '录用结果尚未发布，请耐心等待。',
	}
	showDetail({ ...base, title: stage.title, content: pendingContent[stage.key] })
}

const closeDetail = () => resetDetail()
const handleMaskClick = () => { if (detail.dismissible) closeDetail() }

const persistDepartments = () => {
	departments.value = departments.value.map((department) => ({ ...department }))
}

const rejectSecondInterview = () => {
	const stage = currentStage()
	if (stage?.key !== 'second') return
	stage.status = 'declined'
	stage.detail = {}
	persistDepartments()
	closeDetail()
	uni.showToast({ title: '已拒绝二面邀请', icon: 'none' })
}

const acceptSecondInterview = () => {
	const acceptedDepartment = departments.value.find((department) =>
		department.name !== currentDepartment.value.name
		&& department.stages.find((stage) => stage.key === 'second')?.status === 'accepted',
	)

	if (acceptedDepartment) {
		showDetail({
			type: 'conflict',
			stageKey: 'second',
			departmentName: currentDepartment.value.name,
			title: '无法接受邀请',
			content: `你已接受${acceptedDepartment.name}部门的二面邀请，无法继续接受${currentDepartment.value.name}部门的面试。`,
			accent: true,
			dismissible: false,
		})
		return
	}

	const stage = currentStage()
	if (stage?.key !== 'second') return
	stage.status = 'accepted'
	persistDepartments()
	showDetail({
		type: 'status',
		stageKey: 'second',
		departmentName: currentDepartment.value.name,
		title: `${currentDepartment.value.name}岗二面邀请`,
		content: '请留意后续面试通知，并按时到场参与。',
		statusText: '你已接受面试',
		accent: true,
	})
}

const onDebugDepartmentChange = (event) => { debug.departmentIndex = Number(event.detail.value) }
const onDebugPresetChange = (event) => { debug.presetIndex = Number(event.detail.value) }

const applyDebugState = () => {
	const preset = processPresetOptions[debug.presetIndex] || processPresetOptions[0]
	if (preset.key === 'dualSecondInvited') {
		departments.value = departments.value.map((department) => applyProcessPreset(department, preset.key))
	} else {
		departments.value = departments.value.map((department, index) =>
			index === debug.departmentIndex ? applyProcessPreset(department, preset.key) : department,
		)
	}
	persistDepartments()
	activeDepartment.value = departments.value[debug.departmentIndex]?.name || departments.value[0]?.name || ''
	debug.visible = false
	uni.showToast({ title: `已切换为${preset.label}`, icon: 'none' })
}

onMounted(async () => {
	try {
		departments.value = await getMyInterviewProcess()
		activeDepartment.value = departments.value[0]?.name || ''
	} catch (error) {
		uni.showToast({ title: error?.message || '应聘流程加载失败', icon: 'none' })
	}
})
</script>

<style scoped>
.process-page{flex:1;width:100%;min-height:0;background:#f8fafc;position:relative;display:flex;flex-direction:column}.process-layout{width:100%;height:100%;display:flex}.dept-sidebar{width:152rpx;background:#fff;border-right:2rpx solid #eef2f6;flex-shrink:0}.dept-item{position:relative;height:132rpx;display:flex;flex-direction:column;align-items:center;justify-content:center;gap:10rpx;color:#6a7282}.dept-item-active{background:#fff8f5;color:#ff6600}.dept-item-indicator{position:absolute;left:0;width:4rpx;height:90rpx;background:#ff6600;border-radius:0 999rpx 999rpx 0}.dept-icon{font-size:32rpx;line-height:32rpx}.dept-label{font-size:24rpx;font-weight:500;text-align:center}.dept-label-active{color:#ff6600}.process-scroll{flex:1;height:100%}.process-content{position:relative;padding:44rpx 24rpx 80rpx 30rpx;min-height:100%;box-sizing:border-box}.timeline-line{position:absolute;left:56rpx;top:52rpx;bottom:156rpx;width:2rpx;background:#f4b08a}.stage-row{position:relative;display:flex;gap:20rpx;margin-bottom:46rpx}.timeline-slot{width:52rpx;padding-top:92rpx;display:flex;justify-content:center;flex-shrink:0}.timeline-dot{width:22rpx;height:22rpx;border:4rpx solid #ff6600;border-radius:50%;background:#fff;box-sizing:border-box;z-index:1}.timeline-dot--scheduled,.timeline-dot--passed,.timeline-dot--invited,.timeline-dot--accepted,.timeline-dot--offered{width:28rpx;height:28rpx;background:#ff6600;border:6rpx solid rgba(255,102,0,.16)}.timeline-dot--declined{border-color:#98a2b3;background:#fff}.timeline-dot--locked{border-color:#d0d5dd}.timeline-dot--rejected{border-color:#ff6600;background:#fff}.stage-card{width:446rpx;height:290rpx;padding:34rpx 28rpx 24rpx;background:#fff;border-radius:40rpx;box-shadow:0 8rpx 24rpx rgba(16,24,40,.06);box-sizing:border-box;display:flex;flex-direction:column}.stage-card-header,.stage-card-footer{display:flex;align-items:center;justify-content:space-between}.stage-title{font-size:40rpx;line-height:60rpx;font-weight:700;color:#101828}.stage-arrow{width:64rpx;height:64rpx;background:#f8f9fa;border-radius:50%;display:flex;align-items:center;justify-content:center;font-size:52rpx;font-weight:300;color:#99a1af}.stage-desc{margin-top:22rpx;font-size:26rpx;color:#667085}.stage-divider{margin-top:34rpx;border-top:2rpx solid #f9fafb}.stage-card-footer{margin-top:auto}.stage-detail{font-size:26rpx;font-weight:700;color:#ff6600}.stage-detail--disabled{color:#b7c0cf}.stage-tag{padding:7rpx 14rpx;border-radius:16rpx;background:#f7f8fa;color:#b7c0cf;font-size:22rpx}.process-end-wrap{margin-top:88rpx;display:flex;flex-direction:column;align-items:center;gap:20rpx}.process-end-line{width:96rpx;height:4rpx;background:#e5e7eb;border-radius:99rpx}.process-end{font-size:24rpx;letter-spacing:2.4rpx;color:#99a1af}.empty-state{flex:1;display:flex;flex-direction:column;align-items:center;justify-content:center;box-sizing:border-box}.empty-card{width:calc(100% - 112rpx);padding:56rpx 40rpx 44rpx;border-radius:40rpx;background:#fff;box-shadow:0 16rpx 44rpx rgba(16,24,40,.08);display:flex;flex-direction:column;align-items:center;box-sizing:border-box;transform:translateY(-96rpx)}.empty-mark{width:112rpx;height:112rpx;margin-bottom:30rpx;border-radius:50%;background:#fff3eb;position:relative;display:flex;flex-direction:column;align-items:center;justify-content:center;gap:10rpx}.empty-mark::before{content:'';width:42rpx;height:52rpx;border:4rpx solid #ff6600;border-radius:7rpx;box-sizing:border-box}.empty-mark-line{position:absolute;width:20rpx;height:3rpx;background:#ff6600;border-radius:99rpx;transform:translateY(-7rpx)}.empty-mark-line--short{width:14rpx;transform:translateY(7rpx)}.empty-title{font-size:36rpx;font-weight:700;color:#101828}.empty-desc{margin-top:14rpx;font-size:26rpx;line-height:40rpx;text-align:center;color:#667085}.empty-button{width:100%;height:92rpx;margin-top:40rpx;padding:0 32rpx;border-radius:24rpx;background:#ff6600;color:#fff;display:flex;align-items:center;justify-content:space-between;box-sizing:border-box;font-size:30rpx;font-weight:600;box-shadow:0 10rpx 20rpx rgba(255,102,0,.22)}.empty-button-arrow{font-size:48rpx;font-weight:300;line-height:1}.modal-mask{position:fixed;inset:0;background:rgba(16,24,40,.46);z-index:30}.modal-wrap{position:fixed;inset:0;z-index:31;display:flex;align-items:center;justify-content:center;padding:64rpx;pointer-events:none}.detail-modal{width:100%;padding:56rpx 44rpx 40rpx;background:#fff;border-radius:40rpx;box-sizing:border-box;pointer-events:auto}.modal-title{display:block;text-align:center;font-size:40rpx;font-weight:700;color:#101828}.modal-title--accent{color:#ff6600}.modal-content{display:block;margin-top:28rpx;font-size:30rpx;line-height:1.55;color:#1e2939;white-space:pre-wrap}.modal-detail-list{margin-top:38rpx;display:flex;flex-direction:column;gap:24rpx}.modal-detail-row{display:flex;align-items:flex-start}.modal-detail-label{width:150rpx;flex-shrink:0;font-size:30rpx;line-height:44rpx;color:#101828}.modal-detail-value{flex:1;font-size:30rpx;line-height:44rpx;color:#101828;white-space:pre-wrap}.modal-actions{display:flex;gap:20rpx;margin-top:44rpx}.modal-button{flex:1;height:84rpx;display:flex;align-items:center;justify-content:center;border-radius:20rpx;font-size:28rpx;font-weight:600}.modal-button--primary{background:#ff6600;color:#fff}.modal-button--dark{background:#101010;color:#fff}.modal-button--full{flex:1}.modal-status{margin-top:36rpx;padding:24rpx 0;border-top:2rpx solid #f2f4f7;border-bottom:2rpx solid #f2f4f7;text-align:center;font-size:28rpx;font-weight:600;color:#667085}.debug-fab{position:fixed;right:24rpx;bottom:28rpx;z-index:24;padding:14rpx 20rpx;border-radius:999rpx;background:#101828;color:#fff;font-size:22rpx;box-shadow:0 8rpx 24rpx rgba(16,24,40,.2)}.debug-mask{position:fixed;inset:0;background:rgba(16,24,40,.36);z-index:40}.debug-panel{position:fixed;left:32rpx;right:32rpx;bottom:32rpx;z-index:41;padding:36rpx;background:#fff;border-radius:28rpx;box-shadow:0 16rpx 44rpx rgba(16,24,40,.18)}.debug-title{display:block;margin-bottom:24rpx;font-size:30rpx;font-weight:700;color:#101828}.debug-picker{height:76rpx;margin-top:16rpx;padding:0 24rpx;border:2rpx solid #e5e7eb;border-radius:16rpx;background:#f8f9fa;display:flex;align-items:center;font-size:26rpx;color:#344054}.debug-actions{display:flex;gap:20rpx;margin-top:28rpx}.debug-button{flex:1;height:76rpx;border-radius:16rpx;display:flex;align-items:center;justify-content:center;font-size:26rpx;font-weight:600}.debug-button--ghost{background:#f2f4f7;color:#475467}.debug-button--primary{background:#ff6600;color:#fff}.debug-empty{padding:20rpx 0;font-size:26rpx;line-height:40rpx;color:#667085}
</style>
