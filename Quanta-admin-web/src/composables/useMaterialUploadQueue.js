import { computed, ref } from 'vue'
import { uploadMaterial } from '@/api/materials'
import { ensureMaterialTypeFolder } from '@/api/material-folders'
import { materialFileError } from '@/utils/material-categories'

export function useMaterialUploadQueue({ upload = uploadMaterial, resolveFolder = ensureMaterialTypeFolder, onSettled = () => {} } = {}) {
  const files = ref([]), uploading = ref(false), feedback = ref('')
  const pending = computed(() => files.value.filter(file => file.status !== 'success'))
  let nextId = 0

  function add(file) {
    if (uploading.value) return '正在上传，请稍候'
    const error = materialFileError(file)
    if (error) return error
    if (pending.value.length >= 20) return '一次最多选择 20 个文件'
    files.value.push({ id: ++nextId, uid: file.uid, raw: file, name: file.name, size: file.size, status: 'waiting', progress: 0, error: '' })
    feedback.value = ''
    return ''
  }

  function remove(id) { if (!uploading.value) files.value = files.value.filter(file => file.id !== id) }

  async function confirm({ category, type }) {
    if (uploading.value || !pending.value.length) return null
    uploading.value = true
    feedback.value = ''
    try {
      const folderId = await resolveFolder(category, type)
      const batch = [...pending.value]
      for (const file of batch) {
        file.status = 'uploading'
        file.error = ''
        file.progress = 0
        try {
          await upload(file.raw, { category, folderId, onUploadProgress: event => {
            if (event.total) file.progress = Math.min(99, Math.round(event.loaded / event.total * 100))
          } })
          file.status = 'success'
          file.progress = 100
          file.raw = undefined
          onSettled(file)
        } catch (error) {
          file.status = 'error'
          file.error = error.message || '上传失败，请重试'
        }
      }
      const success = files.value.filter(file => file.status === 'success').length
      const failed = files.value.filter(file => file.status === 'error').length
      feedback.value = `成功 ${success} 个${failed ? `，失败 ${failed} 个，可重试失败文件` : '，全部上传完成'}`
      return { category, type, folderId, success, failed }
    } catch (error) {
      feedback.value = error.message || '无法准备上传分类，请重试'
      return null
    } finally {
      uploading.value = false
    }
  }
  return { files, uploading, feedback, pending, add, remove, confirm }
}
