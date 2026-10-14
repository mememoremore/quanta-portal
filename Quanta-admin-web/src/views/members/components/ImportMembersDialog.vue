<script setup>
import { ref, watch } from 'vue'
import { Download, UploadFilled } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { downloadMemberTemplate, importMembers } from '@/api/members'
import { saveBlob } from '@/utils/download'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
})

const emit = defineEmits(['update:modelValue', 'success'])
const uploadRef = ref()
const selectedFile = ref(null)
const updateSupport = ref(false)
const submitting = ref(false)

watch(
  () => props.modelValue,
  (visible) => {
    if (!visible) resetForm()
  },
)

function resetForm() {
  selectedFile.value = null
  updateSupport.value = false
  uploadRef.value?.clearFiles()
}

function onFileChange(uploadFile) {
  const file = uploadFile.raw
  const extensionValid = /\.(xlsx?|XLSX?)$/.test(file?.name || '')
  const sizeValid = (file?.size || 0) <= 10 * 1024 * 1024
  if (!extensionValid) {
    ElMessage.warning('仅支持 .xls 或 .xlsx 文件')
    uploadRef.value?.clearFiles()
    selectedFile.value = null
    return
  }
  if (!sizeValid) {
    ElMessage.warning('名单文件不能超过 10MB')
    uploadRef.value?.clearFiles()
    selectedFile.value = null
    return
  }
  selectedFile.value = file
}

async function submit() {
  if (!selectedFile.value) {
    ElMessage.warning('请先选择名单文件')
    return
  }
  submitting.value = true
  try {
    const response = await importMembers(selectedFile.value, updateSupport.value)
    ElMessage.success(response.msg || '导入成功')
    emit('success', response.data)
    emit('update:modelValue', false)
  } catch (error) {
    ElMessage.error(error.message || '导入失败')
  } finally {
    submitting.value = false
  }
}

async function downloadTemplate() {
  try {
    const result = await downloadMemberTemplate()
    saveBlob(result.blob, result.fileName)
    ElMessage.success('下载成功')
  } catch (error) {
    ElMessage.warning(error.message || '模板暂不可用')
  }
}
</script>

<template>
  <ElDialog
    :model-value="modelValue"
    title="导入成员名单"
    width="520px"
    destroy-on-close
    :close-on-click-modal="!submitting"
    @close="emit('update:modelValue', false)"
  >
    <ElUpload
      ref="uploadRef"
      drag
      action="#"
      accept=".xls,.xlsx"
      :auto-upload="false"
      :limit="1"
      :on-change="onFileChange"
    >
      <ElIcon class="import-dialog__icon"><UploadFilled /></ElIcon>
      <div class="el-upload__text">拖拽 Excel 到此处，或<em>点击选择</em></div>
      <template #tip>
        <div class="el-upload__tip">仅支持 .xls/.xlsx，文件不超过 10MB</div>
      </template>
    </ElUpload>

    <div class="import-dialog__options">
      <ElCheckbox v-model="updateSupport">覆盖已存在的成员数据</ElCheckbox>
      <ElButton text :icon="Download" @click="downloadTemplate">下载名单模板</ElButton>
    </div>

    <template #footer>
      <ElButton :disabled="submitting" @click="emit('update:modelValue', false)">取消</ElButton>
      <ElButton
        type="primary"
        class="quanta-primary-button"
        :loading="submitting"
        @click="submit"
      >
        开始导入
      </ElButton>
    </template>
  </ElDialog>
</template>

<style scoped>
.import-dialog__icon {
  margin-bottom: 12px;
  color: var(--quanta-primary);
  font-size: 48px;
}

.import-dialog__options {
  display: flex;
  margin-top: 18px;
  align-items: center;
  justify-content: space-between;
}
</style>
