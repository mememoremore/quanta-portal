import { request } from '@/utils/request'
import { requestBlob } from '@/utils/download'
import { displayPersonName } from '@/utils/person-name'

const roleLabels = {
  MGMT: '管理层',
  MANAGER: '经理层',
  INTERN: '实习生',
  MEMBER: '成员',
}

function normalizeRoleCategory(code) {
  const raw = String(code || '').trim()
  const upper = raw.toUpperCase()
  if (upper === 'MGMT' || raw === 'management' || upper === 'CEO') return 'MGMT'
  if (upper === 'MANAGER' || raw === 'manager') return 'MANAGER'
  if (upper === 'INTERN' || raw === 'intern') return 'INTERN'
  if (upper === 'MEMBER') return 'MEMBER'
  return upper || 'INTERN'
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
    department: user.memberDepartment || user.dept?.deptName || '-',
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
