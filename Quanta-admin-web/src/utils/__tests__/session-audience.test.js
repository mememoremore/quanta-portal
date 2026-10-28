import { beforeEach, describe, expect, it } from 'vitest'
import {
  clearAudience,
  entryPathFor,
  getAudience,
  homePathFor,
  loginPathFor,
  loginTypeFor,
  setAudience,
} from '../session-audience'

describe('session audience', () => {
  beforeEach(() => localStorage.clear())

  it('persists only a known audience', () => {
    setAudience('freshman')
    expect(getAudience()).toBe('freshman')
    clearAudience()
    expect(getAudience()).toBe('')
    expect(() => setAudience('unknown')).toThrow('未知登录身份')
  })

  it('maps audiences to login types and safe destinations', () => {
    expect(loginTypeFor('freshman')).toBe('0')
    expect(loginTypeFor('member')).toBe('1')
    expect(loginTypeFor('admin')).toBe('1')
    expect(loginPathFor('freshman')).toBe('/login/freshman')
    expect(loginPathFor('member')).toBe('/login/member')
    expect(loginPathFor('admin')).toBe('/admin/login')
    expect(homePathFor('admin')).toBe('/admin/dashboard')
  })

  it('resolves an entry destination from the current session', () => {
    // 未登录：各入口落到各自的登录页
    expect(entryPathFor({ audience: 'freshman' })).toBe('/login/freshman')
    expect(entryPathFor({ audience: 'member' })).toBe('/login/member')
    expect(entryPathFor({ audience: 'admin' })).toBe('/admin/login')
  })

  it('sends a signed-in identity straight to its own portal home', () => {
    expect(entryPathFor({ audience: 'member', serverAudience: 'member' })).toBe('/member/home')
    expect(entryPathFor({ audience: 'freshman', serverAudience: 'freshman' })).toBe('/freshman/home')
  })

  it('lets an authorized administrator into the console, and everyone else stays at login', () => {
    expect(entryPathFor({ audience: 'admin', serverAudience: 'member', canAccessAdmin: true }))
      .toBe('/admin/dashboard')
    // 跨入口且无后台权限：停在目标登录页，由页面说明当前会话
    expect(entryPathFor({ audience: 'admin', serverAudience: 'member' })).toBe('/admin/login')
    expect(entryPathFor({ audience: 'freshman', serverAudience: 'member' })).toBe('/login/freshman')
  })

  it('falls back to the identity page instead of the admin login for an unknown entry', () => {
    expect(entryPathFor({})).toBe('/')
    expect(entryPathFor({ audience: '' })).toBe('/')
  })
})
