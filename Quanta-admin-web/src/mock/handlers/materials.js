import { findAccountByToken } from '../data/accounts'
import { mockMaterials } from '../data/materials'
import { MATERIAL_EXTENSIONS, MATERIAL_MAX_SIZE, isMaterialCategory } from '@/utils/material-categories'

const has = (account, permission) => account?.permissions.includes('*:*:*') || account?.permissions.includes(permission)
const account = (config) => findAccountByToken(config.headers?.Authorization || '')
const departmentCodes = { 前端部: 'FRONTEND', 后端部: 'BACKEND', 产品部: 'PRODUCT', 设计部: 'DESIGN', 研发部: 'BACKEND' }

export const materialHandlers = [
  {
    method: 'get', path: '/qt/materials', handle(config) {
      const current = account(config)
      if (!current) return { code: 401, msg: '登录状态已失效' }
      if (!has(current, 'qt:material:list') && !current.user?.isQuantaMember) return { code: 403, msg: '没有查看学习资料的权限' }
      const params = config.params || {}
      const filtered = mockMaterials.filter((item) =>
        (!params.categoryScope || isMaterialCategory(item.category)) &&
        (!params.category || item.category === params.category) &&
        (!params.fileName || item.fileName.toLowerCase().includes(String(params.fileName).toLowerCase())),
      )
      const pageNum = Math.max(1, Number(params.pageNum) || 1)
      const pageSize = Math.max(1, Number(params.pageSize) || 20)
      return { code: 200, rows: filtered.slice((pageNum - 1) * pageSize, pageNum * pageSize), total: filtered.length }
    },
  },
  {
    method: 'post', path: '/qt/materials', handle(config) {
      const current = account(config)
      if (!current) return { code: 401, msg: '登录状态已失效' }
      if (!has(current, 'qt:material:add')) return { code: 403, msg: '没有上传学习资料的权限' }
      const file = config.data instanceof FormData ? config.data.get('file') : null
      if (!file) return { code: 400, msg: '请选择文件' }
      if (file.size > MATERIAL_MAX_SIZE) return { code: 400, msg: '单个文件不能超过20MB' }
      const extension = file.name.split('.').pop()?.toLowerCase()
      if (!MATERIAL_EXTENSIONS.includes(extension)) return { code: 400, msg: '暂不支持该文件格式' }
      const category = config.data.get('category')
      if (!isMaterialCategory(category)) return { code: 400, msg: '请选择有效的资料分类' }
      const isSuperAdmin = current.roles.includes('admin') || current.roles.includes('ceo')
      const ownCategory = current.user.deptCode || departmentCodes[current.user.memberDepartment]
      if (!isSuperAdmin && category !== ownCategory) return { code: 403, msg: '只能上传本部门资料' }
      if (config.data.get('visibility') !== 'MEMBER') return { code: 400, msg: '资料必须对全部塔员可见' }
      const materialId = Date.now()
      mockMaterials.unshift({
        materialId, fileName: file.name, category,
        fileSize: file.size, uploaderName: current.user.nickName,
        uploadTime: new Date().toLocaleString('zh-CN', { hour12: false }).replaceAll('/', '-'),
        visibility: 'MEMBER',
        downloadUrl: `/qt/materials/${materialId}/download`,
      })
      return { code: 200, msg: '上传成功' }
    },
  },
  {
    method: 'get', match: (path) => /^\/qt\/materials\/\d+\/download$/.test(path), handle(config) {
      const current = account(config)
      if (!current) return { code: 401, msg: '登录状态已失效' }
      const id = Number(config.path.split('/').at(-2))
      const material = mockMaterials.find((item) => item.materialId === id)
      if (!material) return { code: 404, msg: '学习资料不存在' }
      return {
        code: 200,
        data: { fileName: material.fileName, content: `Mock material: ${material.fileName}` },
      }
    },
  },
  {
    method: 'delete', match: (path) => /^\/qt\/materials\/\d+$/.test(path), handle(config) {
      const current = account(config)
      if (!current) return { code: 401, msg: '登录状态已失效' }
      if (!has(current, 'qt:material:remove')) return { code: 403, msg: '没有删除学习资料的权限' }
      const id = Number(config.path.split('/').pop())
      const index = mockMaterials.findIndex((item) => item.materialId === id)
      if (index >= 0) mockMaterials.splice(index, 1)
      return { code: 200, msg: '删除成功' }
    },
  },
]
