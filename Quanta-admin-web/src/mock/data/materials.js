const initialMaterials = [
  { materialId: 1, fileName: 'React 入门教程.pdf', category: 'FRONTEND', fileSize: 2411724, uploaderName: '王建国', uploadTime: '2025-03-15 14:00', visibility: 'MEMBER', downloadUrl: '/qt/materials/1/download' },
  { materialId: 2, fileName: '接口设计规范.docx', category: 'BACKEND', fileSize: 1126400, uploaderName: '陈佳琪', uploadTime: '2025-03-20 10:30', visibility: 'MEMBER', downloadUrl: '/qt/materials/2/download' },
  { materialId: 3, fileName: '需求文档模板.pptx', category: 'PRODUCT', fileSize: 3523215, uploaderName: '李明华', uploadTime: '2025-04-01 16:00', visibility: 'MEMBER', downloadUrl: '/qt/materials/3/download' },
  { materialId: 4, fileName: '界面设计规范.pdf', category: 'DESIGN', fileSize: 734003, uploaderName: '张晓雪', uploadTime: '2025-04-10 11:00', visibility: 'MEMBER', downloadUrl: '/qt/materials/4/download' },
]

const departments = ['FRONTEND', 'BACKEND', 'PRODUCT', 'DESIGN']
const names = ['培训资料', '技术分享', '项目实践']
const initialFolders = departments.flatMap((category, index) => names.map((folderName, type) => ({ folderId: index * 3 + type + 1, parentId: 0, category, folderName, depth: 1 })))
const categorizedMaterials = initialMaterials.map(row => ({ ...row, folderId: departments.indexOf(row.category) * 3 + 1 }))
export let mockMaterials = structuredClone(categorizedMaterials)
export let mockMaterialFolders = structuredClone(initialFolders)
export function resetMockMaterials() { mockMaterials = structuredClone(categorizedMaterials); mockMaterialFolders = structuredClone(initialFolders) }
