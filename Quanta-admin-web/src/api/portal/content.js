import { request } from '@/utils/request'
import { requestBlob } from '@/utils/download'

export function mapNotice(row = {}) {
  return {
    id: Number(row.noticeId),
    title: row.noticeTitle || '未命名通知',
    content: row.noticeContent || '',
    type: row.noticeType || '',
    createdAt: row.createTime || '',
    isRead: Boolean(row.isRead),
  }
}

export async function getTopNotices() {
  const response = await request({ url: '/system/notice/listTop', method: 'get' })
  return {
    notices: (response.data || []).map(mapNotice),
    unreadCount: Number(response.unreadCount) || 0,
  }
}

export function markNoticeRead(noticeId) {
  return request({
    url: '/system/notice/markRead',
    method: 'post',
    params: { noticeId: Number(noticeId) },
  })
}

export function mapMaterial(row = {}) {
  return {
    ...row,
    materialId: Number(row.materialId),
    fileName: row.fileName || '学习资料',
    category: row.category || '其他',
    uploader: row.uploaderName || '-',
    uploadedAt: row.uploadTime || row.createTime || '',
    downloadUrl: row.downloadUrl || `/qt/materials/${row.materialId}/download`,
  }
}

export async function getPortalMaterials(params = {}) {
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

export function downloadPortalMaterial(material) {
  return requestBlob(
    { url: material.downloadUrl || `/qt/materials/${material.materialId}/download`, method: 'get' },
    material.fileName || '学习资料',
  )
}
