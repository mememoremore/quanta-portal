import { request } from '@/utils/request'
import { isMaterialCategory } from '@/utils/material-categories'
import { validMemberId } from '@/utils/member-profile'

const roleCategories = new Set(['MGMT', 'MANAGER', 'INTERN', 'MEMBER'])
const text = value => String(value ?? '').trim()

// Proposed atomic member-management contract. Never fall back to /system/user writes:
// they do not synchronize the cohort member record and business role together.
export async function getMemberManagementOptions() {
  const { data } = await request({ url: '/qt/member/manage/options', method: 'get' })
  if (data?.contractVersion !== 'member-admin-v1' || !Array.isArray(data.departments) || !Array.isArray(data.cohorts) || !Array.isArray(data.roleProfiles)) throw new Error('成员维护接口尚未接通或版本不匹配，请联系后端')
  const departments = data.departments.filter(item => isMaterialCategory(item.value))
  const roleProfiles = data.roleProfiles.filter(item => roleCategories.has(item.value)).map(item => ({ ...item, titles: (item.titles || []).filter(title => !['CEO','ADMIN','COO','首席执行官','管理员'].includes(text(title).toUpperCase())) }))
  if (!departments.length || !data.cohorts.length || !roleProfiles.length || roleProfiles.some(item => !item.titles.length)) throw new Error('成员维护接口返回的部门、职位或角色选项不完整')
  return { ...data, departments, roleProfiles }
}

export async function getManagedMember(userId, memberCohort) {
  if (!validMemberId(userId)) throw new Error('无效的成员编号')
  const { data } = await request({ url: `/qt/member/manage/${userId}`, method: 'get', params: { memberCohort } })
  if (Number(data?.userId) !== Number(userId) || !text(data?.version)) throw new Error('成员资料接口返回异常，请刷新后重试')
  return data
}

export function managedMemberPayload(form, options, mode) {
  if (!['create','identity','access'].includes(mode)) throw new Error('无效的成员操作')
  const roleCategory = text(form.roleCategory), memberDepartment = text(form.memberDepartment), memberTitle = text(form.memberTitle), memberCohort = text(form.memberCohort)
  const profile = options.roleProfiles.find(item => item.value === roleCategory)
  if (!roleCategories.has(roleCategory) || !profile) throw new Error('请选择已有的成员角色')
  if (!options.departments.some(item => item.value === memberDepartment)) throw new Error('请选择有效的所属部门')
  if (!profile.titles.includes(memberTitle)) throw new Error('请选择与身份层级对应的职位')
  // 分配权限不改届数，允许空；编辑资料必须指定一届，且必须是届次选项里的编号。
  if (mode !== 'access' && !memberCohort) throw new Error('请选择届数')
  if (memberCohort && !options.cohorts.some(item => String(item.value) === memberCohort)) throw new Error('请选择有效的届数')
  const payload = { mode, ...(mode !== 'create' ? { version: text(form.version) } : {}), memberDepartment, memberTitle, memberCohort, roleCategory }
  if (mode !== 'create' && !payload.version) throw new Error('缺少成员资料版本，请重新加载')
  if (mode !== 'access') {
    payload.nickName = text(form.nickName)
    if (!payload.nickName || payload.nickName.length > 30) throw new Error('真实姓名需为 1–30 字')
  }
  if (mode === 'create') {
    if (!['INTERN','MEMBER'].includes(roleCategory)) throw new Error('新增成员默认普通塔员，请创建后再调整管理角色')
    payload.userName = text(form.userName)
    payload.email = text(form.email)
    if (!/^[a-zA-Z0-9_.@-]{2,20}$/.test(payload.userName)) throw new Error('登录账号需为 2–20 位字母、数字或 . _ @ -')
    if (payload.email && (payload.email.length > 64 || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(payload.email))) throw new Error('请输入有效的账号邮箱')
  }
  return payload
}

export async function saveManagedMember(userId, form, options, mode) {
  if (mode !== 'create' && !validMemberId(userId)) throw new Error('无效的成员编号')
  const response = await request({ url: mode === 'create' ? '/qt/member/manage' : `/qt/member/manage/${userId}`, method: mode === 'create' ? 'post' : 'put', data: managedMemberPayload(form, options, mode) })
  if (!validMemberId(response.data?.userId)) throw new Error('服务端未确认成员维护结果，请刷新列表核对后再操作')
  return response.data
}
