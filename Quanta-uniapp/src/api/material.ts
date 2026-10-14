import { API_BASE_URL } from '../config/runtime'
import { TOKEN_KEY } from '../utils/storage'
import request from '../utils/request'
import type { TableResponse } from './contracts'

export interface MaterialDto {
  materialId: number
  fileName: string
  fileSize?: number
  category?: string
  uploaderName?: string
  downloadUrl?: string
  uploadTime?: string
}

export const getMaterials = async () => {
  const response = await request<TableResponse<MaterialDto>>({ url: '/qt/materials', data: { pageNum: 1, pageSize: 100 } })
  return response.rows || []
}

export const downloadMaterial = (material: MaterialDto) => new Promise<string>((resolve, reject) => {
  const token = uni.getStorageSync(TOKEN_KEY)
  const path = material.downloadUrl || `/qt/materials/${material.materialId}/download`
  uni.downloadFile({
    url: `${API_BASE_URL}${path.startsWith('/') ? path : `/${path}`}`,
    header: token ? { Authorization: `Bearer ${token}` } : {},
    success: (response) => response.statusCode === 200 ? resolve(response.tempFilePath) : reject(new Error('资料下载失败')),
    fail: (error) => reject(new Error(error.errMsg || '资料下载失败')),
  })
})

export const openMaterialPicker = async () => {
  const materials = await getMaterials()
  if (!materials.length) {
    uni.showToast({ title: '暂无可见学习资料', icon: 'none' })
    return
  }
  uni.showActionSheet({
    itemList: materials.slice(0, 6).map((item) => item.fileName || '未命名资料'),
    success: async ({ tapIndex }) => {
      const material = materials[tapIndex]
      if (!material) return
      uni.showLoading({ title: '下载中' })
      try {
        const filePath = await downloadMaterial(material)
        uni.openDocument({ filePath, showMenu: true, fail: () => uni.showToast({ title: '文件已下载，当前格式无法预览', icon: 'none' }) })
      } catch (error: any) {
        uni.showToast({ title: error?.message || '资料下载失败', icon: 'none' })
      } finally {
        uni.hideLoading()
      }
    },
  })
}
