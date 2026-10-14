<template>
	<view class="submit-page">
		<view v-if="!canUpdate" class="closed-banner">
			<text class="closed-title">报名已截止</text>
			<text class="closed-desc">本轮招新不再接受新投递。已报名同学仍可修改后再投递。</text>
		</view>
		<scroll-view v-else scroll-y class="form-scroll" :show-scrollbar="false">
			<view class="form-body">
				<view class="profile-row">
					<view class="photo-upload" :class="{ 'photo-upload--filled': form.photo }" @click="handleUploadPhoto">
						<image v-if="form.photo" class="photo-preview" :src="form.photo" mode="aspectFill" />
						<template v-else><view class="camera-circle"><image class="camera-icon" :src="cameraIconSrc" mode="aspectFit" /></view><text class="photo-upload-text">上传证件照</text></template>
					</view>
					<view class="basic-fields">
						<view class="inline-field"><text class="inline-label">姓名：</text><input class="inline-input" v-model="form.realName" maxlength="200" :disabled="!isEditing" /></view>
						<view class="inline-field"><text class="inline-label">性别：</text><picker class="inline-picker" mode="selector" :range="genderOptions" :disabled="!isEditing" @change="onGenderChange"><view class="picker-shell"><text class="picker-value">{{ form.gender }}</text><image class="chevron-icon" :src="chevronIconSrc" mode="aspectFit" /></view></picker></view>
						<view class="inline-field"><text class="inline-label">班级：</text><input class="inline-input" v-model="form.className" maxlength="200" :disabled="!isEditing" /></view>
					</view>
				</view>
				<view class="choice-row">
					<view class="choice-field"><text class="inline-label">一志愿：</text><picker class="choice-picker" mode="selector" :range="deptOptions" :disabled="!isEditing" @change="onFirstChoiceChange"><view class="picker-shell"><text class="picker-value">{{ form.firstChoice }}</text><image class="chevron-icon" :src="chevronIconSrc" mode="aspectFit" /></view></picker></view>
					<view class="choice-field"><text class="inline-label">二志愿：</text><picker class="choice-picker" mode="selector" :range="deptOptions" :disabled="!isEditing" @change="onSecondChoiceChange"><view class="picker-shell"><text class="picker-value">{{ form.secondChoice }}</text><image class="chevron-icon" :src="chevronIconSrc" mode="aspectFit" /></view></picker></view>
				</view>
				<view class="section-block section-block-intro"><text class="section-title">个人介绍</text><textarea class="textarea-field textarea-large" v-model="form.selfIntro" placeholder="请简单介绍一下你自己..." placeholder-class="textarea-placeholder" maxlength="200" :disabled="!isEditing" /></view>
				<view class="section-block"><text class="section-title">之前是否接触过编程</text><textarea class="textarea-field textarea-medium" v-model="form.codingExperienceDesc" placeholder="请简述你的编程经历；没有相关经历也请填写“暂无”" placeholder-class="textarea-placeholder" maxlength="200" :disabled="!isEditing" /></view>
				<view class="section-block section-block-last"><text class="section-title">对Quanta的了解</text><textarea class="textarea-field textarea-medium" v-model="form.quantaUnderstanding" placeholder="请谈谈你对 Quanta 的了解..." placeholder-class="textarea-placeholder" maxlength="200" :disabled="!isEditing" /></view>
			</view>
		</scroll-view>
		<view v-if="canUpdate" class="footer-bar"><view class="footer-actions"><view class="footer-btn btn-save" @click="handleSave"><text class="footer-btn-text">{{ isEditing ? '保存' : '编辑' }}</text></view><view class="footer-btn btn-submit" :class="{ 'btn-submit--disabled': submitted || submitting }" @click="handleSubmit"><text class="footer-btn-text">{{ submitting ? '投递中' : submitted ? '已投递' : '投递' }}</text></view></view></view>
	</view>
</template>

<script setup lang="js">
import { onMounted, reactive, ref, watch } from 'vue'
import { departmentOptions, genderOptions, createEmptyApplication } from '../../../utils/mockRecruitment'
import { getMyApplication, getMyApplicationBundle, mapApplication, mapApplyWindow, submitApplication } from '../../../api/recruitment'

const deptOptions = departmentOptions
const form = reactive(createEmptyApplication())
const isEditing = ref(true)
const submitted = ref(false)
const canUpdate = ref(false)
const dirty = ref(false)
const submitting = ref(false)
let restoring = false
const requiredFields = { photo: '请上传证件照', realName: '请填写姓名', gender: '请选择性别', className: '请填写班级', firstChoice: '请选择一志愿', secondChoice: '请选择二志愿', selfIntro: '请填写个人介绍', codingExperienceDesc: '请填写编程经历', quantaUnderstanding: '请填写对 Quanta 的了解' }

watch(form, () => { if (!restoring && isEditing.value) dirty.value = true }, { deep: true })
const cameraIconSrc = `data:image/svg+xml;utf8,${encodeURIComponent('<svg width="20" height="20" viewBox="0 0 20 20" fill="none" xmlns="http://www.w3.org/2000/svg"><path d="M12.1 3.33H7.91L5.83 5.83H3.33A1.67 1.67 0 0 0 1.67 7.5v7.5a1.67 1.67 0 0 0 1.66 1.66h13.34A1.67 1.67 0 0 0 18.33 15V7.5a1.67 1.67 0 0 0-1.66-1.67h-2.5L12.1 3.33Z" stroke="#99A1AF" stroke-width="1.67" stroke-linecap="round" stroke-linejoin="round"/><path d="M10 13.33a2.5 2.5 0 1 0 0-5 2.5 2.5 0 0 0 0 5Z" stroke="#99A1AF" stroke-width="1.67"/></svg>')}`
const chevronIconSrc = `data:image/svg+xml;utf8,${encodeURIComponent('<svg width="12" height="12" viewBox="0 0 12 12" fill="none" xmlns="http://www.w3.org/2000/svg"><path d="m3 4.5 3 3 3-3" stroke="#99A1AF" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/></svg>')}`
const onGenderChange = (event) => { form.gender = genderOptions[event.detail.value] }
const setChoice = (field, event) => { const value = deptOptions[event.detail.value]; const other = field === 'firstChoice' ? form.secondChoice : form.firstChoice; if (value === other) return uni.showToast({ title: '两个志愿不能相同', icon: 'none' }); form[field] = value }
const onFirstChoiceChange = (event) => setChoice('firstChoice', event)
const onSecondChoiceChange = (event) => setChoice('secondChoice', event)
const compressPhoto = (src) => new Promise((resolve) => { uni.compressImage({ src, quality: 80, success: (result) => resolve(result.tempFilePath), fail: () => resolve(src) }) })
const handleUploadPhoto = () => { if (!isEditing.value) return; uni.chooseImage({ count: 1, sizeType: ['compressed'], sourceType: ['album', 'camera'], success: async (result) => { form.photo = await compressPhoto(result.tempFilePaths[0]); form.photoUrl = '' } }) }
const validateForm = () => { for (const key in requiredFields) { if (!form[key]) { uni.showToast({ title: requiredFields[key], icon: 'none' }); return false } }; if (form.firstChoice === form.secondChoice) { uni.showToast({ title: '两个志愿不能相同', icon: 'none' }); return false }; return true }
const buildPayload = () => ({ ...form, codingExperience: form.codingExperienceDesc.trim() ? '1' : '0' })
const doSave = ({ silent = false } = {}) => { if (!isEditing.value) return true; if (!validateForm()) return false; isEditing.value = false; dirty.value = false; if (!silent) uni.showToast({ title: '已暂存当前编辑', icon: 'success' }); return true }
const handleSave = () => { if (isEditing.value) doSave(); else { isEditing.value = true; submitted.value = false; uni.showToast({ title: '已进入编辑状态', icon: 'none' }) } }
const handleSubmit = async () => {
	if (!canUpdate.value) {
		uni.showToast({ title: '报名已截止，暂不接受新投递', icon: 'none' })
		return
	}
	if (submitting.value || !validateForm()) return
	submitting.value = true
	try {
		await submitApplication(buildPayload())
		const saved = await getMyApplication()
		if (saved) Object.assign(form, saved)
		submitted.value = true
		isEditing.value = false
		dirty.value = false
		uni.showToast({ title: '投递成功', icon: 'success' })
	} catch (error) {
		uni.showToast({ title: error?.message || '投递失败，请稍后重试', icon: 'none' })
	} finally {
		submitting.value = false
	}
}
onMounted(async () => {
	restoring = true
	try {
		const bundle = await getMyApplicationBundle()
		const windowState = mapApplyWindow(bundle)
		canUpdate.value = windowState.canUpdate
		const saved = mapApplication(bundle || { application: null, profile: null })
		if (saved) {
			Object.assign(form, { ...createEmptyApplication(), ...saved })
			submitted.value = true
			isEditing.value = false
		} else if (!canUpdate.value) {
			isEditing.value = false
		}
	} catch (error) {
		canUpdate.value = false
		isEditing.value = false
		uni.showToast({ title: error?.message || '简历加载失败', icon: 'none' })
	} finally {
		restoring = false
		dirty.value = false
	}
})
defineExpose({ hasUnsavedChanges: () => dirty.value, doSave })
</script>

<style scoped>
.submit-page{width:100%;height:100%;position:relative;background:#fff;display:flex;flex-direction:column}.closed-banner{flex:1;display:flex;flex-direction:column;align-items:center;justify-content:center;gap:16rpx;padding:48rpx}.closed-title{font-size:36rpx;font-weight:700;color:#101828}.closed-desc{font-size:28rpx;line-height:42rpx;color:#667085;text-align:center}.form-scroll{flex:1;height:0}.form-body{padding:24rpx 24rpx calc(188rpx + env(safe-area-inset-bottom));box-sizing:border-box}.profile-row{width:calc(100% - 32rpx);margin:0 auto;display:flex;align-items:flex-start;gap:20rpx}.photo-upload{width:210rpx;height:264rpx;flex-shrink:0;border:2rpx solid #e5e7eb;border-radius:32rpx;background:#f8f9fa;display:flex;flex-direction:column;align-items:center;justify-content:center;gap:24rpx;overflow:hidden;box-sizing:border-box}.photo-upload--filled{background:#fff}.photo-preview{width:100%;height:100%}.camera-circle{width:80rpx;height:80rpx;border-radius:50%;background:#fff;display:flex;align-items:center;justify-content:center;box-shadow:0 3rpx 8rpx rgba(16,24,40,.12)}.camera-icon{width:40rpx;height:40rpx}.photo-upload-text{font-size:22rpx;color:#99a1af}.basic-fields{flex:1;height:264rpx;display:flex;flex-direction:column;justify-content:space-between;min-width:0}.inline-field{display:flex;align-items:center;min-width:0}.inline-label{flex-shrink:0;font-size:28rpx;font-weight:700;color:#1e2939}.inline-input,.picker-shell{height:72rpx;border:2rpx solid #e5e7eb;border-radius:16rpx;background:#f8f9fa;box-sizing:border-box;font-size:28rpx;color:#101828}.inline-input{flex:1;width:100%;padding:0 16rpx}.inline-picker,.choice-picker{flex:1;min-width:0}.picker-shell{display:flex;align-items:center;justify-content:space-between;padding:0 12rpx 0 16rpx}.picker-value{flex:1}.chevron-icon{width:24rpx;height:24rpx}.choice-row{width:calc(100% - 32rpx);margin:36rpx auto 0;display:flex;gap:16rpx}.choice-field{flex:1;display:flex;align-items:center;min-width:0}.section-block{width:calc(100% - 32rpx);margin:32rpx auto 0}.section-block-intro{margin-top:52rpx}.section-block-last{margin-bottom:16rpx}.section-title{display:block;margin-bottom:16rpx;font-size:30rpx;line-height:45rpx;font-weight:700;color:#101828}.textarea-field{width:100%;padding:32rpx;border:2rpx solid #e5e7eb;border-radius:24rpx;background:#f8f9fa;box-sizing:border-box;font-size:28rpx;line-height:42rpx;color:#101828}.textarea-large{height:296rpx}.textarea-medium{height:160rpx}.textarea-placeholder{color:#99a1af}.footer-bar{position:fixed;left:0;right:0;bottom:24rpx;height:calc(104rpx + env(safe-area-inset-bottom));padding-bottom:env(safe-area-inset-bottom);z-index:20}.footer-actions{width:calc(100% - 80rpx);height:104rpx;margin:0 auto;display:flex}.footer-btn{height:104rpx;display:flex;align-items:center;justify-content:center}.btn-save{flex:0 0 235rpx;background:#101828;border-radius:24rpx 0 0 24rpx}.btn-submit{flex:1;background:#ff6600;border-radius:0 24rpx 24rpx 0}.btn-submit--disabled{background:#d0d5dd}.footer-btn-text{font-size:32rpx;font-weight:600;color:#fff}
</style>
