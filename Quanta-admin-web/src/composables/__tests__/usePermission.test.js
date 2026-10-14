import { describe, expect, it } from 'vitest'
import {
  hasAllPermissions,
  hasAnyPermission,
  hasBusinessRole,
  isCeoRole,
} from '../usePermission'

describe('permission helpers', () => {
  it('supports any and all permission checks', () => {
    const granted = ['qt:member:list', 'qt:member:retain']
    expect(hasAnyPermission(['system:user:remove', 'qt:member:retain'], granted)).toBe(true)
    expect(hasAllPermissions(['qt:member:list', 'qt:member:retain'], granted)).toBe(true)
    expect(hasAllPermissions(['qt:member:list', 'system:user:remove'], granted)).toBe(false)
  })

  it('allows the RuoYi super permission', () => {
    expect(hasAnyPermission('anything:manage', ['*:*:*'])).toBe(true)
    expect(hasAllPermissions(['one', 'two'], ['*:*:*'])).toBe(true)
  })

  it('treats admin as CEO and accepts backend management roles', () => {
    expect(isCeoRole('ceo')).toBe(true)
    expect(isCeoRole('admin')).toBe(true)
    expect(hasBusinessRole('ceo', ['admin'])).toBe(true)
    expect(hasBusinessRole('qt_mgmt', ['qt_mgmt'])).toBe(true)
    expect(hasBusinessRole('qt_manager', ['qt_manager'])).toBe(true)
  })
})
