<script setup>
import { computed, onBeforeUnmount, ref, watch } from 'vue'
const props = defineProps({ modelValue: { type: Boolean, default: false }, department: { type: String, default: '' }, options: { type: Array, default: () => [] }, preview: { type: Object, default: () => ({}) }, cachedFile: { type: Object, default: null }, loading: { type: Boolean, default: false }, submitting: { type: Boolean, default: false } })
const emit = defineEmits(['update:modelValue', 'update:department', 'update:cachedFile', 'submit'])
const visible = computed({ get: () => props.modelValue, set: (value) => emit('update:modelValue', value) })
const file = ref(null)
const error = ref('')
const previewUrl = ref('')
const requiresQr = computed(() => Boolean(props.department || props.preview.offeredDepartment || props.preview.result === 'PASS'))
const canSendDepartment = computed(() => !props.options.length || props.options.find((option) => option.value === props.department)?.status === 'UNSENT')
function revokePreviewUrl() {
  if (previewUrl.value) URL.revokeObjectURL(previewUrl.value)
  previewUrl.value = ''
}
watch(file, (value) => {
  revokePreviewUrl()
  if (value) previewUrl.value = URL.createObjectURL(value)
}, { immediate: true })
watch(() => props.modelValue, (value) => { if (value) { file.value = props.cachedFile; error.value = '' } }, { immediate: true })
watch(() => props.cachedFile, (value) => { if (props.modelValue && value !== file.value) { file.value = value; error.value = '' } })
function selectFile(event) {
  const selected = event.target.files?.[0] || null
  if (!selected) return
  if (!['image/jpeg', 'image/png'].includes(selected.type)) error.value = '仅支持 JPG、JPEG、PNG 图片'
  else if (selected.size > 5 * 1024 * 1024) error.value = '二维码图片不能超过 5 MB'
  else { file.value = selected; error.value = ''; emit('update:cachedFile', selected) }
}
function clearFile() { file.value = null; error.value = ''; emit('update:cachedFile', null) }
function submit() { if (props.loading || props.submitting || !canSendDepartment.value) return; if (requiresQr.value && !file.value) { error.value = '录用邮件必须添加群二维码'; return }; emit('submit', file.value) }
onBeforeUnmount(revokePreviewUrl)
</script>
<template>
  <ElDialog v-model="visible" width="620" class="quanta-dialog notice-dialog" destroy-on-close>
    <template #header><strong>发送结果邮件</strong></template>
    <div v-loading="loading" class="notice-dialog__body">
      <div v-if="options.length" class="notice-dialog__departments" aria-label="录用部门">
        <button v-for="option in options" :key="option.value" data-test="notice-department" type="button" :class="{ 'is-active': option.value === department }" :disabled="option.status !== 'UNSENT' || loading || submitting" @click="emit('update:department', option.value)">
          {{ option.label }} <small v-if="option.status === 'SENT'">邮件已发送</small><small v-else-if="option.status !== 'UNSENT'">状态待同步</small>
        </button>
      </div>
      <dl><div><dt>收件人</dt><dd>{{ preview.recipientName || '—' }} · {{ preview.email || '—' }}</dd></div><div><dt>录用部门</dt><dd>{{ preview.departmentLabel || preview.offeredDepartmentLabel || preview.resultLabel || '—' }}</dd></div><div><dt>邮件主题</dt><dd>{{ preview.subject || '—' }}</dd></div></dl>
      <div class="notice-dialog__preview" aria-label="邮件模板" v-html="loading ? '' : preview.content || ''"></div>
      <div v-if="requiresQr" class="notice-dialog__upload">
        <span>群二维码</span>
        <div v-if="file" class="notice-dialog__selected-file">
          <img data-test="qr-preview" :src="previewUrl" alt="群二维码预览">
          <div><strong>{{ file.name }}</strong><small>本页面内会自动复用于同部门的后续邮件</small></div>
          <div class="notice-dialog__file-actions">
            <label class="notice-dialog__replace">更换图片<input type="file" accept=".jpg,.jpeg,.png,image/jpeg,image/png" @change="selectFile"></label>
            <button data-test="clear-qr-cache" type="button" @click="clearFile">清除缓存</button>
          </div>
        </div>
        <label v-else class="notice-dialog__picker">选择二维码图片<input type="file" accept=".jpg,.jpeg,.png,image/jpeg,image/png" @change="selectFile"></label>
        <small>将显示在邮件中，并作为附件备份。支持 JPG、JPEG、PNG，最大 5 MB</small>
      </div>
      <p v-if="error" class="notice-dialog__error" role="alert">{{ error }}</p>
    </div>
    <template #footer><ElButton :disabled="submitting" @click="visible = false">取消</ElButton><ElButton data-test="send-notice" type="primary" class="notice-dialog__submit" :loading="submitting" :disabled="loading || submitting || !canSendDepartment" @click="submit">确认发送</ElButton></template>
  </ElDialog>
</template>
