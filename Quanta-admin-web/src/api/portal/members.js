import { request } from '@/utils/request'

const departmentNames = {
  PRODUCT: '产品',
  DESIGN: '设计',
  FRONTEND: '前端',
  BACKEND: '后端',
}

function assetUrl(value = '') {
  if (!value || /^https?:\/\//i.test(value) || value.startsWith('data:')) return value
  const base = String(import.meta.env.VITE_API_BASE_URL || '').replace(/\/$/, '')
  return `${base}${value.startsWith('/') ? value : `/${value}`}`
}

export function departmentLabel(value = '') {
  return departmentNames[value] || value || '未分配'
}

export function mapMember(row = {}) {
  const memberFlag = String(row.isQuantaMember ?? '') === '1'
  return {
    id: Number(row.userId),
    name: row.nickName || row.userName || row.memberNo || 'Quanta 成员',
    userName: row.userName || '',
    memberNo: row.memberNo || '',
    departmentCode: row.memberDepartment || '',
    department: departmentLabel(row.memberDepartment),
    title: row.memberTitle || row.roleCategory || '成员',
    cohort: row.memberCohort || '未分届',
    phone: row.phonenumber || '',
    avatar: assetUrl(row.avatar || ''),
    isActive: row.memberStatus ? row.memberStatus === 'ACTIVE' : memberFlag,
  }
}

export async function getMemberDirectory(params = {}) {
  const response = await request({
    url: '/qt/member/list',
    method: 'get',
    params: { pageNum: 1, pageSize: 500, ...params },
  })
  return {
    rows: (response.rows || []).map(mapMember),
    total: Number(response.total) || 0,
  }
}

export async function getCurrentProfile() {
  const response = await request({ url: '/getInfo', method: 'get' })
  return {
    ...mapMember(response.user || {}),
    roles: response.roles || [],
    permissions: response.permissions || [],
  }
}
