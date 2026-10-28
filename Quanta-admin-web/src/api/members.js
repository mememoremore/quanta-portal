import { request } from '@/utils/request'
import { requestBlob } from '@/utils/download'
import { displayPersonName } from '@/utils/person-name'

// 角色类别。后端 qt_member_record.role_category 的取值之一。
// 普通塔员在业务上统一叫「实习生」，因此筛选里不出现独立的「成员」项：
// 历史上存的 MEMBER 会被归并成 INTERN 展示与筛选。
const roleLabels = {
  MGMT: '管理层',
  MANAGER: '经理层',
  INTERN: '实习生',
}

// 部门中文映射。sys_user.member_department 历史数据混存过英文代码，
// 展示与筛选统一按这四类归一为中文。
const departmentLabels = {
  FRONTEND: '前端',
  BACKEND: '后端',
  PRODUCT: '产品',
  DESIGN: '设计',
}

function normalizeRoleCategory(code) {
  const raw = String(code || '').trim()
  const upper = raw.toUpperCase()
  if (upper === 'MGMT' || raw === 'management' || upper === 'CEO') return 'MGMT'
  if (upper === 'MANAGER' || raw === 'manager') return 'MANAGER'
  // INTERN 与历史 MEMBER 同义，统一归为实习生
  if (upper === 'INTERN' || raw === 'intern' || upper === 'MEMBER') return 'INTERN'
  return upper || 'INTERN'
}

function normalizeDepartment(value) {
  const raw = String(value || '').trim()
  if (!raw) return '-'
  const code = raw.toUpperCase()
  if (departmentLabels[code]) return departmentLabels[code]
  // 历史数据可能直接存了中文，原样透传，避免「后端部」等中文被吞掉
  return raw
}

// 真正的届在 qt_member_record.cohort_id，历史账号的 member_cohort 可能是编号或届次名称。
// 列表优先展示届次名称，纯编号兜底拼「第 N 届」。
function cohortLabel(user = {}) {
  const cohortName = String(user.cohortName || '').trim()
  if (cohortName) return cohortName
  const memberCohort = String(user.memberCohort || '').trim()
  if (/^\d+$/.test(memberCohort)) return `第${memberCohort}届`
  return memberCohort
}

export function mapMember(user = {}) {
  const roleCode = normalizeRoleCategory(user.roleCategory)
  return {
    id: user.userId,
    name: displayPersonName(user, '-'),
    department: normalizeDepartment(user.memberDepartment || user.dept?.deptName),
    roleCode,
    roleLabel: roleLabels[roleCode] || user.memberTitle || '-',
    title: user.memberTitle || roleLabels[roleCode] || '-',
    cohort: cohortLabel(user),
    cohortId: String(user.memberCohort || ''),
    phoneMasked: user.phonenumber || '-',
    joinedAt: user.joinTime || user.createTime || '-',
    status: user.memberStatus || (user.status === '1' ? 'resigned' : 'active'),
    canRetain: Boolean(user.canRetain),
  }
}

export function mapCohorts(payload = {}) {
  const list = Array.isArray(payload) ? payload : payload.cohorts || []
  return list.map((item) => ({
    value: String(item.value ?? item.id ?? item.cohortId),
    label: item.label ?? item.name ?? item.cohortName,
    isCurrent: Boolean(item.isCurrent),
  }))
}

export async function getMemberCohorts() {
  const response = await request({ url: '/qt/member/cohorts', method: 'get' })
  return mapCohorts(response.data || {})
}

export async function getMemberList(query = {}) {
  const response = await request({
    url: '/qt/member/list',
    method: 'get',
    params: query,
  })
  return {
    rows: (response.rows || []).map(mapMember),
    total: Number(response.total) || 0,
    departments: response.departments || [],
  }
}

export function removeMember(userId) {
  return request({ url: `/system/user/${userId}`, method: 'delete' })
}

export function resetMemberPassword(userId, password) {
  return request({ url: '/system/user/resetPwd', method: 'put', data: { userId, password } })
}

export function retainMember(userId, options = {}) {
  return request({
    url: `/qt/member/${userId}/retain`,
    method: 'put',
    data: { ...options, retain: true },
  })
}

export function importMembers(file, updateSupport = false) {
  const data = new FormData()
  data.append('file', file)
  data.append('updateSupport', String(updateSupport))
  return request({
    url: '/system/user/importData',
    method: 'post',
    data,
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

export function downloadMemberTemplate() {
  return requestBlob(
    { url: '/system/user/importTemplate', method: 'post' },
    '成员导入模板.xlsx',
  )
}

export const memberRoleOptions = Object.entries(roleLabels).map(([value, label]) => ({
  value,
  label,
}))
