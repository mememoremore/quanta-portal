<script setup>
import { computed, inject, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { onBeforeRouteLeave, routeLocationKey } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import { useMaterialUploadQueue } from '@/composables/useMaterialUploadQueue'
import { MATERIAL_CATEGORIES, MATERIAL_EXTENSIONS, materialCategoryLabel } from '@/utils/material-categories'
import { MATERIAL_TYPES, materialTypeLabel } from '@/utils/material-types'
import MaterialFileIcon from './MaterialFileIcon.vue'

const props = defineProps({ modelValue: Boolean, policy: { type: Object, required: true }, initialCategory: { type: String, default: '' }, initialType: { type: String, default: '' } })
const emit = defineEmits(['update:modelValue', 'uploaded'])
const { files, uploading, feedback, pending, add, remove, confirm } = useMaterialUploadQueue()
const category = ref(''), type = ref('TRAINING'), uploadRef = ref(null)
const selectedCategory = computed(() => props.policy.canChoose ? category.value : props.policy.category)
const stateLabels = { waiting: '待上传', uploading: '上传中', success: '已上传', error: '上传失败' }
const retrying = computed(() => pending.value.length && pending.value.every(file => file.status === 'error'))
const targetLocked = computed(() => uploading.value || files.value.some(file => file.status !== 'waiting'))
const inheritedTarget = computed(() => Boolean(props.initialCategory) && selectedCategory.value === props.initialCategory && type.value === (props.initialType || 'TRAINING'))

watch(() => props.modelValue, open => {
  if (!open || pending.value.length || uploading.value) return
  if (files.value.length) { files.value = []; uploadRef.value?.clearFiles() }
  category.value = props.policy.canChoose ? props.initialCategory || '' : props.policy.category
  type.value = props.initialType || 'TRAINING'
  feedback.value = ''
}, { immediate: true })

function selectFile(uploadFile) {
  if (!uploadFile.raw) return
  const error = add(uploadFile.raw)
  if (error) { ElMessage.warning(error); uploadRef.value?.handleRemove(uploadFile) }
}

function removeFile(file) {
  remove(file.id)
  uploadRef.value?.handleRemove({ uid: file.raw.uid })
}

function close() { if (!uploading.value) emit('update:modelValue', false) }
function size(bytes) { return bytes < 1048576 ? `${Math.max(1, Math.ceil(bytes / 1024))} KB` : `${(bytes / 1048576).toFixed(1)} MB` }

async function upload() {
  if (!props.policy.allowed || !selectedCategory.value) return
  const result = await confirm({ category: selectedCategory.value, type: type.value })
  if (result?.success) emit('uploaded', result)
}

function beforeUnload(event) {
  if (!uploading.value && !pending.value.length) return
  event.preventDefault()
  event.returnValue = ''
}
onMounted(() => window.addEventListener('beforeunload', beforeUnload))
onBeforeUnmount(() => window.removeEventListener('beforeunload', beforeUnload))
if (inject(routeLocationKey, null)) onBeforeRouteLeave(async () => {
  if (uploading.value) { ElMessage.warning('资料正在上传，请等待完成后离开'); return false }
  if (!pending.value.length) return true
  try {
    await ElMessageBox.confirm('还有文件尚未上传，离开会丢失待上传队列。是否离开？', '待上传资料', { confirmButtonText: '离开页面', cancelButtonText: '继续上传', type: 'warning' })
    return true
  } catch { return false }
})
</script>

<template>
  <ElDrawer class="material-upload-drawer" :model-value="modelValue" title="上传学习资料" size="min(440px, 100vw)" :close-on-click-modal="!uploading" :close-on-press-escape="!uploading" :show-close="!uploading" @update:model-value="close">
    <div class="material-upload-drawer__fields">
      <label for="material-upload-category">所属部门</label>
      <ElSelect v-if="policy.canChoose" id="material-upload-category" v-model="category" :disabled="targetLocked" placeholder="选择所属部门" aria-label="上传资料所属部门"><ElOption v-for="item in MATERIAL_CATEGORIES" :key="item.value" :value="item.value" :label="item.label" /></ElSelect>
      <div v-else class="material-upload-drawer__department">{{ materialCategoryLabel(selectedCategory) }}部<span>本部门</span></div>
      <span class="material-upload-drawer__label">资料类型</span>
      <div class="material-upload-drawer__types" role="group" aria-label="上传资料类型"><button v-for="item in MATERIAL_TYPES" :key="item.value" type="button" :aria-pressed="type === item.value" :disabled="targetLocked" @click="type = item.value">{{ item.label }}</button></div>
      <p v-if="inheritedTarget" class="material-upload-drawer__note">{{ initialType ? '已沿用当前页面的部门与类型。' : '已带入当前部门，请确认本次上传的资料类型。' }}</p>
      <ElUpload ref="uploadRef" multiple drag action="#" :auto-upload="false" :show-file-list="false" :limit="20" :on-change="selectFile" :on-exceed="() => ElMessage.warning('一次最多选择 20 个文件')" :disabled="uploading || !selectedCategory" :accept="MATERIAL_EXTENSIONS.map(item => `.${item}`).join(',')">
        <ElIcon><UploadFilled /></ElIcon><strong>点击选择，或拖拽文件到这里</strong><span>PDF、Word、PPT、Excel、TXT、Markdown、图片</span><small>最多 20 个文件 · 单个文件不超过 20 MB</small>
      </ElUpload>
      <div class="material-upload-drawer__queue-heading">上传队列 <span>{{ files.length }}</span></div>
      <div v-if="!files.length" class="material-upload-drawer__empty">还未选择文件，选择后点击下方确认上传。</div>
      <div v-for="file in files" :key="file.id" class="material-upload-drawer__file" data-testid="material-upload-item">
        <MaterialFileIcon :name="file.name" />
        <div class="material-upload-drawer__file-info"><strong>{{ file.name }}</strong><span>{{ size(file.size) }} · <span class="material-upload-drawer__status" :class="`is-${file.status}`">{{ stateLabels[file.status] }}{{ file.status === 'uploading' ? ` ${file.progress}%` : '' }}</span></span><ElProgress v-if="file.status === 'uploading'" :percentage="file.progress" :stroke-width="4" :show-text="false" color="#ff6700" :aria-label="`${file.name}上传进度`" /><small v-if="file.error" role="alert">{{ file.error }}</small></div>
        <button v-if="file.status !== 'success'" type="button" :disabled="uploading" @click="removeFile(file)">移除</button>
      </div>
      <p class="material-upload-drawer__note">上传至 {{ materialCategoryLabel(selectedCategory) || '待选部门' }} · {{ materialTypeLabel(type) }}。资料对全部塔员可见；失败文件可单独重试。</p>
    </div>
    <template #footer>
      <p class="material-upload-drawer__feedback" role="status">{{ feedback || `已选择 ${files.length} 个文件` }}</p>
      <div class="material-upload-drawer__actions"><ElButton :disabled="uploading" @click="close">{{ pending.length ? '稍后上传' : '关闭' }}</ElButton><ElButton type="primary" data-testid="confirm-material-upload" :loading="uploading" :disabled="!pending.length || !selectedCategory" @click="upload">{{ retrying ? '重试失败文件' : '确认上传' }}</ElButton></div>
    </template>
  </ElDrawer>
</template>

<style>
.material-upload-drawer.el-drawer { --el-drawer-padding-primary: 24px; max-width: 100vw; }
.material-upload-drawer .el-drawer__header { margin-bottom: 0; padding-bottom: 20px; border-bottom: 1px solid #e9ecf1; color: #252a32; font-weight: 650; }
.material-upload-drawer .el-drawer__body { overflow-y: auto; }
.material-upload-drawer__fields { display: grid; gap: 13px; }
.material-upload-drawer__fields > label, .material-upload-drawer__label { font-size: 12px; font-weight: 650; color: #303640; }
.material-upload-drawer__label { margin-top: 6px; }
.material-upload-drawer__department { display: flex; padding: 13px; border: 1px solid #e9ecf1; border-radius: 9px; background: #fafbfc; justify-content: space-between; font-size: 12px; }
.material-upload-drawer__department span { color: #84909f; }
.material-upload-drawer__types { display: flex; gap: 7px; }
.material-upload-drawer__types button { min-width: 0; padding: 10px 3px; border: 1px solid #e9ecf1; border-radius: 8px; background: white; color: #778292; font: inherit; font-size: 12px; flex: 1; cursor: pointer; }
.material-upload-drawer__types button[aria-pressed=true] { border-color: #ffcea9; background: #fff1e7; color: #e95d00; font-weight: 650; }
.material-upload-drawer__types button:focus-visible { outline: 2px solid #ff6700; outline-offset: 2px; }
.material-upload-drawer__types button:disabled { cursor: not-allowed; }
.material-upload-drawer .el-upload { display: block; margin-block: 5px; }
.material-upload-drawer .el-upload-dragger { display: flex; min-height: 170px; padding: 21px 10px; border: 1px dashed #f3b683; border-radius: 12px; background: #fff7f0; align-items: center; justify-content: center; flex-direction: column; gap: 9px; }
.material-upload-drawer .el-upload-dragger .el-icon { padding: 11px; margin-bottom: 4px; border-radius: 12px; background: white; color: #ff6700; font-size: 25px; box-sizing: content-box; }
.material-upload-drawer .el-upload-dragger strong { font-size: 12px; color: #343b46; }
.material-upload-drawer .el-upload-dragger span, .material-upload-drawer .el-upload-dragger small { font-size: 11px; color: #84909f; }
.material-upload-drawer__queue-heading { font-size: 12px; font-weight: 650; }
.material-upload-drawer__queue-heading > span { margin-left: 5px; color: #84909f; font-weight: 400; }
.material-upload-drawer__empty, .material-upload-drawer__note, .material-upload-drawer__feedback { margin: 0; color: #84909f; font-size: 12px; line-height: 1.7; overflow-wrap: anywhere; }
.material-upload-drawer__file { display: flex; min-width: 0; padding: 12px; border: 1px solid #e9ecf1; border-radius: 10px; background: #fafbfc; align-items: center; gap: 10px; }
.material-upload-drawer__file-info { display: grid; min-width: 0; gap: 5px; flex: 1; }
.material-upload-drawer__file-info strong { color: #303640; font-size: 12px; font-weight: 600; overflow-wrap: anywhere; }
.material-upload-drawer__file-info > span { color: #84909f; font-size: 11px; }
.material-upload-drawer__file-info small { color: #bf4335; font-size: 11px; }
.material-upload-drawer__file > button { padding: 5px; border: 0; background: transparent; color: #7c8796; font-size: 11px; cursor: pointer; }
.material-upload-drawer__status::before { display: inline-block; width: 7px; height: 7px; margin-right: 5px; border-radius: 50%; background: #22b46e; content: ''; }
.material-upload-drawer__status.is-error::before { background: #db594e; }
.material-upload-drawer__status.is-uploading::before { background: #ff8a36; }
.material-upload-drawer .el-drawer__footer { border-top: 1px solid #e9ecf1; }
.material-upload-drawer__feedback { margin-bottom: 13px; text-align: left; }
.material-upload-drawer__actions { display: flex; justify-content: flex-end; gap: 10px; }
.material-upload-drawer__actions .el-button { border-radius: 9px; }
.material-upload-drawer__actions .el-button--primary { --el-button-bg-color: #ff6700; --el-button-border-color: #ff6700; --el-button-text-color: #fff; --el-button-disabled-text-color: #fff; --el-button-disabled-bg-color: #ffba89; --el-button-disabled-border-color: #ffba89; min-width: 120px; color: white; }
@media(max-width: 700px) {
  .material-upload-drawer.el-drawer { --el-drawer-padding-primary: 20px; width: 100vw !important; }
  .material-upload-drawer .el-drawer__footer { padding-bottom: max(20px, env(safe-area-inset-bottom)); }
  .material-upload-drawer__types button, .material-upload-drawer__file > button { min-height: 44px; }
  .material-upload-drawer__actions .el-button { min-height: 44px; }
  .material-upload-drawer__actions .el-button--primary { flex: 1; }
}
</style>
