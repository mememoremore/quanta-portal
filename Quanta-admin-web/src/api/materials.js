import { request } from '@/utils/request'
import { requestBlob } from '@/utils/download'

export function mapMaterial(item = {}) {
  return {
    ...item,
    uploader: item.uploaderName || item.uploader || '-',
    uploadedAt: item.uploadTime || item.createTime || item.uploadedAt || '-',
    downloadUrl: item.downloadUrl || `/qt/materials/${item.materialId}/download`,
  }
}

export async function getMaterials(params = {}) {
  const response = await request({
    url: '/qt/materials',
    method: 'get',
    params: { pageNum: 1, pageSize: 20, categoryScope: 'STANDARD', ...params },
  })
  return {
    rows: (response.rows || []).map(mapMaterial),
    total: Number(response.total) || 0,
  }
}

export function uploadMaterial(file, { category, folderId, onUploadProgress } = {}) {
  const data = new FormData()
  data.append('file', file)
  data.append('category', category)
  data.append('visibility', 'MEMBER')
  if (Number(folderId) > 0) data.append('folderId', String(folderId))
  // 资料最大 20 MB，单独放宽超时，避免被全局 15s 掐断
  return request({ url: '/qt/materials', method: 'post', data, timeout: 120000, ...(onUploadProgress ? { onUploadProgress } : {}) })
}

export function removeMaterial(materialId) {
  return request({ url: `/qt/materials/${materialId}`, method: 'delete' })
}

// Dedicated CEO endpoint. Do not silently turn a batch into a series of single deletes.
export async function batchRemoveMaterials(materialIds) {
  const validId = id => Number.isSafeInteger(id) && id > 0
  if (!Array.isArray(materialIds) || !materialIds.length || materialIds.length > 20 ||
      !materialIds.every(validId) || new Set(materialIds).size !== materialIds.length) {
    throw new Error('请选择当前页内有效的资料（最多 20 份）')
  }
  const ids = [...materialIds]
  let response
  try {
    response = await request({ url: '/qt/materials/batch-delete', method: 'post', data: { materialIds: ids } })
  } catch (error) {
    if ([404, 405, 501].includes(Number(error.code))) {
      throw Object.assign(new Error('批量删除接口尚未接通，请联系后端；未改用逐条删除。'), { code: error.code })
    }
    // A dropped response or server error may happen after a commit; never claim no deletion occurred.
    if (!Number(error.code) || Number(error.code) >= 500) {
      throw Object.assign(new Error('删除结果未确认，请刷新列表核对后再操作。'), { uncertain: true })
    }
    throw error
  }
  const { deletedIds, alreadyDeletedIds } = response?.data || {}
  if (Array.isArray(deletedIds) && Array.isArray(alreadyDeletedIds)) {
    const completed = [...deletedIds, ...alreadyDeletedIds]
    if (completed.length === ids.length && new Set(completed).size === ids.length &&
        completed.every(id => validId(id) && ids.includes(id))) return { deletedIds, alreadyDeletedIds }
  }
  throw Object.assign(new Error('批量删除返回格式异常，删除结果未确认，请刷新列表核对。'), { uncertain: true })
}

export function downloadMaterial(material) {
  return requestBlob(
    { url: material.downloadUrl, method: 'get' },
    material.fileName || '学习资料',
  )
}
