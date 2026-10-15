export const MATERIAL_CATEGORIES = Object.freeze([
  { value: 'FRONTEND', label: '前端' },
  { value: 'BACKEND', label: '后端' },
  { value: 'PRODUCT', label: '产品' },
  { value: 'DESIGN', label: '设计' },
])

export const MATERIAL_EXTENSIONS = Object.freeze([
  'pdf', 'doc', 'docx', 'ppt', 'pptx', 'xls', 'xlsx', 'txt', 'md',
  'jpg', 'jpeg', 'png', 'webp',
])

export const MATERIAL_MAX_SIZE = 20 * 1024 * 1024

const categoryValues = new Set(MATERIAL_CATEGORIES.map((item) => item.value))

export function isMaterialCategory(value) {
  return categoryValues.has(value)
}

export function materialCategoryLabel(value) {
  return MATERIAL_CATEGORIES.find((item) => item.value === value)?.label || ''
}

export function getMaterialUploadPolicy({ roles = [], permissions = [], departmentCode = '' } = {}) {
  const canAccessAdmin = roles.some((role) => ['admin', 'ceo', 'qt_mgmt', 'qt_manager'].includes(role))
  const hasUploadPermission = permissions.includes('qt:material:add') || permissions.includes('*:*:*')
  if (!canAccessAdmin || !hasUploadPermission) return { allowed: false, canChoose: false, category: '', reason: '' }
  if (roles.includes('admin') || roles.includes('ceo') || permissions.includes('*:*:*')) {
    return { allowed: true, canChoose: true, category: '', reason: '' }
  }
  if (!isMaterialCategory(departmentCode)) {
    return { allowed: false, canChoose: false, category: '', reason: '无法识别所属部门，请联系管理员完善账号资料' }
  }
  return { allowed: true, canChoose: false, category: departmentCode, reason: '' }
}

export function materialFileError(file) {
  const extension = String(file?.name || '').split('.').pop()?.toLowerCase()
  if (!MATERIAL_EXTENSIONS.includes(extension)) return '仅支持 PDF、Word、PPT、Excel、TXT、Markdown 和常见图片'
  if (file.size > MATERIAL_MAX_SIZE) return '单个文件不能超过 20 MB'
  return ''
}
