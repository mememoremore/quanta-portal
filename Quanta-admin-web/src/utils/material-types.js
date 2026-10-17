export const MATERIAL_TYPES = Object.freeze([
  { value: 'TRAINING', label: '培训资料' },
  { value: 'TECH', label: '技术分享' },
  { value: 'PROJECT', label: '项目实践' },
])

export function materialTypeLabel(value) {
  return MATERIAL_TYPES.find(item => item.value === value)?.label || ''
}

export function materialFolderForType(folders, type) {
  const name = materialTypeLabel(type)
  return name ? folders.find(folder => folder.folderName === name && Number(folder.parentId) === 0) : undefined
}

export function materialRowTypeLabel(row, selectedType = '') {
  return materialTypeLabel(selectedType) || MATERIAL_TYPES.find(item => item.label === row.folderName)?.label || ''
}
