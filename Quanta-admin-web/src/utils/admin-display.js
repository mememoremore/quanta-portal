import { normalizeDepartmentCode } from '@/stores/user'
import { materialCategoryLabel } from './material-categories'

const titles = {
  MANAGER: '经理', QT_MANAGER: '经理', MGMT: '管理层', QT_MGMT: '管理层', MANAGEMENT: '管理层',
  ADMIN: '管理员', CEO: '首席执行官', VP: '副总裁', DIRECTOR: '总监', LEADER: '负责人',
  MEMBER: '塔员', INTERN: '实习生', TRAINEE: '实习生',
}

export function adminRoleLabel(user = {}, roles = []) {
  const title = String(user?.memberTitle || user?.roleCategory || '').trim()
  if (title) return titles[title.toUpperCase()] || title
  for (const role of ['admin', 'ceo', 'qt_mgmt', 'qt_manager']) {
    if (roles.includes(role)) return titles[role.toUpperCase()]
  }
  return ''
}

export function adminDepartmentLabel(user = {}) {
  const name = user?.dept?.deptName || user?.memberDepartment || ''
  const code = normalizeDepartmentCode(user?.deptCode || user?.dept?.deptCode || user?.memberDepartmentCode || name)
  return code ? `${materialCategoryLabel(code)}部` : name || 'Quanta'
}

export function adminMenuGroup(group, title = '') {
  const fallback = { 控制台: '工作台', 访问指标: '工作台', 成员管理: '人员与招新', 招新管理: '人员与招新', 活动报名: '内容与服务', 学习资料: '内容与服务', 图书借阅: '内容与服务', 塔服订购: '内容与服务' }
  return { 总览: '工作台', 成员管理: '人员与招新', 招生端: '人员与招新', 塔员端: '内容与服务' }[group] || group || fallback[title] || '其他'
}
