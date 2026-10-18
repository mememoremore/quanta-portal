import { normalizeDepartmentCode } from '@/stores/user'
import { materialCategoryLabel } from './material-categories'

const titles = {
  MANAGER: '经理', QT_MANAGER: '经理', MGMT: '管理层', QT_MGMT: '管理层', MANAGEMENT: '管理层',
  ADMIN: '管理员', CEO: '首席执行官', VP: '副总裁', DIRECTOR: '总监', LEADER: '负责人',
  MEMBER: '塔员', INTERN: '实习生', TRAINEE: '实习生',
}

function readableLabel(value) {
  const text = String(value || '').trim()
  // Do not guess a global transcoding or mutate server data. Fall back to codes
  // when a display label contains replacement/control characters or UTF-8 mojibake.
  const invalidCodePoint = Array.from(text).some(char => {
    const code = char.charCodeAt(0)
    return code < 32 || (code >= 127 && code <= 159) || (code >= 0xe000 && code <= 0xf8ff)
  })
  return invalidCodePoint || /\uFFFD|[ÃÂ]|[äåæçèé][^\p{L}\p{N}\s]/u.test(text) ? '' : text
}

export function adminRoleLabel(user = {}, roles = []) {
  for (const value of [user?.memberTitle, user?.roleCategory]) {
    const title = readableLabel(value)
    if (title) return titles[title.toUpperCase()] || title
  }
  for (const role of ['admin', 'ceo', 'qt_mgmt', 'qt_manager']) {
    if (roles.includes(role)) return titles[role.toUpperCase()]
  }
  return user?.memberTitle || user?.roleCategory ? '身份待完善' : ''
}

export function adminDepartmentLabel(user = {}) {
  const values = [user?.deptCode, user?.dept?.deptCode, user?.memberDepartmentCode, user?.memberDepartment, user?.dept?.deptName]
  const code = values.map(normalizeDepartmentCode).find(Boolean)
  if (code) return `${materialCategoryLabel(code)}部`
  const name = [user?.dept?.deptName, user?.memberDepartment].map(readableLabel).find(Boolean)
  return name || (values.some(Boolean) ? '部门待完善' : 'Quanta')
}

export function adminMenuGroup(group, title = '') {
  const fallback = { 控制台: '工作台', 访问指标: '工作台', 成员管理: '人员与招新', 招新管理: '人员与招新', 活动报名: '内容与服务', 学习资料: '内容与服务', 图书借阅: '内容与服务', 塔服订购: '内容与服务' }
  return { 总览: '工作台', 成员管理: '人员与招新', 招生端: '人员与招新', 塔员端: '内容与服务' }[group] || group || fallback[title] || '其他'
}
