import { request } from '@/utils/request'
import { isMaterialCategory } from '@/utils/material-categories'
import { materialFolderForType, materialTypeLabel } from '@/utils/material-types'

export async function getMaterialFolders(category) {
  if (!isMaterialCategory(category)) throw new Error('请选择有效的资料所属部门')
  const response = await request({ url: '/qt/materials/folders', method: 'get', params: { category, parentId: 0 } })
  if (!Array.isArray(response.data)) throw new Error('资料分类接口返回格式异常，请联系管理员')
  return response.data.filter(folder => folder.category === category && Number(folder.parentId) === 0 && Number(folder.folderId) > 0)
    .map(folder => ({ ...folder, folderId: Number(folder.folderId), parentId: 0 }))
}

// Only invoked by an explicit upload confirmation. Browsing never creates categories.
export async function ensureMaterialTypeFolder(category, type) {
  if (!isMaterialCategory(category) || !materialTypeLabel(type)) throw new Error('请选择有效的部门和资料类型')
  const existing = materialFolderForType(await getMaterialFolders(category), type)
  if (existing) return existing.folderId
  let createError
  try {
    await request({ url: '/qt/materials/folders', method: 'post', data: { category, parentId: 0, folderName: materialTypeLabel(type) } })
  } catch (error) {
    createError = error
  }
  // The backend may return only code=200; also handle a concurrent same-name creation.
  let folders
  try { folders = await getMaterialFolders(category) } catch (error) { throw createError || error }
  const created = materialFolderForType(folders, type)
  if (created) return created.folderId
  throw createError || new Error('资料类型尚未建立，请联系管理员后重试')
}
