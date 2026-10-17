import { describe, expect, it } from 'vitest'
import { adminDepartmentLabel, adminRoleLabel, adminMenuGroup } from '../admin-display'

describe('admin display-only labels', () => {
  it('maps department and title codes without changing permission roles', () => {
    const user = { memberDepartment: 'FRONTEND', memberTitle: 'MANAGER' }
    expect(adminDepartmentLabel(user)).toBe('前端部')
    expect(adminRoleLabel(user, ['qt_manager'])).toBe('经理')
    expect(user).toEqual({ memberDepartment: 'FRONTEND', memberTitle: 'MANAGER' })
    expect(adminRoleLabel({ memberTitle: 'manager' })).toBe('经理')
    expect(adminRoleLabel({ memberTitle: 'CEO' })).toBe('首席执行官')
    expect(adminRoleLabel({ memberTitle: 'VP' })).toBe('副总裁')
  })
  it('preserves unknown labels and supplies an explicit role fallback', () => {
    expect(adminRoleLabel({ memberTitle: '技术负责人' })).toBe('技术负责人')
    expect(adminRoleLabel({}, ['qt_mgmt'])).toBe('管理层')
    expect(adminRoleLabel({}, ['admin'])).toBe('管理员')
    expect(adminDepartmentLabel({ deptCode: 'BACKEND' })).toBe('后端部')
  })
  it('groups navigation for display without replacing routes', () => {
    expect(adminMenuGroup('总览')).toBe('工作台')
    expect(adminMenuGroup('成员管理')).toBe('人员与招新')
    expect(adminMenuGroup('招生端')).toBe('人员与招新')
    expect(adminMenuGroup('塔员端')).toBe('内容与服务')
    expect(adminMenuGroup('系统配置')).toBe('系统配置')
  })
})
