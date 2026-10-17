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

export function downloadMaterial(material) {
  return requestBlob(
    { url: material.downloadUrl, method: 'get' },
    material.fileName || '学习资料',
  )
}
