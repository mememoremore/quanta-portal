import { findAccountByToken } from '../data/accounts'
import {
  findMockMember,
  mockCohorts,
  mockDepartments,
  mockMembers,
} from '../data/members'

function requireAccount(config) {
  const authorization = config.headers?.Authorization || config.headers?.authorization || ''
  return findAccountByToken(authorization)
}

function unauthorized() {
  return { code: 401, msg: '登录状态已失效' }
}

function hasPermission(account, permission) {
  return account?.permissions?.includes('*:*:*') || account?.permissions?.includes(permission)
}

function isCeo(account) {
  return account?.roles?.some((role) => role === 'ceo' || role === 'admin')
}

function forbidden() {
  return { code: 403, msg: '没有该操作权限' }
}

function sortById(rows) {
  return [...rows].sort((a, b) => Number(b.userId) - Number(a.userId))
}

function listMembers(config) {
  const account = requireAccount(config)
  if (!account) return unauthorized()

  const params = config.params || {}
  const keyword = String(params.nickName || '').trim().toLowerCase()
  const cohort = String(params.memberCohort || '')
  const department = String(params.memberDepartment || '')
  const roleCategory = String(params.roleCategory || '')
  const pageNum = Math.max(1, Number(params.pageNum) || 1)
  const pageSize = Math.max(1, Number(params.pageSize) || 20)

  const filtered = sortById(mockMembers).filter((member) => {
    if (cohort && member.memberCohort !== cohort) return false
    if (department && member.memberDepartment !== department) return false
    if (roleCategory && member.roleCategory !== roleCategory) return false
    if (keyword && !member.nickName.toLowerCase().includes(keyword)) return false
    return true
  })
  const start = (pageNum - 1) * pageSize

  return {
    code: 200,
    msg: '操作成功',
    rows: filtered.slice(start, start + pageSize),
    total: filtered.length,
    departments: mockDepartments.map((name) => ({ value: name, label: name })),
  }
}

function removeMember(config) {
  const account = requireAccount(config)
  if (!account) return unauthorized()
  if (!hasPermission(account, 'system:user:remove') || !isCeo(account)) return forbidden()

  const userId = config.path.match(/^\/system\/user\/(\d+)$/)?.[1]
  const index = mockMembers.findIndex((member) => String(member.userId) === userId)
  if (index < 0) return { code: 404, msg: '成员不存在' }
  mockMembers.splice(index, 1)
  return { code: 200, msg: '删除成功' }
}

function resetPassword(config) {
  const account = requireAccount(config)
  if (!account) return unauthorized()
  if (!hasPermission(account, 'system:user:resetPwd') || !isCeo(account)) return forbidden()
  if (!findMockMember(config.data?.userId)) return { code: 404, msg: '成员不存在' }
  const password = String(config.data?.password || '')
  if (password.length < 5 || password.length > 20) return { code: 400, msg: '密码长度必须在5到20个字符之间' }
  return { code: 200, msg: '密码重置成功' }
}

function retainMember(config) {
  const account = requireAccount(config)
  if (!account) return unauthorized()
  if (!hasPermission(account, 'qt:member:retain') || !isCeo(account)) return forbidden()

  const userId = config.path.match(/^\/qt\/member\/(\d+)\/retain$/)?.[1]
  if (config.data?.retain !== true) return { code: 400, msg: '留任标记不正确' }
  const sourceCohort = String(config.data?.sourceCohortId || '')
  const member = sourceCohort
    ? findMockMember(userId, sourceCohort)
    : mockMembers.find((item) => String(item.userId) === String(userId))
  if (!member) return { code: 404, msg: '成员不存在' }

  const targetCohort = String(Number(sourceCohort) + 1)
  const existingTarget = mockMembers.find(
    (item) => item.memberCohort === targetCohort && item.nickName === member.nickName,
  )

  if (member.memberStatus === 'retained' && existingTarget) {
    return {
      code: 200,
      msg: `该成员已同步至第${targetCohort}届`,
      data: { sourceStatus: 'retained', targetCohort },
    }
  }
  if (!member.canRetain && member.memberStatus !== 'retained') {
    return { code: 409, msg: '当前状态不可留任' }
  }

  member.memberStatus = 'retained'
  member.canRetain = false

  if (!existingTarget) {
    const nextId = Math.max(...mockMembers.map((item) => Number(item.userId))) + 1
    mockMembers.push({
      ...member,
      userId: nextId,
      memberCohort: targetCohort,
      memberStatus: 'active',
      canRetain: false,
      joinTime: `${Number(member.joinTime.slice(0, 4)) + 1}${member.joinTime.slice(4)}`,
    })
  }

  if (!mockCohorts.some((item) => item.value === targetCohort)) {
    mockCohorts.unshift({ value: targetCohort, label: `第${targetCohort}届`, isCurrent: true })
    mockCohorts.forEach((item) => {
      if (item.value !== targetCohort) item.isCurrent = false
    })
  }

  return {
    code: 200,
    msg: `留任成功，已同步至第${targetCohort}届`,
    data: { sourceStatus: 'retained', targetCohort },
  }
}

function importMembers(config) {
  const account = requireAccount(config)
  if (!account) return unauthorized()
  if (!hasPermission(account, 'system:user:import') || !isCeo(account)) return forbidden()
  const file = config.data instanceof FormData ? config.data.get('file') : null
  if (!file) return { code: 400, msg: '请选择名单文件' }
  return { code: 200, msg: '导入成功', data: { successCount: 3, failureCount: 0 } }
}

export const memberHandlers = [
  {
    method: 'get',
    path: '/qt/member/cohorts',
    handle(config) {
      if (!requireAccount(config)) return unauthorized()
      const current = mockCohorts.find((item) => item.isCurrent)
      const cohortValue = (value) => (config.__partialMock ? `${value}st` : value)
      return {
        code: 200,
        msg: '操作成功',
        data: {
          currentCohort: current
            ? { cohortId: cohortValue(current.value), cohortName: current.label, isCurrent: true }
            : null,
          cohorts: mockCohorts.map((item) => ({
            id: cohortValue(item.value),
            name: item.label,
            isCurrent: item.isCurrent,
          })),
        },
      }
    },
  },
  { method: 'get', path: '/qt/member/list', handle: listMembers },
  {
    method: 'delete',
    match: (path) => /^\/system\/user\/\d+$/.test(path),
    handle: removeMember,
  },
  { method: 'put', path: '/system/user/resetPwd', handle: resetPassword },
  { method: 'post', path: '/system/user/importData', handle: importMembers },
  {
    method: 'post',
    path: '/system/user/importTemplate',
    handle(config) {
      const account = requireAccount(config)
      if (!account) return unauthorized()
      if (!hasPermission(account, 'system:user:import') || !isCeo(account)) return forbidden()
      return {
        code: 200,
        data: { fileName: '成员导入模板.xlsx', content: 'Mock member import template' },
      }
    },
  },
  {
    method: 'put',
    match: (path) => /^\/qt\/member\/\d+\/retain$/.test(path),
    handle: retainMember,
  },
]
